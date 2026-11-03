package app.template.patches.pepper.debugmenu

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/EnableDebugMenuPatch;"

@Suppress("unused")
val enableDebugMenuPatch = bytecodePatch(
    name = "Enable debug menu",
    description = "Adds a setting to show the hidden debug menu of the developers of Pepper " +
        "at the top right of the home screen. Some of its actions can break the app.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "addDebugMenu")

        addPatchedFeature("enableDebugMenu")

        MainActivityOnCreateOptionsMenuFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p1 }, $EXTENSION_CLASS->addDebugMenu(Landroid/app/Activity;Landroid/view/Menu;)V"
        )
    }
}
