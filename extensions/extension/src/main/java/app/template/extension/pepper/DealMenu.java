package app.template.extension.pepper;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Locale;

/**
 * Items the patches add to the menu of a deal.
 */
final class DealMenu {

    static final int ADD_TO_CALENDAR = 0x48480001;

    static final int SEARCH_COMMENTS = 0x48480002;

    private DealMenu() {
    }

    static void addItem(Menu menu, int id, String title) {
        if (menu.findItem(id) == null) {
            menu.add(Menu.NONE, id, Menu.CATEGORY_SECONDARY, title)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
    }

    /**
     * @return The ID of the deal the screen shows, or -1.
     */
    static long getThreadId(Activity activity) {
        return activity.getIntent().getLongExtra(activity.getPackageName() + ".extra:thread_id", -1);
    }

    static String text(String polishText, String englishText) {
        // The default locale follows the language chosen for the app.
        return "pl".equals(Locale.getDefault().getLanguage()) ? polishText : englishText;
    }
}
