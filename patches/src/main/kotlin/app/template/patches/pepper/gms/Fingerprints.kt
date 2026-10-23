package app.template.patches.pepper.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

/**
 * Reads the credential the Google Play services provider of Credential Manager got.
 *
 * On Android 14 and newer it expects the response as GetCredentialResponse of the
 * Android framework, but MicroG-RE always answers with a bundle, the format of Android 13
 * and older, so the credential is dropped as "No provider data returned".
 */
internal object HandleGetCredentialResponseFingerprint : Fingerprint(
    definingClass = "Landroidx/credentials/playservices/controllers/ResponseUtils\$Companion;",
    name = "handleGetCredentialResponse",
    returnType = "V",
    filters = listOf(
        fieldAccess(smali = "Landroid/os/Build\$VERSION;->SDK_INT:I"),
    )
)

internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pepper/apps/android/presentation/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
