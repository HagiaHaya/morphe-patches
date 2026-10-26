/*
 * Based on the GmsCore support patch of Morphe Patches.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.template.patches.shared.gms

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.gms.Constants.ACTIONS
import app.template.patches.shared.gms.Constants.AUTHORITIES
import app.template.patches.shared.gms.Constants.CREDENTIAL_TYPES
import app.template.patches.shared.gms.Constants.PERMISSIONS
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

/**
 * Vendor group of MicroG-RE, the GmsCore that Morphe patched apps use.
 */
private const val GMS_CORE_VENDOR_GROUP_ID = "app.revanced"

private const val GMS_CORE_PACKAGE_NAME = "$GMS_CORE_VENDOR_GROUP_ID.android.gms"

/**
 * Makes GmsCore present the original package name and signature of the app to Google,
 * and lets the app see GmsCore and receive push notifications from it.
 *
 * @param packageName Package name of the app.
 * @param originalSignature SHA-1 of the certificate the original app is signed with.
 * Google sign-in is set up for it only, so GmsCore presents it to Google
 * in place of the one of the patched app.
 */
internal fun gmsCoreSupportResourcePatch(packageName: String, originalSignature: String) = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0)

            // GmsCore presents the original package name and signature to Google.
            mapOf(
                "$GMS_CORE_PACKAGE_NAME.SPOOFED_PACKAGE_NAME" to packageName,
                "$GMS_CORE_PACKAGE_NAME.SPOOFED_PACKAGE_SIGNATURE" to originalSignature,
            ).forEach { (name, value) ->
                application.appendChild(
                    document.createElement("meta-data").apply {
                        setAttribute("android:name", name)
                        setAttribute("android:value", value)
                    }
                )
            }

            // Unlike Google Play services, GmsCore is not visible to other apps by default.
            val queries = document.getElementsByTagName("queries").item(0)
                ?: document.documentElement.appendChild(document.createElement("queries"))
            queries.appendChild(
                document.createElement("package").apply {
                    setAttribute("android:name", GMS_CORE_PACKAGE_NAME)
                }
            )

            // Receive push notifications from GmsCore.
            val elements = document.getElementsByTagName("*")
            for (elementIndex in 0 until elements.length) {
                val attributes = elements.item(elementIndex).attributes
                for (attributeIndex in 0 until attributes.length) {
                    val attribute = attributes.item(attributeIndex)
                    if (attribute.nodeValue.startsWith("com.google.android.c2dm.")) {
                        attribute.nodeValue = attribute.nodeValue.toGmsCore()
                    }
                }
            }
        }
    }
}

/**
 * Makes the Google Play services library of the app use GmsCore instead of Google Play services.
 */
internal fun BytecodePatchContext.useGmsCore() {
    // GmsCore is not signed by Google, so report it as available. Matched before
    // replacing the package name of Google Play services, which the method uses.
    val googlePlayServicesAvailabilityMethod = listOf(
        GooglePlayServicesAvailabilityFingerprint,
        GooglePlayServicesUtilLightAvailabilityFingerprint,
    ).firstNotNullOfOrNull { it.methodOrNull }
        ?: throw PatchException("Could not find the Google Play services availability check")

    googlePlayServicesAvailabilityMethod.addInstructions(
        0,
        """
            const/4 v0, 0x0
            return v0
        """
    )

    // Use GmsCore instead of Google Play services.
    val exactStrings = setOf("com.google", "com.google.android.gms") +
            PERMISSIONS + ACTIONS + AUTHORITIES + CREDENTIAL_TYPES
    val classes = exactStrings.flatMap { classDefByStrings(it) } +
            classDefByStrings("content://", StringComparisonType.STARTS_WITH)

    replaceStrings(classes.distinctBy { it.type }.map { it.type }) { string ->
        when {
            string in exactStrings -> string.toGmsCore()
            // Content URIs of Google Play services providers.
            AUTHORITIES.any { string.startsWith("content://$it") } -> string.toGmsCore()
            else -> null
        }
    }
}

private fun String.toGmsCore() = replaceFirst("com.google", GMS_CORE_VENDOR_GROUP_ID)

/**
 * Header in which Firebase sends the SHA-1 of the certificate of the app to Google.
 */
private const val ANDROID_CERT_HEADER = "X-Android-Cert"

