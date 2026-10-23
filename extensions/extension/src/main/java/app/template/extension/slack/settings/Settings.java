package app.template.extension.slack.settings;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Settings of the patches, changed on the screen added to the preferences of Slack.
 */
@SuppressWarnings("unused")
public final class Settings {

    static final String TAG = "MorpheSlack";

    /**
     * Shared with the switch of Keep message history, which predates this screen.
     */
    private static final String PREFERENCES_NAME = "hagiahaya_patches";

    public static final String SILENT_TYPING = "silent_typing";
    public static final String BLOCK_TELEMETRY = "block_telemetry";
    public static final String SANITIZE_LINKS = "sanitize_links";
    public static final String ANONYMIZE_FILE_NAMES = "anonymize_file_names";
    public static final String KEEP_DELETED_MESSAGES = "keep_deleted_messages";
    public static final String KEEP_EDIT_HISTORY = "keep_edit_history";
    public static final String STANDARD_MARKDOWN = "standard_markdown";
    public static final String NICKNAMES = "nicknames";
    public static final String FEATURE_FLAGS = "feature_flags";
    public static final String HIDE_HISTORY_LIMIT_BANNER = "hide_history_limit_banner";
    public static final String KEEP_MESSAGE_HISTORY = "keep_message_history";

    private static Context appContext;

    private static SharedPreferences preferences;

    /**
     * The activity of Slack in front, from which the screens of the patches open.
     */
    private static WeakReference<Activity> resumedActivity = new WeakReference<>(null);

    /**
     * Injection point. Called when the app is created.
     */
    public static void initialize(Context context) {
        try {
            appContext = context.getApplicationContext();
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityTracker());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load the settings", ex);
        }
    }

    /**
     * @return The application context, or null before the app is created.
     */
    public static Context getContext() {
        return appContext;
    }

    static Activity getResumedActivity() {
        return resumedActivity.get();
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

    private static final class ActivityTracker implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            resumedActivity = new WeakReference<>(activity);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
