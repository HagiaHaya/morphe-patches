package app.template.extension.pepper.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Settings of the patches, changed on the screen added to the settings of Pepper.
 */
@SuppressWarnings("unused")
public final class Settings {

    private static final String TAG = "MorphePepper";

    private static final String PREFERENCES_NAME = "hagiahaya_patches";

    public static final String REMOVE_ADS = "remove_ads";
    public static final String SANITIZE_SHARING_LINKS = "sanitize_sharing_links";
    public static final String SANITIZE_DEAL_LINKS = "sanitize_deal_links";
    public static final String UNLOCK_CUSTOM_APP_ICONS = "unlock_custom_app_icons";

    /**
     * Contributor level to show, from 1 (Silver) to 3 (Platinum), or 0 to show the one earned.
     */
    public static final String CONTRIBUTOR_LEVEL = "contributor_level";

    /**
     * What to do when a Flamedeer of the Flamedeer hunt shows up, one of the FLAMEDEER_MODE values.
     */
    public static final String FLAMEDEER_MODE = "flamedeer_mode";

    /**
     * Only the app shows the Flamedeer.
     */
    public static final int FLAMEDEER_MODE_OFF = 0;

    /**
     * A notification opens the prize screen.
     */
    public static final int FLAMEDEER_MODE_NOTIFY = 1;

    /**
     * The prize screen opens at once.
     */
    public static final int FLAMEDEER_MODE_CATCH = 2;

    /**
     * Minutes without a request of the app after which to look for a Flamedeer, or 0 not to.
     */
    public static final String FLAMEDEER_SEARCH_INTERVAL = "flamedeer_search_interval";

    private static SharedPreferences preferences;

    /**
     * Injection point. Called when the app is created.
     */
    public static void initialize(Context context) {
        try {
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load the settings", ex);
        }
    }

    /**
     * @return If the feature is turned on. Features are on until turned off.
     */
    public static boolean isEnabled(String key) {
        return preferences == null || preferences.getBoolean(key, true);
    }

    static void setEnabled(String key, boolean enabled) {
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).apply();
        }
    }

    public static int getContributorLevel() {
        return preferences == null ? 0 : preferences.getInt(CONTRIBUTOR_LEVEL, 0);
    }

    static void setContributorLevel(int level) {
        if (preferences != null) {
            preferences.edit().putInt(CONTRIBUTOR_LEVEL, level).apply();
        }
    }

    public static int getFlamedeerMode() {
        return preferences == null ? FLAMEDEER_MODE_CATCH : preferences.getInt(FLAMEDEER_MODE, FLAMEDEER_MODE_CATCH);
    }

    static void setFlamedeerMode(int mode) {
        if (preferences != null) {
            preferences.edit().putInt(FLAMEDEER_MODE, mode).apply();
        }
    }

    public static int getFlamedeerSearchInterval() {
        return preferences == null ? 0 : preferences.getInt(FLAMEDEER_SEARCH_INTERVAL, 0);
    }

    static void setFlamedeerSearchInterval(int minutes) {
        if (preferences != null) {
            preferences.edit().putInt(FLAMEDEER_SEARCH_INTERVAL, minutes).apply();
        }
    }
}
