package app.template.extension.shared;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.util.Locale;

/**
 * Asks to install MicroG-RE, which the GmsCore support patches make apps use
 * instead of Google Play services, and to let it run in the background,
 * and keeps it connected for the push notifications of the app.
 */
public final class GmsCoreSupport {

    private static final String TAG = "MorpheGmsCore";

    private static final String GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms";

    private static final String GMS_CORE_DOWNLOAD_URL =
            "https://github.com/MorpheApp/MicroG-RE/releases/latest";

    /**
     * A content provider of GmsCore, which can only be reached while GmsCore can run.
     */
    private static final String GMS_CORE_PROVIDER_AUTHORITY = "app.revanced.android.gsf.gservices";

    /**
     * Receiver of GmsCore that connects for push notifications when it is not connected.
     */
    private static final String GMS_CORE_PUSH_TRIGGER_RECEIVER = "org.microg.gms.gcm.TriggerReceiver";

    /**
     * Any action the receiver does not know, so it honors the settings of GmsCore.
     */
    private static final String GMS_CORE_CONNECT_ACTION = "app.template.extension.CONNECT_GMS_CORE";

    /**
     * ID of {@link GmsCoreConnectionJob}, far above the IDs WorkManager gives the jobs of the app, from 0 up.
     */
    private static final int CONNECTION_JOB_ID = 0x4D6F7270;

    /**
     * How often {@link GmsCoreConnectionJob} runs, the shortest period Android allows.
     * Android runs it less often while the phone is idle or the app is rarely used.
     */
    private static final long CONNECTION_JOB_INTERVAL_MS = 15 * 60 * 1000;

    private static final String PREFERENCES_NAME = "hagiahaya_gms_core";

    /**
     * If the push registration from before the app used GmsCore was deleted.
     */
    private static final String PUSH_REGISTRATION_RESET_KEY = "push_registration_reset";

    /**
     * If the user chose not to be asked again to let GmsCore run in the background.
     */
    private static final String BACKGROUND_CHECK_DISABLED_KEY = "background_check_disabled";

    /**
     * If the user chose not to be asked again to let GmsCore access the location.
     */
    private static final String LOCATION_CHECK_DISABLED_KEY = "location_check_disabled";

    /**
     * Prefix of the files in which Firebase keeps the installation, the ID the push token is issued for.
     */
    private static final String FIREBASE_INSTALLATION_FILE_PREFIX = "PersistedInstallation.";

    /**
     * Preferences in which Firebase caches the push token.
     */
    private static final String FIREBASE_MESSAGING_PREFERENCES_NAME = "com.google.android.gms.appid";

    /**
     * If GmsCore was already checked for, so a dialog is shown once per app launch.
     */
    private static boolean checked;

    private GmsCoreSupport() {
    }

    /**
     * Shows a dialog that asks to install GmsCore, if it is missing, or to let it run in the background,
     * if Android may stop it. Notifications come through GmsCore, and stop when it does not run.
     *
     * @param appName       Name of the app in English, such as "Wykop".
     * @param appNamePolish Name of the app in the genitive case in Polish, such as "Wykopu".
     */
    public static void checkGmsCore(Activity activity, String appName, String appNamePolish) {
        checkGmsCore(activity, appName, appNamePolish, false);
    }

    /**
     * Like {@link #checkGmsCore(Activity, String, String)}, and for an app that gets the location
     * through GmsCore, first asks to let GmsCore access the location, if it can not.
     *
     * @param usesLocation If the app gets the location through GmsCore.
     */
    public static void checkGmsCore(Activity activity, String appName, String appNamePolish, boolean usesLocation) {
        if (checked) return;
        checked = true;

        // Reaching the content provider of GmsCore may have to start it first.
        new Thread(() -> {
            try {
                Problem problem = findProblem(activity, usesLocation);
                if (problem != Problem.NOT_INSTALLED) {
                    scheduleConnectionJob(activity.getApplicationContext());
                }
                // The location is checked before whether GmsCore can run, so it may as well connect.
                if (problem == null || problem == Problem.NO_LOCATION_ACCESS) {
                    connectGmsCore(activity);
                }
                if (problem != null) {
                    // Show the dialog once the activity is created.
                    activity.runOnUiThread(() -> showDialog(activity, problem, appName, appNamePolish));
                }
            } catch (Exception ex) {
                Log.e(TAG, "checkGmsCore failure", ex);
            }
        }).start();
    }

