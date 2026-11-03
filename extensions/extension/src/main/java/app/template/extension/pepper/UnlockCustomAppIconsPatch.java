package app.template.extension.pepper;

import android.util.Log;

import app.template.extension.pepper.settings.Settings;

@SuppressWarnings("unused")
public final class UnlockCustomAppIconsPatch {

    private static final String TAG = "MorphePepper";

    /**
     * Injection point. Called for each icon the "App icon" screen lists.
     *
     * @param minTierLevel Contributor level the icon needs, a constant of the level enum.
     * @return The lowest level, DEFAULT, to list and unlock the icon for everyone.
     */
    public static Object getMinTierLevel(Object minTierLevel) {
        try {
            if (Settings.isEnabled(Settings.UNLOCK_CUSTOM_APP_ICONS) && minTierLevel instanceof Enum) {
                // DEFAULT is the first constant.
                return ((Enum<?>) minTierLevel).getDeclaringClass().getEnumConstants()[0];
            }
        } catch (Exception ex) {
            Log.e(TAG, "getMinTierLevel failure", ex);
        }

        return minTierLevel;
    }

    /**
     * Injection point. Called for each icon the "App icon" screen lists.
     *
     * @param isEventThemingAppIcon If the icon is a seasonal one, only listed during its sales event.
     */
    public static boolean isEventThemingAppIcon(boolean isEventThemingAppIcon) {
        return isEventThemingAppIcon && !Settings.isEnabled(Settings.UNLOCK_CUSTOM_APP_ICONS);
    }
}
