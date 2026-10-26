package app.template.patches.shared.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

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
 * The same method in versions of the library without these strings, in apps
 * that keep the names of the library classes.
 */
internal object GooglePlayServicesUtilLightAvailabilityFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/common/GooglePlayServicesUtilLight;",
    name = "isGooglePlayServicesAvailable",
    returnType = "I",
    parameters = listOf("Landroid/content/Context;", "I"),
)

/**
 * Initializes Firebase when the app starts, before the application is created.
 */
internal object FirebaseInitProviderOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/provider/FirebaseInitProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = listOf(),
)

/**
 * isAvailableOnDevice() of the Credential Manager provider of the Android framework.
 *
 * Apps that sign in with Google through Credential Manager use this provider on
 * Android 14 and newer. It hands the request to the credential service of the system,
 * which asks Google Play services directly instead of going through the Google Play services
 * library of the app, so GmsCore never gets the request. The method name is kept, as
 * Credential Manager loads its providers by reflection.
 */
internal object FrameworkCredentialProviderAvailabilityFingerprint : Fingerprint(
    name = "isAvailableOnDevice",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(definingClass = "this", type = "Landroid/credentials/CredentialManager;"),
    )
)
