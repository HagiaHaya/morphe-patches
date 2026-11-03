package app.template.extension.pepper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import app.template.extension.pepper.api.PepperApi;
import app.template.extension.pepper.settings.Settings;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * While a Flamedeer hunt runs, the server can only report a Flamedeer with a request of the app.
 * When the app made no request for the interval chosen in the settings, such as while it is idle
 * or in the background, this makes one, so a Flamedeer is found anyway.
 * <p>
 * Android freezes the app soon after it goes to the background, so an alarm wakes it up for that.
 * If Android ends the app, it waits until the app is opened again, as it can only make requests
 * as the user with the HTTP client of the app.
 */
final class FlamedeerHunter {

    private static final String TAG = "MorphePepper";

    private static final String APP_PREFERENCES = "pepper_preferences";

    private static final String HUNT_TOKEN = "pepper_mascotcard_configuration_token";

    private static final String NO_HUNT_TOKEN = "NO_MASCOTCARD";

    /**
     * A small request of the signed-in user, which the server checks for a Flamedeer like any other.
     */
    private static final String REQUEST_PATH = "user";

    private static final long CHECK_PERIOD_SECONDS = 30;

    private static Context applicationContext;

    private static SharedPreferences appPreferences;

    private static ScheduledExecutorService executor;

    private FlamedeerHunter() {
    }

    static synchronized void start(Context context) {
        if (executor != null) {
            return;
        }
        applicationContext = context.getApplicationContext();
        appPreferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "FlamedeerHunter");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> check(false),
                CHECK_PERIOD_SECONDS, CHECK_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Called by {@link FlamedeerAlarmReceiver} when the alarm goes off.
     */
    static void onAlarm(Runnable onDone) {
        if (executor == null) {
            onDone.run();
            return;
        }
        executor.execute(() -> {
            try {
                check(true);
            } finally {
                onDone.run();
            }
        });
    }

    private static void check(boolean fromAlarm) {
        long intervalMillis = TimeUnit.MINUTES.toMillis(Settings.getFlamedeerSearchInterval());
        try {
            if (intervalMillis <= 0
                    || Settings.getFlamedeerMode() == Settings.FLAMEDEER_MODE_OFF
                    || !isHuntRunning()
                    || !PepperApi.isReady()) {
                return;
            }

            // An alarm can go off a little early.
            long idleMillis = SystemClock.elapsedRealtime() - PepperApi.getLastCallTime();
            if (idleMillis >= (fromAlarm ? intervalMillis / 2 : intervalMillis)) {
                Log.i(TAG, "Looking for a Flamedeer");
                PepperApi.get(REQUEST_PATH);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to look for a Flamedeer: " + ex);
        } finally {
            scheduleAlarm(intervalMillis);
        }
    }

    /**
     * Sets the alarm to go off after the interval, or cancels it if there is nothing to look for.
     * While the app runs, the checks keep moving the alarm, so it only goes off when the app is frozen.
     */
    private static void scheduleAlarm(long intervalMillis) {
        try {
            AlarmManager alarmManager = applicationContext.getSystemService(AlarmManager.class);
            PendingIntent alarm = PendingIntent.getBroadcast(applicationContext, 0,
                    new Intent(applicationContext, FlamedeerAlarmReceiver.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (intervalMillis <= 0
                    || Settings.getFlamedeerMode() == Settings.FLAMEDEER_MODE_OFF
                    || !isHuntRunning()
                    || !PepperApi.isReady()) {
                alarmManager.cancel(alarm);
                return;
            }
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis, alarm);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to set the alarm to look for a Flamedeer", ex);
        }
    }

    /**
     * @return If a hunt runs, as the app only saves the token of the hunt then.
     */
    private static boolean isHuntRunning() {
        String token = appPreferences.getString(HUNT_TOKEN, null);
        return token != null && !token.isEmpty() && !token.equals(NO_HUNT_TOKEN);
    }
}
