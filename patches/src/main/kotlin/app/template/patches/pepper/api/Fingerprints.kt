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

/**
 * Constructor kotlinx.serialization creates the responses of the REST API with:
 * `ApiSuccessRepresentation(seenFields, data, additionalData, messages, marker)`.
 */
internal object ApiSuccessDeserializationConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/pepper/network/apirepresentation/ApiSuccessRepresentation;",
    name = "<init>",
    custom = { method, _ -> method.parameterTypes.firstOrNull() == "I" },
)
