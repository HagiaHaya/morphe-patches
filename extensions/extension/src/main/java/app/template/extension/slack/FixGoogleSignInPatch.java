package app.template.extension.slack;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;

@SuppressWarnings("unused")
public final class FixGoogleSignInPatch {

    private static final String AUTH_OPTIONS_KEY_EXTRA = "extra_auth_options_key";

    /**
     * Injection point. Called with the next step of signing in, which arrived
     * while the sign-in screen is shown.
     */
    public static void openGetStarted(Activity activity, Parcelable key) {
        Intent intent = new Intent(activity, activity.getClass()).putExtra(AUTH_OPTIONS_KEY_EXTRA, key);

        // Finish first, so the screen launched as singleTop opens again instead of
        // receiving the step as a new intent once more.
        activity.finish();
        activity.startActivity(intent);
    }
}
