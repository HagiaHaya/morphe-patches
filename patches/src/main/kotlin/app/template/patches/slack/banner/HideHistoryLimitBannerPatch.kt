package app.template.patches.slack.banner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_SLACK

@Suppress("unused")
val hideHistoryLimitBannerPatch = bytecodePatch(
    name = "Hide history limit banner",
    description = "Hides the banner at the start of conversations that says messages older than " +
        "90 days are hidden on the free plan and offers a Pro trial.",
    default = true
) {
    compatibleWith(COMPATIBILITY_SLACK)

    execute {
        // Hide the banner instead of presenting it. The method suspends, but may return its
        // result right away. The banner parameter is a high register, which invoke-virtual
        // can not take, so it is copied first.
        PresentFreePlanBannerFingerprint.method.addInstructions(
            0,
            """
                move-object/from16 v1, p1
                const/16 v0, 0x8
                invoke-virtual { v1, v0 }, Landroid/view/View;->setVisibility(I)V
                sget-object v0, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                return-object v0
            """
        )
    }
}
