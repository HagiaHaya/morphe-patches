package app.template.patches.pepper.debugmenu

import app.morphe.patcher.Fingerprint

/**
 * Creates the options menu of the home screen. The release build only calls the super method,
 * but `MainActivity.onOptionsItemSelected` still handles the items of the debug menu.
 */
internal object MainActivityOnCreateOptionsMenuFingerprint : Fingerprint(
    definingClass = "Lcom/pepper/apps/android/presentation/MainActivity;",
    name = "onCreateOptionsMenu",
    returnType = "Z",
    parameters = listOf("Landroid/view/Menu;"),
)
