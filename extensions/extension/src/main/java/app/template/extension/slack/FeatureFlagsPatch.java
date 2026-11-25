package app.template.extension.slack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.template.extension.slack.settings.Settings;
import slack.featureflag.FeatureFlagEnum;
import slack.services.featureflag.store.FeatureFlagStoreImpl;

/**
 * Overrides the feature flags of Slack, which turn its features and experiments on and off.
 */
@SuppressWarnings("unused")
public final class FeatureFlagsPatch {

    private static final String TAG = "MorpheSlack";

    private static final String PREFERENCES_NAME = "hagiahaya_feature_flags";

    private static final int NO_OVERRIDE = -1;

    /**
     * The value of a flag and if it is overridden.
     */
    public static final class Flag {
        public final String key;
        public final boolean value;

        Flag(String key, boolean value) {
            this.key = key;
            this.value = value;
        }
    }

    private static volatile Map<String, Boolean> overrides;

    private static volatile WeakReference<FeatureFlagStoreImpl> store = new WeakReference<>(null);

    private static SharedPreferences preferences() {
        Context context = Settings.getContext();
        return context == null ? null : context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static Map<String, Boolean> overrides() {
        Map<String, Boolean> loaded = overrides;
        if (loaded != null) {
            return loaded;
        }
        SharedPreferences preferences = preferences();
        if (preferences == null) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> map = new ConcurrentHashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                map.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }
        overrides = map;
        return map;
    }

    /**
     * Injection point. Called when the feature flags of a workspace are loaded.
     */
    public static void onStoreCreated(FeatureFlagStoreImpl featureFlagStore) {
        store = new WeakReference<>(featureFlagStore);
    }

    /**
     * Injection point. Called whenever Slack checks a feature flag.
     *
     * @return 1 or 0 to turn the flag on or off, or -1 to use its value.
     */
    public static int override(FeatureFlagEnum flag) {
        try {
            Map<String, Boolean> map = overrides();
            if (map.isEmpty() || !Settings.isEnabled(Settings.FEATURE_FLAGS)) {
                return NO_OVERRIDE;
            }
            Boolean value = map.get(flag.getKey());
            return value == null ? NO_OVERRIDE : value ? 1 : 0;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to override a feature flag", ex);
            return NO_OVERRIDE;
        }
    }

    /**
     * @return The flags of the workspace last loaded with their values from Slack, sorted by key,
     * or an empty list if no workspace was loaded yet.
     */
    public static List<Flag> getFlags() {
        List<Flag> flags = new ArrayList<>();
        FeatureFlagStoreImpl featureFlagStore = store.get();
        if (featureFlagStore == null) {
            return flags;
        }
        for (Map.Entry<Object, Boolean> entry : featureFlagStore.featureFlagMap.entrySet()) {
            try {
                String key = ((FeatureFlagEnum) entry.getKey()).getKey();
                flags.add(new Flag(key, Boolean.TRUE.equals(entry.getValue())));
            } catch (Exception ex) {
                Log.e(TAG, "Failed to read a feature flag", ex);
            }
        }
        Collections.sort(flags, (a, b) -> a.key.compareTo(b.key));
        return flags;
    }

    /**
     * @return The override of a flag, or null if it has none.
     */
    public static Boolean getOverride(String key) {
        return overrides().get(key);
    }

    /**
     * @param value The value to override the flag with, or null to remove the override.
     */
    public static void setOverride(String key, Boolean value) {
        SharedPreferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        Map<String, Boolean> map = overrides();
        if (value == null) {
            map.remove(key);
            preferences.edit().remove(key).apply();
        } else {
            map.put(key, value);
            preferences.edit().putBoolean(key, value).apply();
        }
    }

    public static int getOverrideCount() {
        return overrides().size();
    }

    public static void clearOverrides() {
        SharedPreferences preferences = preferences();
        if (preferences != null) {
            preferences.edit().clear().apply();
        }
        overrides().clear();
    }
}
