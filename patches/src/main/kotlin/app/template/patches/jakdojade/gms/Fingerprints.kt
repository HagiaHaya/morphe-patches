package app.template.patches.jakdojade.gms

import app.morphe.patcher.Fingerprint

/**
 * onCreate() of the launcher activity, where the check that asks to install GmsCore is injected.
 * The activity is the MAIN/LAUNCHER entry of the app and keeps its name across releases.
 */
internal object SplashScreenActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/citynav/jakdojade/pl/android/SplashScreenActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
