package app.template.patches.pepper.icon

import app.morphe.patcher.Fingerprint

/**
 * Launcher icon an app icon switches to. Its name is kept, because it is parcelable.
 */
internal const val MANIFEST_ALIAS_ID_CLASS = "Lcom/pepper/presentation/customappicon/ManifestAliasId;"

/**
 * Builds the list of the app icons the "App icon" screen offers. Each icon has the contributor
 * level of the loyalty program it needs, and the seasonal icons are only listed while
 * their sales event is running, such as Black Friday:
 *
 * ```
 * val icons = listOf(CustomAppIcon(..., ManifestAliasId.Silver, minTierLevel = LEVEL_1,
 *     analyticsName = "custom_app_icon_tier1_default", isEventThemingAppIcon = false), ...)
 * for (icon in icons) {
 *     if (loyaltyProgram == null && icon.minTierLevel != DEFAULT && !isSelected) continue
 *     if (icon.isEventThemingAppIcon && icon.manifestAliasId != runningEvent?.appIcon) continue
 *     val isUnlocked = icon.minTierLevel <= (loyaltyProgram?.currentTierLevel ?: DEFAULT)
 *     ...
 * }
 * ```
 */
internal object CustomAppIconListFingerprint : Fingerprint(
    strings = listOf("custom_app_icon_tier3_tight"),
)
