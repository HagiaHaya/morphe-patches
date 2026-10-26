package app.template.extension.pepper;

import android.app.Activity;
import android.content.ContentProvider;

import app.template.extension.shared.GmsCoreSupport;

@SuppressWarnings("unused")
public final class GmsCoreSupportPatch {

    /**
     * Injection point. Called when the main activity is created.
     */
    public static void checkGmsCore(Activity activity) {
        GmsCoreSupport.checkGmsCore(activity, "Pepper", "Peppera");
    }

    /**
     * Injection point. Called before Firebase initializes.
     * <p>
     * Pepper sends the new push token to its servers itself, in place of the one of the device.
     */
    public static void resetPushRegistration(ContentProvider firebaseInitProvider) {
        GmsCoreSupport.resetPushRegistration(firebaseInitProvider.getContext());
    }
}
