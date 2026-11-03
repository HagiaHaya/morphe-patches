package app.template.extension.pepper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.pepper.network.apirepresentation.ThreadFullApiRepresentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static app.template.extension.pepper.DealMenu.text;

/**
 * Adds "Add to calendar" to the menu of a deal. The event starts when the deal starts, or ends
 * when the deal expires, if the deal has these dates.
 */
@SuppressWarnings("unused")
public final class DealCalendarPatch {

    private static final String TAG = "MorphePepper";

    private static final int REMEMBERED_DEALS = 20;

    private static final long EVENT_DURATION = TimeUnit.MINUTES.toMillis(30);

    /**
     * Details of the deals the app loaded last, by ID.
     */
    private static final Map<Long, ThreadFullApiRepresentation> deals =
            new LinkedHashMap<Long, ThreadFullApiRepresentation>(REMEMBERED_DEALS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, ThreadFullApiRepresentation> eldest) {
                    return size() > REMEMBERED_DEALS;
                }
            };

    /**
     * Injection point. Called with the data of each response of the REST API.
     */
    public static Object rememberDeal(Object data, Object additionalData) {
        try {
            if (data instanceof ThreadFullApiRepresentation) {
                ThreadFullApiRepresentation deal = (ThreadFullApiRepresentation) data;
                synchronized (deals) {
                    deals.put(deal.getId(), deal);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "rememberDeal failure", ex);
        }
        return data;
    }

    /**
     * Injection point. Called when the screen of a deal creates its menu.
     */
    public static void addMenuItem(Activity activity, Menu menu) {
        DealMenu.addItem(menu, DealMenu.ADD_TO_CALENDAR, text("Dodaj do kalendarza", "Add to calendar"));
    }

    /**
     * Injection point. Called when an item of the menu of a deal is selected.
     *
     * @return If the item was handled.
     */
    public static boolean onMenuItemSelected(Activity activity, MenuItem item) {
        if (item.getItemId() != DealMenu.ADD_TO_CALENDAR) {
            return false;
        }
        try {
            ThreadFullApiRepresentation deal;
            synchronized (deals) {
                deal = deals.get(DealMenu.getThreadId(activity));
            }
            if (deal == null) {
                Toast.makeText(activity, text("Okazja jeszcze się wczytuje.", "The deal is still loading."),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            activity.startActivity(calendarIntent(deal));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(activity, text("Brak aplikacji kalendarza.", "No calendar app."), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Log.e(TAG, "onMenuItemSelected failure", ex);
        }
        return true;
    }

    private static Intent calendarIntent(ThreadFullApiRepresentation deal) {
        String title = deal.getTitle();
        long now = System.currentTimeMillis();
        long start = toMillis(deal.getStartDateInSeconds());
        long expiry = toMillis(deal.getExpiryDateInSeconds());
        long begin = 0;
        if (start > now) {
            begin = start;
        } else if (expiry > now) {
            begin = Math.max(now, expiry - EVENT_DURATION);
            title = text("Koniec okazji: ", "Deal ends: ") + title;
        }

        StringBuilder description = new StringBuilder();
        if (!TextUtils.isEmpty(deal.getPriceDisplay())) {
            description.append(deal.getPriceDisplay());
        }
        if (!TextUtils.isEmpty(deal.getMerchantNameDisplay())) {
            description.append(description.length() > 0 ? " · " : "").append(deal.getMerchantNameDisplay());
        }
        String link = !TextUtils.isEmpty(deal.getShareableLink()) ? deal.getShareableLink() : deal.getDealUri();
        if (!TextUtils.isEmpty(link)) {
            description.append(description.length() > 0 ? "\n" : "").append(link);
        }

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description.toString());
        if (begin > 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + EVENT_DURATION);
        }
        return intent;
    }

    private static long toMillis(Long seconds) {
        return seconds == null ? 0 : TimeUnit.SECONDS.toMillis(seconds);
    }
}
