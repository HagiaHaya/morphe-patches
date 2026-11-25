package app.template.patches.pepper.loyalty

import app.morphe.patcher.Fingerprint

/**
 * Loyalty program of the signed in user, as loaded from the REST API. Its name is kept,
 * because kotlinx.serialization needs it, and the app reads it through the getters only.
 */
private const val LOYALTY_PROGRAM_API_REPRESENTATION_CLASS =
    "Lcom/pepper/network/apirepresentation/LoyaltyProgramApiRepresentation;"

/**
 * Contributor level of the user: 0 (none), 1 (Silver), 2 (Gold) or 3 (Platinum),
 * or null if the user is not part of the loyalty program.
 */
internal object GetCurrentTierLevelFingerprint : Fingerprint(
    definingClass = LOYALTY_PROGRAM_API_REPRESENTATION_CLASS,
    name = "getCurrentTierLevel",
    returnType = "Ljava/lang/Integer;",
    parameters = listOf(),
)

/**
 * Points the user needs for the next contributor level, shown as
 * "10 points to unlock Platinum rewards!" while it is more than 0.
 */
internal object GetPointsToNextTierLevelFingerprint : Fingerprint(
    definingClass = LOYALTY_PROGRAM_API_REPRESENTATION_CLASS,
    name = "getPointsToNextTierLevel",
    returnType = "Ljava/lang/Integer;",
    parameters = listOf(),
)

/**
 * Whether to warn the user with "You are about to lose your rewards!",
 * because they did not earn enough points to keep their level.
 */
internal object IsTierLevelAboutToBeLostFingerprint : Fingerprint(
    definingClass = LOYALTY_PROGRAM_API_REPRESENTATION_CLASS,
    name = "isTierLevelAboutToBeLost",
    returnType = "Ljava/lang/Boolean;",
    parameters = listOf(),
)
