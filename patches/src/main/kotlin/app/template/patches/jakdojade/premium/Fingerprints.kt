package app.template.patches.jakdojade.premium

import app.morphe.patcher.Fingerprint

/**
 * The method of the premium manager that decides whether premium is active. It returns true when
 * a premium subscription was purchased, when the city of the user offers a partnership premium, or
 * when the "force-premium" runtime flag is set, and is read wherever the app gates a premium
 * feature. Found by the unique "force-premium" flag name, as the class name is obfuscated.
 */
internal object PremiumActiveFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("force-premium"),
)

/**
 * Reads the "forcedTestPremium" developer flag from the app settings. It is a hidden test switch,
 * honored by the billing, account and analytics code on top of the regular premium check, so it is
 * forced on as well to keep every premium path consistent. Found by the unique flag name; the
 * setter that writes the flag returns void and the account screen lambda returns an object, so a
 * no-argument method that returns a boolean matches only this getter.
 */
internal object ForcedTestPremiumFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("forcedTestPremium"),
)
