package app.template.patches.pepper.loyalty

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val spoofContributorLevelPatch = bytecodePatch(
    name = "Spoof contributor level",
    description = "Shows a contributor level of the loyalty program in the app, such as on the profile, " +
        "instead of the one earned. It unlocks the app icons of the level and hides the deal vote prompt " +
        "from the Gold level up, but rewards granted by Pepper, such as more deal stats, are not unlocked.",
    default = false
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    val contributorLevel by intOption(
        key = "contributorLevel",
        default = 3,
        values = mapOf(
            "Silver" to 1,
            "Gold" to 2,
            "Platinum" to 3,
        ),
        title = "Contributor level",
        description = "The contributor level to show.",
        required = true,
    )

    execute {
        /**
         * Makes an Integer getter return [value] instead, but keep returning null,
         * which means that the user is not part of the loyalty program.
         */
        fun MutableMethod.returnUnlessNull(value: Int) {
            val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
            val register = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructionsWithLabels(
                returnIndex,
                """
                    if-eqz v$register, :keep
                    const/16 v$register, $value
                    invoke-static { v$register }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                    move-result-object v$register
                """,
                ExternalLabel("keep", getInstruction(returnIndex))
            )
        }

        GetCurrentTierLevelFingerprint.method.returnUnlessNull(contributorLevel!!)

        // Hide "10 points to unlock Platinum rewards!", as for the highest level.
        GetPointsToNextTierLevelFingerprint.method.returnUnlessNull(0)

        // The warning is about the level earned.
        IsTierLevelAboutToBeLostFingerprint.method.addInstructions(
            0,
            """
                sget-object p0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object p0
            """
        )
    }
}
