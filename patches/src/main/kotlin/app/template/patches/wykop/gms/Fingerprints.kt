package app.template.patches.wykop.gms

import app.morphe.patcher.Fingerprint

/**
 * The main activity is a Flutter activity that does not override onCreate(),
 * so the onCreate() of the Flutter activity it extends is patched instead.
 */
internal class ActivityOnCreateFingerprint(activityClass: String) : Fingerprint(
    definingClass = activityClass,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
