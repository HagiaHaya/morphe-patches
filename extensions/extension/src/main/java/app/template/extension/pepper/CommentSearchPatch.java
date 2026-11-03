package app.template.extension.pepper;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import static app.template.extension.pepper.DealMenu.text;

/**
 * Adds "Search comments" to the menu of a deal, which opens {@link CommentSearchActivity}.
 */
@SuppressWarnings("unused")
public final class CommentSearchPatch {

    private static final String TAG = "MorphePepper";

    /**
     * Injection point. Called when the screen of a deal creates its menu.
     */
    public static void addMenuItem(Activity activity, Menu menu) {
        DealMenu.addItem(menu, DealMenu.SEARCH_COMMENTS, text("Szukaj w komentarzach", "Search comments"));
    }

    /**
     * Injection point. Called when an item of the menu of a deal is selected.
     *
     * @return If the item was handled.
     */
    public static boolean onMenuItemSelected(Activity activity, MenuItem item) {
        if (item.getItemId() != DealMenu.SEARCH_COMMENTS) {
            return false;
        }
        try {
            long threadId = DealMenu.getThreadId(activity);
            if (threadId >= 0) {
                activity.startActivity(new Intent(activity, CommentSearchActivity.class)
                        .putExtra(CommentSearchActivity.EXTRA_THREAD_ID, threadId));
            }
        } catch (Exception ex) {
            Log.e(TAG, "onMenuItemSelected failure", ex);
        }
        return true;
    }
}
