package app.template.patches.slack.banner

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * Shows the banner at the start of a conversation of a free workspace, saying that messages and
 * files older than 90 days are hidden on the current plan, with an offer of a Pro trial.
 * R8 merged its class, MessageLimitEmptyStateHelperImpl, into another one, but kept the name.
 */
internal object PresentFreePlanBannerFingerprint : Fingerprint(
    name = "presentFreePlanBanner",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Lslack/uikit/components/banner/SKBanner;",
        "Lkotlin/coroutines/jvm/internal/ContinuationImpl;",
    ),
    filters = listOf(
        string("pro_trial_awareness_banner"),
    ),
)
