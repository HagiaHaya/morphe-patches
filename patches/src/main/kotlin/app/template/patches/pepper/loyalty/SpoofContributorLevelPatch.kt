package app.template.patches.pepper.loyalty

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.pepper.settings.addPatchedFeature
import app.template.patches.pepper.settings.settingsPatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import app.template.patches.shared.requireExtensionMethods
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/template/extension/pepper/SpoofContributorLevelPatch;"

@Suppress("unused")
val spoofContributorLevelPatch = bytecodePatch(
    name = "Spoof contributor level",
    description = "Adds a setting to show Silver, Gold or Platinum in the app, such as on the profile, " +
        "instead of the contributor level earned. It unlocks the app icons of the level, " +
        "but rewards granted by Pepper, such as more deal stats, are not unlocked.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    dependsOn(settingsPatch)

    extendWith("extensions/extension.mpe")

    execute {
        requireExtensionMethods(
            EXTENSION_CLASS,
            "getCurrentTierLevel",
            "getPointsToNextTierLevel",
            "isTierLevelAboutToBeLost",
        )

        addPatchedFeature("spoofContributorLevel")

        mapOf(
            GetCurrentTierLevelFingerprint to "getCurrentTierLevel(Ljava/lang/Integer;)Ljava/lang/Integer;",
            // Hide "10 points to unlock Platinum rewards!", as for the highest level.
            GetPointsToNextTierLevelFingerprint to "getPointsToNextTierLevel(Ljava/lang/Integer;)Ljava/lang/Integer;",
            // The warning is about the level earned.
            IsTierLevelAboutToBeLostFingerprint to "isTierLevelAboutToBeLost(Ljava/lang/Boolean;)Ljava/lang/Boolean;",
        ).forEach { (fingerprint, extensionMethod) ->
            fingerprint.method.apply {
                val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
                val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA

                addInstructions(
                    returnIndex,
                    """
                        invoke-static/range { v$register .. v$register }, $EXTENSION_CLASS->$extensionMethod
                        move-result-object v$register
                    """
                )
            }
        }
    }
}
