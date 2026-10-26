package app.template.extension.pepper.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
    public static final String DEBUG_MENU = "debug_menu";
    public static final String PRICE_WITH_SHIPPING = "price_with_shipping";
    public static final String RANDOM_DEVICE_ID = "random_device_id";

    // Deal filters: lists have one entry per line, numbers are empty when not set.
    public static final String FILTER_KEYWORDS = "filter_keywords";
    public static final String FILTER_MERCHANTS = "filter_merchants";
    public static final String FILTER_USERS = "filter_users";
    public static final String FILTER_CATEGORIES = "filter_categories";
    public static final String FILTER_MAX_PRICE = "filter_max_price";
    public static final String FILTER_MIN_TEMPERATURE = "filter_min_temperature";

    /**
     * Words that hide a comment, one per line.
     */
    public static final String COMMENT_FILTER_WORDS = "comment_filter_words";

    /**
     * Discount in percent from which a new deal gets a notification, or 0 for none.
     */
    public static final String PRICE_ERROR_DISCOUNT = "price_error_discount";

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

    public static boolean getBoolean(String key, boolean defaultValue) {
        return preferences == null ? defaultValue : preferences.getBoolean(key, defaultValue);
    }

    static void setEnabled(String key, boolean enabled) {
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).apply();
        }
    }

    public static String getString(String key) {
        return preferences == null ? "" : preferences.getString(key, "");
    }

    public static void setString(String key, String value) {
        if (preferences != null) {
            preferences.edit().putString(key, value).apply();
        }
    }

    public static int getInt(String key) {
        return preferences == null ? 0 : preferences.getInt(key, 0);
    }

    static void setInt(String key, int value) {
        if (preferences != null) {
            preferences.edit().putInt(key, value).apply();
        }
    }

    /**
     * @return The non-empty lines of a setting, without surrounding spaces.
     */
    public static List<String> getLines(String key) {
        List<String> lines = new ArrayList<>();
        for (String line : getString(key).split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
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
