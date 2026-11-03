package app.template.patches.pepper.tracking

import app.morphe.patcher.Fingerprint

/**
 * Derives the device ID, which the app sends with each request and registers the device with:
 * `md5(Settings.Secure.ANDROID_ID ?: Build.SERIAL)`.
 */
internal object DeviceIdFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/ContentResolver;"),
    strings = listOf("android_id", "MD5"),
)

/**
 * `ListenableWorker.Result.Success.toString()` of WorkManager, which R8 obfuscated.
 */
internal object WorkerResultSuccessToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Success {mOutputData="),
)

/**
 * Sets the address of the Iterable API, which sends marketing events and messages.
 */
internal object IterableApiUrlFingerprint : Fingerprint(
    strings = listOf("https://api.iterable.com/api/"),
)
