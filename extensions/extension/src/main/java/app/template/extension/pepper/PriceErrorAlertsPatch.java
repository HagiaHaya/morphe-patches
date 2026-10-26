package app.template.extension.pepper;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import app.template.extension.pepper.api.PepperApi;
import app.template.extension.pepper.settings.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static app.template.extension.pepper.DealMenu.text;

/**
 * Notifies of new deals with a discount of at least the percentage chosen in the settings,
 * such as price errors. Checks the newest deals every few minutes while the app runs, also in the
 * background, where an alarm wakes the frozen app up. If Android ends the app, it checks again
 * once the app is opened, as it can only make requests with the HTTP client of the app.
 */
@SuppressWarnings("unused")
public final class PriceErrorAlertsPatch {

    private static final String TAG = "MorphePepper";

    private static final long CHECK_INTERVAL_MINUTES = 10;

    private static final String NEW_DEALS = "thread?criteria=";

    private static final String NEW_DEALS_CRITERIA = "{\"tab\":\"new\",\"whereabouts\":\"deals\",\"show_nsfw\":false}";

    private static final String NOTIFICATION_CHANNEL_ID = "hagiahaya_price_errors";

    private static final String NOTIFICATION_TAG = "hagiahaya_price_error";

    private static final String STATE_PREFERENCES = "hagiahaya_price_errors";

    /**
     * When the newest deal checked was submitted, in seconds.
     */
    private static final String LAST_SUBMITTED = "last_submitted";

    private static Context applicationContext;

    private static ScheduledExecutorService executor;

    /**
     * Injection point. Called when the app is created.
     */
    public static synchronized void initialize(Application application) {
        if (executor != null) {
            return;
        }
        applicationContext = application;
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "PriceErrorAlerts");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(PriceErrorAlertsPatch::check, 1, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Called by {@link PriceErrorAlarmReceiver} when the alarm goes off.
     */
    static void onAlarm(Runnable onDone) {
        if (executor == null) {
            onDone.run();
            return;
        }
        executor.execute(() -> {
            try {
                check();
            } finally {
                onDone.run();
            }
        });
    }

    private static void check() {
        int minDiscount = Settings.getInt(Settings.PRICE_ERROR_DISCOUNT);
        try {
            if (minDiscount <= 0 || !PepperApi.isReady()) {
                return;
            }

            JSONArray deals = new JSONObject(PepperApi.get(NEW_DEALS + URLEncoder.encode(NEW_DEALS_CRITERIA, "UTF-8")
                    + "&limit=50")).optJSONArray("data");
            if (deals == null) {
                return;
            }

            SharedPreferences state = applicationContext.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE);
            long lastSubmitted = state.getLong(LAST_SUBMITTED, 0);
            long newestSubmitted = lastSubmitted;
            for (int i = 0; i < deals.length(); i++) {
                JSONObject deal = deals.getJSONObject(i);
                long submitted = deal.optLong("submitted");
                newestSubmitted = Math.max(newestSubmitted, submitted);
                // The first check only notes where to start.
                if (lastSubmitted == 0 || submitted <= lastSubmitted || deal.optBoolean("expired")) {
                    continue;
                }
                int discount = discount(deal);
                if (discount >= minDiscount) {
                    showNotification(deal, discount);
                }
            }
            state.edit().putLong(LAST_SUBMITTED, newestSubmitted).apply();
        } catch (Exception ex) {
            Log.w(TAG, "Failed to check for price errors: " + ex);
        } finally {
            scheduleAlarm(minDiscount);
        }
    }

    /**
     * @return The discount of a deal in percent, or 0 if unknown.
     */
    private static int discount(JSONObject deal) {
        if (!deal.isNull("price_discount")) {
            return deal.optInt("price_discount");
        }
        double price = deal.optDouble("price", Double.NaN);
        double nextBestPrice = deal.optDouble("next_best_price", Double.NaN);
        if (deal.optBoolean("display_price_as_free")) {
            price = 0;
        }
        if (Double.isNaN(price) || Double.isNaN(nextBestPrice) || nextBestPrice <= 0 || price >= nextBestPrice) {
            return 0;
        }
        return (int) Math.round((nextBestPrice - price) * 100 / nextBestPrice);
    }

    private static void showNotification(JSONObject deal, int discount) {
        Context context = applicationContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                text("Błędy cenowe", "Price errors"), NotificationManager.IMPORTANCE_HIGH));

        StringBuilder message = new StringBuilder(deal.optString("price_display"));
        String nextBestPrice = deal.optString("next_best_price_display");
        if (!TextUtils.isEmpty(nextBestPrice) && !nextBestPrice.equals("null")) {
            message.append(text(" zamiast ", " instead of ")).append(nextBestPrice);
        }
        JSONObject merchant = deal.optJSONObject("merchant");
        if (merchant != null && !TextUtils.isEmpty(merchant.optString("name"))) {
            message.append(" · ").append(merchant.optString("name"));
        }

        int id = Long.hashCode(deal.optLong("thread_id"));
        Notification.Builder builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("-" + discount + "% " + deal.optString("title"))
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(deal.optString("title") + "\n" + message))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_RECOMMENDATION);

        String link = deal.optString("deal_uri");
        if (!TextUtils.isEmpty(link)) {
            // The app opens its links on the screen of the deal.
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(context.getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder.setContentIntent(PendingIntent.getActivity(context, id, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }

        int iconId = context.getResources().getIdentifier("ic_android_notification_small_new_deal", "drawable",
                context.getPackageName());
        builder.setSmallIcon(iconId != 0 ? iconId : context.getApplicationInfo().icon);
        int colorId = context.getResources().getIdentifier("static_graphic_accent_brand", "color", context.getPackageName());
        if (colorId != 0) {
            builder.setColor(context.getColor(colorId));
        }

        notificationManager.notify(NOTIFICATION_TAG, id, builder.build());
    }

    /**
     * Sets the alarm to go off after the interval, or cancels it if alerts are off.
     * While the app runs, the checks keep moving the alarm, so it only goes off when the app is frozen.
     */
    private static void scheduleAlarm(int minDiscount) {
        try {
            AlarmManager alarmManager = applicationContext.getSystemService(AlarmManager.class);
            PendingIntent alarm = PendingIntent.getBroadcast(applicationContext, 0,
                    new Intent(applicationContext, PriceErrorAlarmReceiver.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (minDiscount <= 0 || !PepperApi.isReady()) {
                alarmManager.cancel(alarm);
                return;
            }
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(CHECK_INTERVAL_MINUTES), alarm);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to set the alarm to check for price errors", ex);
        }
    }
}
