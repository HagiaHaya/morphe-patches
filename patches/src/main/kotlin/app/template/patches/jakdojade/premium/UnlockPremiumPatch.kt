package app.template.patches.jakdojade.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JAKDOJADE

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks the premium features, such as hiding ads, offline timetables and " +
        "departure notifications, without a subscription. Also turns on the hidden test premium " +
        "developer flag so the account screen and billing report premium as active.",
    default = true
) {
    compatibleWith(COMPATIBILITY_JAKDOJADE)

    execute {
        listOf(PremiumActiveFingerprint, ForcedTestPremiumFingerprint).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }
    }
}
