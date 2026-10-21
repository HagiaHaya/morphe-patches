package app.template.patches.wykop.gms

import app.morphe.patcher.Fingerprint

/**
 * isGooglePlayServicesAvailable(Context, int) of the Google Play services library,
 * which also verifies that Google Play services is signed by Google.
 */
internal object GooglePlayServicesAvailabilityFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("MetadataValueReader", "This should never happen."),
)

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
