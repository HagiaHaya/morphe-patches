package app.template.patches.slack.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/**
 * Reads the credential the Google Play services provider of Credential Manager got,
 * which R8 merged into a result receiver of the provider:
 *
 * ```
 * val response = if (Build.VERSION.SDK_INT >= 34) extractGetCredentialResponse(intent)
 *     else GetCredentialResponse(Credential.createFrom(type, data))
 * ```
 *
 * On Android 14 and newer it expects the response as GetCredentialResponse of the
 * Android framework, but MicroG-RE always answers with a bundle, the format of Android 13
 * and older, so the credential is dropped as "No provider data returned".
 */
internal object HandleGetCredentialResponseFingerprint : Fingerprint(
    filters = listOf(
        methodCall(
            name = "extractGetCredentialResponse",
            parameters = listOf("Landroid/content/Intent;"),
            returnType = "Landroidx/credentials/GetCredentialResponse;",
        ),
    )
)

/**
 * The screen to sign in from, shown while signed out.
 */
internal object AppLandingActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lslack/features/applanding/AppLandingActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

/**
 * The main screen, shown while signed in.
 */
internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lslack/app/ui/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
