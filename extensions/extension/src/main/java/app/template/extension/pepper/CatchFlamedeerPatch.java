package app.template.extension.pepper;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import app.template.extension.pepper.settings.Settings;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Flamedeers ("Ogniste Renifery") of the Flamedeer hunt. While a hunt is running, the app asks
 * the server with every API request whether the user found one, and the server answers with
 * the ID of the prize. The app then shows the Flamedeer at the bottom of the screen until
 * it is tapped, which opens the prize screen that claims the prize.
 * <p>
 * Depending on the settings, this posts a notification that opens the prize screen,
 * or opens the prize screen at once. Flamedeers found while no screen of the app is open,
 * which the app would drop, always get a notification.
 */
@SuppressWarnings("unused")
public final class CatchFlamedeerPatch {

    private static final String TAG = "MorphePepper";

    private static final String PRIZE_ACTIVITY = "com.pepper.presentation.mascotcardprize.MascotcardPrizeActivity";

    private static final String EXTRA_PRIZE_ID = "mascotcardprize:extra:prize_id";

    private static final String NOTIFICATION_CHANNEL_ID = "hagiahaya_flamedeer";

    private static final String NOTIFICATION_TAG = "hagiahaya_flamedeer";

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Prizes already handled, as the server can report one with several requests.
     */
    private static final Set<Long> foundPrizeIds = Collections.synchronizedSet(new HashSet<>());

    /**
     * Prizes whose prize screen was opened, so the app does not show their Flamedeer.
     */
    private static final Set<Long> claimedPrizeIds = Collections.synchronizedSet(new HashSet<>());

    private static Context applicationContext;

    private static volatile WeakReference<Activity> resumedActivity = new WeakReference<>(null);

    /**
     * Activity the app last showed a Flamedeer in.
     */
    private static WeakReference<Activity> mascotActivity = new WeakReference<>(null);

    /**
     * Injection point. Called when the app is created.
     */
    public static void initialize(Application application) {
        applicationContext = application;
        FlamedeerHunter.start(application);
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                resumedActivity = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (resumedActivity.get() == activity) {
                    resumedActivity = new WeakReference<>(null);
                }
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    /**
     * Injection point. Called on a network thread when the server reports that the user found a Flamedeer.
     *
     * @param prizeId ID of the prize, which the prize screen claims.
     */
    public static void onFlamedeerFound(long prizeId) {
        try {
            int mode = Settings.getFlamedeerMode();
            Log.i(TAG, "Found a Flamedeer, prize " + prizeId);
            if (mode == Settings.FLAMEDEER_MODE_OFF || applicationContext == null || !foundPrizeIds.add(prizeId)) {
                return;
            }

            // Mark the prize before the app shows its Flamedeer, which this thread triggers next.
            boolean catchNow = mode == Settings.FLAMEDEER_MODE_CATCH && resumedActivity.get() != null;
            if (catchNow) {
                claimedPrizeIds.add(prizeId);
            }

            mainThreadHandler.post(() -> {
                Activity activity = resumedActivity.get();
                if (catchNow && activity != null && !activity.isFinishing()) {
                    try {
                        activity.startActivity(prizeIntent(activity, prizeId));
                        return;
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to open the prize screen", ex);
                    }
                }

                claimedPrizeIds.remove(prizeId);
                showNotification(prizeId, activity != null);
            });
        } catch (Exception ex) {
            Log.e(TAG, "onFlamedeerFound failure", ex);
        }
    }

    /**
     * Injection point. Called on the main thread before the app shows a Flamedeer.
     *
     * @return If the app must not show it, because its prize screen was opened already.
     */
    public static boolean hideFlamedeer(Activity activity, long prizeId) {
        if (claimedPrizeIds.contains(prizeId)) {
            return true;
        }
        mascotActivity = new WeakReference<>(activity);
        return false;
    }

    /**
     * Injection point. Called when the prize screen is created, by the patch or by tapping a Flamedeer.
     */
    public static void onPrizeScreenCreated(Activity prizeActivity) {
        try {
            long prizeId = prizeActivity.getIntent().getLongExtra(EXTRA_PRIZE_ID, -1);
            if (prizeId < 0) {
                return;
            }
            claimedPrizeIds.add(prizeId);

            NotificationManager notificationManager = prizeActivity.getSystemService(NotificationManager.class);
            notificationManager.cancel(NOTIFICATION_TAG, notificationId(prizeId));

            // The prize was opened from the notification, so remove the Flamedeer the app shows.
            Activity activity = mascotActivity.get();
            int viewId = resourceId(prizeActivity, "mascotcard_view", "id");
            View flamedeer = activity == null || viewId == 0 ? null : activity.findViewById(viewId);
            if (flamedeer != null && flamedeer.getParent() instanceof ViewGroup) {
                ((ViewGroup) flamedeer.getParent()).removeView(flamedeer);
            }
        } catch (Exception ex) {
            Log.e(TAG, "onPrizeScreenCreated failure", ex);
        }
    }

    private static void showNotification(long prizeId, boolean inForeground) {
        Context context = applicationContext;
        String title = text("Ognisty Renifer!", "A Flamedeer showed up!");
        String message = text("Dotknij, aby go złapać.", "Tap to catch it.");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // The Flamedeer of the app is on the screen.
            if (inForeground) {
                Toast.makeText(context, title, Toast.LENGTH_LONG).show();
            }
            return;
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                text("Ogniste Renifery", "Flamedeers"), NotificationManager.IMPORTANCE_HIGH));

        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId(prizeId),
                prizeIntent(context, prizeId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_EVENT);

        int iconId = resourceId(context, "ic_android_notification_small", "drawable");
        builder.setSmallIcon(iconId != 0 ? iconId : context.getApplicationInfo().icon);
        int colorId = resourceId(context, "static_graphic_accent_brand", "color");
        if (colorId != 0) {
            builder.setColor(context.getColor(colorId));
        }

        notificationManager.notify(NOTIFICATION_TAG, notificationId(prizeId), builder.build());
    }

    private static Intent prizeIntent(Context context, long prizeId) {
        return new Intent().setClassName(context, PRIZE_ACTIVITY).putExtra(EXTRA_PRIZE_ID, prizeId);
    }

    private static int notificationId(long prizeId) {
        return Long.hashCode(prizeId);
    }

    private static int resourceId(Context context, String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private static String text(String polishText, String englishText) {
        // The default locale follows the language chosen for the app.
        return "pl".equals(Locale.getDefault().getLanguage()) ? polishText : englishText;
    }
}
