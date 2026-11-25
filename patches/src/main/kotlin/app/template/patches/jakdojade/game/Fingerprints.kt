package app.template.patches.jakdojade.game

import app.morphe.patcher.Fingerprint

internal object SplashScreenOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/citynav/jakdojade/pl/android/SplashScreenActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
