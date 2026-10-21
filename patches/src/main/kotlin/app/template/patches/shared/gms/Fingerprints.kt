package app.template.patches.shared.gms

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
