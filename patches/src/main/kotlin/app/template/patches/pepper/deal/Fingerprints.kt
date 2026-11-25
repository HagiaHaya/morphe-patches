package app.template.patches.pepper.deal

import app.morphe.patcher.Fingerprint

private const val DEAL_THREAD_ACTIVITY = "Lcom/pepper/apps/android/app/activity/DealThreadActivity;"

internal object DealThreadOnCreateOptionsMenuFingerprint : Fingerprint(
    definingClass = DEAL_THREAD_ACTIVITY,
    name = "onCreateOptionsMenu",
    returnType = "Z",
    parameters = listOf("Landroid/view/Menu;"),
)

internal object DealThreadOnOptionsItemSelectedFingerprint : Fingerprint(
    definingClass = DEAL_THREAD_ACTIVITY,
    name = "onOptionsItemSelected",
    returnType = "Z",
    parameters = listOf("Landroid/view/MenuItem;"),
)
