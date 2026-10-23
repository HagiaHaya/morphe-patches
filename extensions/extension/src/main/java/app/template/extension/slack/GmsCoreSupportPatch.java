package app.template.extension.slack;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;

import app.template.extension.shared.GmsCoreSupport;

@SuppressWarnings("unused")
public final class GmsCoreSupportPatch {

    private static final String TAG = "MorpheSlack";

    private static final String PREFERENCES_NAME = "hagiahaya_gms_core";

    /**
     * If the push registration from before Slack used GmsCore was deleted.
     */
    private static final String PUSH_REGISTRATION_RESET_KEY = "push_registration_reset";

    /**
     * Prefix of the files in which Firebase keeps the installation, the ID the push token is issued for.
     */
    private static final String FIREBASE_INSTALLATION_FILE_PREFIX = "PersistedInstallation.";

    /**
     * Preferences in which Firebase caches the push token.
     */
    private static final String FIREBASE_MESSAGING_PREFERENCES_NAME = "com.google.android.gms.appid";

    /**
     * Preferences in which Slack keeps the push token it registers with its servers.
     */
    private static final String SLACK_PUSH_PREFERENCES_NAME = "GCM_PREFS";
    private static final String SLACK_PUSH_TOKEN_KEY = "KEY_GCM_REG_ID";

    /**
     * Injection point. Called when the sign in screen or the main screen is created.
     */
    public static void checkGmsCore(Activity activity) {
        GmsCoreSupport.checkGmsCore(activity, "Slack", "Slacka");
    }

    /**
     * Injection point. Called before Firebase initializes.
     * <p>
     * Deletes the push registration once, so Slack registers again through GmsCore.
     * A push token from before Slack used GmsCore belongs to Google Play services, which then
     * receives the notifications but delivers them with an action the patched app does not receive.
     * Firebase keeps using the cached token for a week, and after that GmsCore cannot register
     * the same Firebase installation again (Google answers INTERNAL_SERVER_ERROR),
     * so notifications never come back on their own.
     */
    public static void resetPushRegistration(ContentProvider firebaseInitProvider) {
        try {
            Context context = firebaseInitProvider.getContext();
            SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            if (preferences.getBoolean(PUSH_REGISTRATION_RESET_KEY, false)) return;

            File[] installations = context.getNoBackupFilesDir().listFiles(
                    (directory, name) -> name.startsWith(FIREBASE_INSTALLATION_FILE_PREFIX));
            if (installations != null) {
                for (File installation : installations) {
                    if (!installation.delete()) {
                        Log.w(TAG, "Could not delete " + installation);
                    }
                }
            }

            context.getSharedPreferences(FIREBASE_MESSAGING_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit().clear().commit();

            // Without a stored token, Slack asks Firebase for a new one and registers it.
            context.getSharedPreferences(SLACK_PUSH_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit().remove(SLACK_PUSH_TOKEN_KEY).commit();

            preferences.edit().putBoolean(PUSH_REGISTRATION_RESET_KEY, true).apply();
            Log.i(TAG, "Reset the push registration");
        } catch (Exception ex) {
            Log.e(TAG, "resetPushRegistration failure", ex);
        }
    }
}
