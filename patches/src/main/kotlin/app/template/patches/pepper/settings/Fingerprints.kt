package app.template.patches.pepper.settings

import app.morphe.patcher.Fingerprint

/**
 * The application class is named in the manifest, so its name is kept.
 */
internal object PepperApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pepper/apps/android/app/PepperApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf(),
)
