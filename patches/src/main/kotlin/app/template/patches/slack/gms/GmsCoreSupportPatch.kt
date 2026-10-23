/*
 * Based on the GmsCore support patch of Morphe Patches, changed for Slack.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.template.patches.slack.gms

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK
import app.template.patches.shared.gms.FrameworkCredentialProviderAvailabilityFingerprint
import app.template.patches.shared.gms.gmsCoreSupportResourcePatch
import app.template.patches.shared.gms.useGmsCore
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * SHA-1 of the certificate the original app is signed with, the app signing key of Google Play.
 */
private const val ORIGINAL_SIGNATURE = "c2a4e59ef0ab8081c671b28d89a8586647b5bf3e"

private const val ANDROID_13_SDK_INT = 33

private const val EXTENSION_CLASS = "Lapp/template/extension/slack/GmsCoreSupportPatch;"

@Suppress("unused")
val gmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore support",
    description = "Allows signing in with Google and receiving notifications " +
        "by using MicroG-RE instead of Google Play services.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    dependsOn(gmsCoreSupportResourcePatch(COMPATIBILITY_SLACK.packageName!!, ORIGINAL_SIGNATURE))

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "checkGmsCore", "resetPushRegistration")

        useGmsCore()

        // Report the provider of the Android framework as unavailable, so Credential Manager
        // signs in with Google through its Google Play services provider, which GmsCore handles.
        FrameworkCredentialProviderAvailabilityFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """
        )

        // Read the response of GmsCore in the format of Android 13, which it answers with.
        // The Android version read right before the response is extracted decides the format,
        // and also the format of the errors.
        HandleGetCredentialResponseFingerprint.let {
            val extractIndex = it.instructionMatches.first().index
            val sdkIntIndex = it.method.instructions.subList(0, extractIndex).indexOfLast { instruction ->
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                instruction.opcode == Opcode.SGET && field?.definingClass == "Landroid/os/Build\$VERSION;" &&
                    field.name == "SDK_INT"
            }
            check(sdkIntIndex >= 0) { "Could not find the read of the Android version" }
            val register = it.method.getInstruction<OneRegisterInstruction>(sdkIntIndex).registerA

            it.method.replaceInstruction(sdkIntIndex, "const/16 v$register, $ANDROID_13_SDK_INT")
        }

        // Register for push notifications again through GmsCore once, as a push token
        // Slack got before it used GmsCore belongs to Google Play services.
        FirebaseInitProviderOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->" +
                "resetPushRegistration(Landroid/content/ContentProvider;)V"
        )

        // Ask to install GmsCore if it is missing, on the first screen shown signed out or in.
        listOf(AppLandingActivityOnCreateFingerprint, MainActivityOnCreateFingerprint).forEach {
            it.method.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkGmsCore(Landroid/app/Activity;)V"
            )
        }
    }
}
