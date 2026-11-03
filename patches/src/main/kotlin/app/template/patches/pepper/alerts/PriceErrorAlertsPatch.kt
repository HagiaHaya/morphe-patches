package app.template.patches.pepper.alerts

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.pepper.api.pepperApiPatch
import app.template.patches.pepper.settings.PepperApplicationOnCreateFingerprint
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/PriceErrorAlertsPatch;"

private const val ALARM_RECEIVER = "app.template.extension.pepper.PriceErrorAlarmReceiver"

/**
 * Adds the receiver of the alarm that wakes the app up to check for price errors in the background.
 */
private val priceErrorAlertsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.getElementsByTagName("application").item(0).appendChild(
                document.createElement("receiver").apply {
                    setAttribute("android:name", ALARM_RECEIVER)
                    setAttribute("android:exported", "false")
                }
            )
        }
    }
}

@Suppress("unused")
val priceErrorAlertsPatch = bytecodePatch(
    name = "Price error alerts",
    description = "Adds a setting to get a notification of new deals with a discount of at least 70 to 95%, " +
        "such as price errors. Checks every 10 minutes, also in the background.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch, pepperApiPatch, priceErrorAlertsResourcePatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "initialize")

        addPatchedFeature("priceErrorAlerts")

        PepperApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->initialize(Landroid/app/Application;)V"
        )
    }
}
