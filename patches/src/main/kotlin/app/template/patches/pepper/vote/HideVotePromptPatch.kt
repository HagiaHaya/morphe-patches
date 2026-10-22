package app.template.patches.pepper.vote

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_PEPPER

@Suppress("unused")
val hideVotePromptPatch = bytecodePatch(
    name = "Hide deal vote prompt",
    description = "Hides the \"How was this deal?\" prompt shown after returning from the shop " +
        "a deal links to, as for contributors from the Gold level up.",
    default = true
) {
    compatibleWith(COMPATIBILITY_PEPPER)

    execute {
        // Drop votePromptData.
        ThreadClickoutReturnDataConstructorFingerprint.method.addInstruction(0, "const/16 p1, 0x0")
    }
}
