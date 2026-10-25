package app.template.patches.pepper.api

import app.morphe.patcher.Fingerprint

/**
 * `RealCall.execute()` of OkHttp, which R8 obfuscated, except for the names of the methods of
 * `okhttp3.Call`. RealCall is created with `RealCall(client, originalRequest, forWebSocket)`.
 */
internal object RealCallExecuteFingerprint : Fingerprint(
    name = "execute",
    parameters = listOf(),
    strings = listOf("Already Executed"),
)
