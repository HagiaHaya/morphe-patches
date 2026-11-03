package app.template.patches.pepper.flamedeer

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * Interceptor of the API requests. While a Flamedeer hunt runs, it asks the server with each
 * request whether the user found a Flamedeer, and passes on the prize the server answers with:
 *
 * ```
 * val token = preferences.getString("pepper_mascotcard_configuration_token", null)
 * if (token.isNullOrEmpty() || token == "NO_MASCOTCARD") return chain.proceed(request)
 * val response = chain.proceed(request.newBuilder().header("Pepper-Check-Mascotcard", "true").build())
 * val prizeId = response.header("Pepper-Mascotcard-Found") ?: return response
 * scope.launch { foundPrizes.emit(prizeId.toLong()) }
 * ```
 */
internal object FlamedeerHeaderInterceptorFingerprint : Fingerprint(
    filters = listOf(
        string("Pepper-Mascotcard-Found"),
        methodCall(smali = "Ljava/lang/Long;->parseLong(Ljava/lang/String;)J"),
    )
)

/**
 * Shows the Flamedeer of a prize found at the bottom of a screen. Tapping it opens the prize
 * screen, which claims the prize: `showFlamedeer(activity: Activity, prizeId: Long, applyInsets: Boolean)`.
 */
internal object ShowFlamedeerFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "J", "Z"),
    strings = listOf("popMascotcardView() mascotImageUri is NULL!"),
)

/**
 * The prize screen. It claims the prize of its extra, and shows what the user won.
 */
internal object PrizeScreenOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pepper/presentation/mascotcardprize/MascotcardPrizeActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
