package app.template.patches.slack.signin

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

private const val AUTH_OPTIONS_ACTIVITY_CLASS = "Lslack/features/authoptions/AuthOptionsActivity;"

/**
 * Receives the next step of signing in, while the sign-in screen is shown:
 *
 * ```
 * val key = intent.getParcelableExtra("extra_auth_options_key")
 * if (key is AuthOptionsIntentKey.GetStarted) {
 *     if (isAuthOptionsCircuitEnabled) authOptionsIntentRelay.channel.trySend(key)
 *     else authOptionsPresenter.handleGetStartedDeepLink(key)
 * }
 * ```
 */
internal object AuthOptionsActivityOnNewIntentFingerprint : Fingerprint(
    definingClass = AUTH_OPTIONS_ACTIVITY_CLASS,
    name = "onNewIntent",
    returnType = "V",
    parameters = listOf("Landroid/content/Intent;"),
    filters = listOf(
        fieldAccess(smali = "$AUTH_OPTIONS_ACTIVITY_CLASS->isAuthOptionsCircuitEnabled:Z"),
        fieldAccess(
            smali = "$AUTH_OPTIONS_ACTIVITY_CLASS->authOptionsIntentRelay:" +
                "Lslack/features/authoptions/circuit/AuthOptionsIntentRelay;"
        ),
    )
)