/**
 * Makes Firebase send the certificate of the original app to Google, instead of the one
 * the patched app is signed with. An app can restrict its API key to its own certificate,
 * and Google then rejects the requests of the patched app, so it gets no Firebase installation
 * and no push token, and no notifications.
 *
 * @param originalSignature SHA-1 of the certificate the original app is signed with.
 */
internal fun BytecodePatchContext.spoofFirebaseCertificate(originalSignature: String) {
    // Firebase sends the hash in uppercase hexadecimal digits.
    val certificateHash = originalSignature.uppercase()

    classDefByStrings(ANDROID_CERT_HEADER).distinctBy { it.type }.forEach { classDef ->
        mutableClassDefBy(classDef.type).methods.forEach { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach

            // The header is set right after its name is loaded, such as with
            // setRequestProperty("X-Android-Cert", hash) of the connection.
            val valueLoads = instructions.mapIndexedNotNull { index, instruction ->
                if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO) {
                    return@mapIndexedNotNull null
                }
                val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                if (string != ANDROID_CERT_HEADER) return@mapIndexedNotNull null

                val nameRegister = (instruction as OneRegisterInstruction).registerA
                val callIndex = (index + 1 until instructions.size).firstOrNull {
                    nameRegister in instructions[it].argumentRegisters()
                }
                val call = callIndex?.let { instructions[it] }
                val arguments = call?.argumentRegisters().orEmpty()
                val parameterTypes = ((call as? ReferenceInstruction)?.reference as? MethodReference)?.parameterTypes
                if (parameterTypes?.map { it.toString() } != listOf("Ljava/lang/String;", "Ljava/lang/String;") ||
                    arguments.indexOf(nameRegister) != arguments.size - 2
                ) {
                    throw PatchException("Could not find where Firebase sets $ANDROID_CERT_HEADER in $method")
                }

                // The register only holds the hash of the header, so it can be replaced before the call.
                callIndex to arguments.last()
            }

            valueLoads.asReversed().forEach { (callIndex, valueRegister) ->
                method.addInstruction(callIndex, "const-string v$valueRegister, \"$certificateHash\"")
            }
        }
    }
}

private fun Instruction.argumentRegisters(): List<Int> = when (this) {
    is FiveRegisterInstruction -> listOf(registerC, registerD, registerE, registerF, registerG).take(registerCount)
    is RegisterRangeInstruction -> (startRegister until startRegister + registerCount).toList()
    else -> emptyList()
}

/**
 * Makes the app register for push notifications through GmsCore again, once.
 *
 * A push token the app got from Google Play services, before it used GmsCore, belongs to
 * Google Play services, which then receives the notifications but delivers them with an action
 * the patched app does not receive. Firebase keeps using the cached token for a week, and after that
 * GmsCore cannot register the same Firebase installation again (Google answers INTERNAL_SERVER_ERROR),
 * so notifications never come back on their own.
 *
 * @param extensionClass Extension class with resetPushRegistration(ContentProvider),
 * which deletes the registration.
 */
internal fun BytecodePatchContext.resetPushRegistration(extensionClass: String) {
    FirebaseInitProviderOnCreateFingerprint.method.addInstruction(
        0,
        "invoke-static/range { p0 .. p0 }, $extensionClass->" +
                "resetPushRegistration(Landroid/content/ContentProvider;)V"
    )
}

/**
 * Replaces the string constants of the classes, for which [transform] returns a replacement.
 */
private fun BytecodePatchContext.replaceStrings(
    classTypes: Iterable<String>,
    transform: (String) -> String?,
) {
    classTypes.forEach { classType ->
        mutableClassDefBy(classType).methods.forEach { method ->
            val instructions = method.implementation?.instructions ?: return@forEach

            // The count is read on every iteration, as aligning payloads after replacing
            // an instruction with a longer one can add or remove nop instructions.
            var index = -1
            while (++index < instructions.size) {
                val instruction = method.getInstruction(index)
                if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO) {
                    continue
                }

                val string = ((instruction as ReferenceInstruction).reference as StringReference).string
                val replacement = transform(string) ?: continue

                // The jumbo variant can reference any string, wherever it ends up in the dex file.
                method.replaceInstruction(
                    index,
                    BuilderInstruction31c(
                        Opcode.CONST_STRING_JUMBO,
                        (instruction as OneRegisterInstruction).registerA,
                        ImmutableStringReference(replacement),
                    )
                )
            }
        }
    }
}
