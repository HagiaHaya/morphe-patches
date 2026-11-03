package app.template.patches.happycolor.gms

import app.morphe.patcher.Fingerprint

internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/coloring/game/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
