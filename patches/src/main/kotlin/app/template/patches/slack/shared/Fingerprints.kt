package app.template.patches.slack.shared

import app.morphe.patcher.Fingerprint

/**
 * The application class is named in the manifest, so its name is kept.
 */
internal object SlackAppOnCreateFingerprint : Fingerprint(
    definingClass = "Lslack/app/SlackApp;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
)