    /**
     * Asks GmsCore to connect to Google for push notifications, if it is not connected and its
     * settings allow it. Android does not let GmsCore connect in the background while it is not
     * exempt from battery optimization. GmsCore has one connection for all apps, and once Android
     * stops its push service, it does not reconnect on its own until the phone restarts.
     */
    static void connectGmsCore(Context context) {
        try {
            context.sendBroadcast(new Intent(GMS_CORE_CONNECT_ACTION)
                    .setClassName(GMS_CORE_PACKAGE_NAME, GMS_CORE_PUSH_TRIGGER_RECEIVER)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
        } catch (Exception ex) {
            Log.e(TAG, "connectGmsCore failure", ex);
        }
    }

    /**
     * Schedules {@link GmsCoreConnectionJob}, which keeps GmsCore connected for the app, so its
     * notifications do not depend on the app or another patched app being opened.
     * The job is kept when the phone restarts.
     */
    private static void scheduleConnectionJob(Context context) {
        try {
            JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
            if (jobScheduler.getPendingJob(CONNECTION_JOB_ID) != null) return;

            jobScheduler.schedule(new JobInfo.Builder(CONNECTION_JOB_ID,
                    new ComponentName(context, GmsCoreConnectionJob.class))
                    .setPeriodic(CONNECTION_JOB_INTERVAL_MS)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build());
        } catch (Exception ex) {
            Log.e(TAG, "scheduleConnectionJob failure", ex);
        }
    }

    /**
     * Asks to exempt GmsCore from battery optimization, and connects it once it is.
     */
    private static void requestBatteryOptimizationExemption(Activity activity) {
        open(activity, new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.fromParts("package", GMS_CORE_PACKAGE_NAME, null)));

        Context context = activity.getApplicationContext();
        new Thread(() -> {
            try {
                PowerManager powerManager = context.getSystemService(PowerManager.class);
                for (int second = 0; second < 120; second++) {
                    if (powerManager.isIgnoringBatteryOptimizations(GMS_CORE_PACKAGE_NAME)) {
                        connectGmsCore(context);
                        return;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                Log.e(TAG, "requestBatteryOptimizationExemption failure", ex);
            }
        }).start();
    }

    private enum Problem {
        NOT_INSTALLED,
        NO_LOCATION_ACCESS,
        BATTERY_OPTIMIZED,
        NOT_RUNNING,
    }

    private static Problem findProblem(Context context, boolean usesLocation) {
        try {
            context.getPackageManager().getPackageInfo(GMS_CORE_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.i(TAG, "GmsCore is not installed");
            return Problem.NOT_INSTALLED;
        }

        SharedPreferences preferences = getPreferences(context);

        // GmsCore gets the location for an app only with all of these permissions. It asks for the missing
        // ones itself from the background, where Android may not show the request, and waits for the answer.
        if (usesLocation && !preferences.getBoolean(LOCATION_CHECK_DISABLED_KEY, false)
                && !hasLocationAccess(context)) {
            Log.i(TAG, "GmsCore can not access the location");
            return Problem.NO_LOCATION_ACCESS;
        }

        if (preferences.getBoolean(BACKGROUND_CHECK_DISABLED_KEY, false)) {
            return null;
        }

        // Unlike Google Play services, GmsCore is not exempt from battery optimization by default,
        // so Android does not let it stay connected for push notifications in the background.
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        if (!powerManager.isIgnoringBatteryOptimizations(GMS_CORE_PACKAGE_NAME)) {
            Log.i(TAG, "GmsCore is not exempt from battery optimization");
            return Problem.BATTERY_OPTIMIZED;
        }

        // Some phones also do not let other apps start an app, until it is allowed in their own settings.
        ContentProviderClient client = context.getContentResolver()
                .acquireContentProviderClient(GMS_CORE_PROVIDER_AUTHORITY);
        if (client == null) {
            Log.i(TAG, "GmsCore is not running");
            return Problem.NOT_RUNNING;
        }
        client.close();
        return null;
    }

    private static boolean hasLocationAccess(Context context) {
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION}
                : new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        PackageManager packageManager = context.getPackageManager();
        for (String permission : permissions) {
            if (packageManager.checkPermission(permission, GMS_CORE_PACKAGE_NAME) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static void showDialog(Activity activity, Problem problem, String appName, String appNamePolish) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // The dialog follows the language of the device.
        final boolean polish = "pl".equals(Locale.getDefault().getLanguage());

        // Use the dialog style of the device, which looks right with any theme of the activity.
        final boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, night
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setNegativeButton(polish ? "Nie teraz" : "Not now", null);

        DialogInterface.OnClickListener doNotAskAgain = (dialogInterface, which) ->
                getPreferences(activity).edit().putBoolean(BACKGROUND_CHECK_DISABLED_KEY, true).apply();

        switch (problem) {
            case NOT_INSTALLED:
                builder.setTitle(polish ? "Brak MicroG" : "MicroG is not installed")
                        .setMessage(polish
                                ? "Logowanie przez Google i powiadomienia w tej zmodyfikowanej wersji "
                                + appNamePolish + " działają przez MicroG-RE, które nie jest zainstalowane."
                                : "Signing in with Google and notifications in this patched "
                                + appName + " work through MicroG-RE, which is not installed.")
                        .setPositiveButton(polish ? "Pobierz" : "Download", (dialogInterface, which) ->
                                open(activity, new Intent(Intent.ACTION_VIEW, Uri.parse(GMS_CORE_DOWNLOAD_URL))));
                break;
            case NO_LOCATION_ACCESS:
                builder.setTitle(polish ? "MicroG nie ma dostępu do lokalizacji" : "MicroG can't access the location")
                        .setMessage(polish
                                ? "Lokalizację w tej zmodyfikowanej wersji " + appNamePolish
                                + " ustala MicroG-RE, które nie ma do niej dostępu. W uprawnieniach MicroG-RE "
                                + "zezwól na dostęp do lokalizacji zawsze i włącz dokładną lokalizację."
                                : "The location in this patched " + appName + " comes from MicroG-RE, "
                                + "which can't access it. In the permissions of MicroG-RE, allow access to "
                                + "the location all the time and turn on precise location.")
                        .setPositiveButton(polish ? "Otwórz ustawienia" : "Open settings", (dialogInterface, which) ->
                                openGmsCoreSettings(activity))
                        .setNeutralButton(polish ? "Nie pytaj ponownie" : "Don't ask again", (dialogInterface, which) ->
                                getPreferences(activity).edit().putBoolean(LOCATION_CHECK_DISABLED_KEY, true).apply());
                break;
            case BATTERY_OPTIMIZED:
                builder.setTitle(polish ? "MicroG nie działa w tle" : "MicroG can't run in the background")
                        .setMessage(polish
                                ? "Powiadomienia w tej zmodyfikowanej wersji " + appNamePolish
                                + " przychodzą przez MicroG-RE. Android zatrzymuje je w tle, żeby oszczędzać "
                                + "baterię, więc powiadomienia przychodzą z opóźnieniem albo wcale. "
                                + "Pozwolić MicroG-RE działać w tle?"
                                : "Notifications in this patched " + appName + " come through MicroG-RE. "
                                + "Android stops it in the background to save battery, so notifications "
                                + "come late or not at all. Allow MicroG-RE to run in the background?")
                        .setPositiveButton(polish ? "Pozwól" : "Allow", (dialogInterface, which) ->
                                requestBatteryOptimizationExemption(activity))
                        .setNeutralButton(polish ? "Nie pytaj ponownie" : "Don't ask again", doNotAskAgain);
                break;
            case NOT_RUNNING:
                builder.setTitle(polish ? "MicroG nie działa" : "MicroG is not running")
                        .setMessage(polish
                                ? "Powiadomienia w tej zmodyfikowanej wersji " + appNamePolish
                                + " przychodzą przez MicroG-RE, którego telefon nie pozwala uruchomić. "
                                + "W ustawieniach baterii lub uruchamiania aplikacji pozwól MicroG-RE "
                                + "uruchamiać się automatycznie, przez inne aplikacje i działać w tle."
                                : "Notifications in this patched " + appName + " come through MicroG-RE, "
                                + "which the phone does not let start. In the settings of the battery or of "
                                + "app launch, allow MicroG-RE to start on its own, to be started by other apps "
                                + "and to run in the background.")
                        .setPositiveButton(polish ? "Otwórz ustawienia" : "Open settings", (dialogInterface, which) ->
                                openGmsCoreSettings(activity))
                        .setNeutralButton(polish ? "Nie pytaj ponownie" : "Don't ask again", doNotAskAgain);
                break;
        }

        builder.show();
    }

    private static void openGmsCoreSettings(Activity activity) {
        open(activity, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", GMS_CORE_PACKAGE_NAME, null)));
    }

    private static void open(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "Could not open " + intent, ex);
        }
    }

    /**
     * Deletes the push registration of Firebase once, so the app registers again through GmsCore.
     * Must be called before Firebase initializes.
     *
     * @return If the registration was deleted now.
     */
    public static boolean resetPushRegistration(Context context) {
        try {
            SharedPreferences preferences = getPreferences(context);
            if (preferences.getBoolean(PUSH_REGISTRATION_RESET_KEY, false)) return false;

            // Older versions of Firebase keep the installation in the files directory,
            // newer ones move it to the directory that is not backed up.
            for (File directory : new File[]{context.getNoBackupFilesDir(), context.getFilesDir()}) {
                File[] installations = directory.listFiles(
                        (parent, name) -> name.startsWith(FIREBASE_INSTALLATION_FILE_PREFIX));
                if (installations == null) continue;

                for (File installation : installations) {
                    if (!installation.delete()) {
                        Log.w(TAG, "Could not delete " + installation);
                    }
                }
            }

            context.getSharedPreferences(FIREBASE_MESSAGING_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit().clear().commit();

            preferences.edit().putBoolean(PUSH_REGISTRATION_RESET_KEY, true).apply();
            Log.i(TAG, "Reset the push registration");
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "resetPushRegistration failure", ex);
            return false;
        }
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
