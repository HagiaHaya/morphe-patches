package app.template.extension.slack;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.Context;
import android.util.Log;

import app.template.extension.shared.GmsCoreSupport;

@SuppressWarnings("unused")
public final class GmsCoreSupportPatch {

    private static final String TAG = "MorpheSlack";

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
     */
    public static void resetPushRegistration(ContentProvider firebaseInitProvider) {
        try {
            Context context = firebaseInitProvider.getContext();
            if (!GmsCoreSupport.resetPushRegistration(context)) return;

            // Without a stored token, Slack asks Firebase for a new one and registers it.
            context.getSharedPreferences(SLACK_PUSH_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit().remove(SLACK_PUSH_TOKEN_KEY).commit();
        } catch (Exception ex) {
            Log.e(TAG, "resetPushRegistration failure", ex);
        }
    }
}
