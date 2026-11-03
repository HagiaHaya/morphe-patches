package app.template.patches.pepper.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/RemoveAdsPatch;"

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes the ads shown between deals and on the deal pages.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(EXTENSION_CLASS, "areAdsEnabled")

        addPatchedFeature("removeAds")

        // Let the extension turn !isAdFree to false, so the deal lists and the deal pages
        // never get any ad slots, instead of leaving empty ones behind.
        AreAdsEnabledFingerprint.let {
            val xorIndex = it.instructionMatches[2].index
            val register = it.method.getInstruction<OneRegisterInstruction>(xorIndex).registerA

            it.method.addInstructions(
                xorIndex + 1,
                """
                    invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->areAdsEnabled(Z)Z
                    move-result v$register
                """
            )
        }
    }
}
