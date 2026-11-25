package app.template.extension.happycolor;

import android.app.Activity;
import android.content.ContentProvider;

import app.template.extension.shared.GmsCoreSupport;

@SuppressWarnings("unused")
public final class GmsCoreSupportPatch {

    /**
     * Injection point. Called when the main activity is created.
     */
    public static void checkGmsCore(Activity activity) {
        GmsCoreSupport.checkGmsCore(activity, "Happy Color", "Happy Color");
    }

    /**
     * Injection point. Called before Firebase initializes.
     */
    public static void resetPushRegistration(ContentProvider firebaseInitProvider) {
        GmsCoreSupport.resetPushRegistration(firebaseInitProvider.getContext());
    }
}
