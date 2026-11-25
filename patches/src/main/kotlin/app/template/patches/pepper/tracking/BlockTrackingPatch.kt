package app.template.patches.pepper.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.pepper.api.RealCallExecuteFingerprint
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/BlockTrackingPatch;"

/**
 * Workers that send the analytics events and the tracking pixels of deals and banners.
 * Their names are kept, as WorkManager creates them by name.
 */
private val TRACKING_WORKERS = listOf(
    "Lcom/pepper/analytics/backgroundjob/AnalyticsEventTransmissionWorker;",
    "Lcom/pepper/analytics/backgroundjob/ExternalTrackingPixelTransmissionWorker;",
)

/**
 * Content providers that start SDKs of ads and attribution when the app starts.
 */
private val SDK_PROVIDERS = listOf(
    "Lcom/facebook/ads/AudienceNetworkContentProvider;",
    "Lcom/vungle/ads/VungleProvider;",
    "Lcom/adjust/sdk/SystemLifecycleContentProvider;",
    "Lcom/google/android/gms/ads/MobileAdsInitProvider;",
)

private val blockTrackingResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0)
            mapOf(
                // Unlike firebase_analytics_collection_enabled, which the app turns on after consent,
                // this cannot be undone while the app runs.
                "firebase_analytics_collection_deactivated" to "true",
                "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
                "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
            ).forEach { (name, value) ->
                application.appendChild(
                    document.createElement("meta-data").apply {
                        setAttribute("android:name", name)
                        setAttribute("android:value", value)
                    }
                )
            }
        }
    }
}

@Suppress("unused")
val blockTrackingPatch = bytecodePatch(
    name = "Block tracking",
    description = "Blocks the analytics of Pepper, such as reports of the deals visited and shared and " +
        "tracking pixels, Firebase Analytics, Facebook app events, Adjust, Iterable and the start of the SDKs " +
        "of ads. Adds a setting to send a random device ID instead of one derived from the Android ID.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch, blockTrackingResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "onCallStarted", "getDeviceId")

        addPatchedFeature("blockTracking")

        // Cancel the calls of OkHttp to the analytics of Pepper when they start, so they fail without
        // connecting. Both execute() and enqueue() first check "Already Executed".
        RealCallExecuteFingerprint.classDef.let { realCallClass ->
            val constructor = realCallClass.methods.single {
                it.name == "<init>" && it.parameterTypes.size == 3 && it.parameterTypes[2] == "Z"
            }
            val requestType = constructor.parameterTypes[1].toString()
            val requestField = realCallClass.fields.single { it.type == requestType }

            realCallClass.methods.filter { method ->
                method.name != "<init>" && method.instructions.any {
                    it.opcode == Opcode.CONST_STRING &&
                        ((it as ReferenceInstruction).reference as StringReference).string == "Already Executed"
                }
            }.also { check(it.size == 2) { "Expected execute() and enqueue() of RealCall" } }.forEach { start ->
                start.addInstructions(
                    0,
                    """
                        iget-object v0, p0, ${realCallClass.type}->${requestField.name}:$requestType
                        invoke-static { p0, v0 }, $EXTENSION_CLASS->onCallStarted(Ljava/lang/Object;Ljava/lang/Object;)V
                    """
                )
            }
        }

        // Report the workers of analytics as done without sending anything.
        val successType = WorkerResultSuccessToStringFingerprint.classDef.type
        TRACKING_WORKERS.forEach { worker ->
            mutableClassDefBy(worker).methods.single {
                it.name != "<init>" && it.returnType == "Ljava/lang/Object;" && it.parameterTypes.size == 1
            }.addInstructions(
                0,
                """
                    new-instance v0, $successType
                    invoke-direct { v0 }, $successType-><init>()V
                    return-object v0
                """
            )
        }

        // Do not start Adjust and the SDKs of ads.
        mutableClassDefBy("Lcom/adjust/sdk/Adjust;").methods.single { it.name == "initSdk" }
            .addInstruction(0, "return-void")
        mutableClassDefBy("Lcom/google/android/gms/ads/MobileAds;").methods
            .filter { it.name == "initialize" && it.returnType == "V" }
            .forEach { it.addInstruction(0, "return-void") }
        SDK_PROVIDERS.forEach { provider ->
            mutableClassDefBy(provider).methods.single { it.name == "onCreate" && it.returnType == "Z" }
                .addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """
                )
        }

        // Send nothing to Iterable.
        IterableApiUrlFingerprint.method.apply {
            val index = instructions.indexOfFirst {
                it.opcode == Opcode.CONST_STRING &&
                    ((it as ReferenceInstruction).reference as StringReference).string == "https://api.iterable.com/api/"
            }
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-string v$register, \"https://127.0.0.1:1/api/\"")
        }

        // Optionally send a random device ID.
        DeviceIdFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS->getDeviceId()Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :original
                return-object v0
                :original
                nop
            """
        )
    }
}
