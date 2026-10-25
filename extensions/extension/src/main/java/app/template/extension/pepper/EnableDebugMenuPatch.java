package app.template.extension.pepper;

import android.app.Activity;
import android.util.Log;
import android.view.Menu;

import app.template.extension.pepper.settings.Settings;

/**
 * Shows the debug menu of the developers of Pepper on the home screen. Its items and their
 * actions are still in the app, but the release build no longer adds the menu.
 */
@SuppressWarnings("unused")
public final class EnableDebugMenuPatch {

    private static final String TAG = "MorphePepper";

    private static final String DEBUG_MENU = "activity_main_debug";

    /**
     * Injection point. Called when the home screen creates its options menu.
     */
    public static void addDebugMenu(Activity activity, Menu menu) {
        try {
            if (!Settings.isEnabled(Settings.DEBUG_MENU)) {
                return;
            }
            int menuId = activity.getResources().getIdentifier(DEBUG_MENU, "menu", activity.getPackageName());
            if (menuId != 0) {
                activity.getMenuInflater().inflate(menuId, menu);
            }
        } catch (Exception ex) {
            Log.e(TAG, "addDebugMenu failure", ex);
        }
    }
}
