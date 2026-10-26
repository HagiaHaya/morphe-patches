package app.template.extension.shared;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.Locale;

/**
 * Asks to install MicroG-RE, which the GmsCore support patches make apps use
 * instead of Google Play services.
 */
public final class GmsCoreSupport {

    private static final String TAG = "MorpheGmsCore";

    private static final String GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms";

    private static final String GMS_CORE_DOWNLOAD_URL =
            "https://github.com/MorpheApp/MicroG-RE/releases/latest";

    private static final String PREFERENCES_NAME = "hagiahaya_gms_core";

    /**
     * If the push registration from before the app used GmsCore was deleted.
     */
    private static final String PUSH_REGISTRATION_RESET_KEY = "push_registration_reset";

    /**
     * Prefix of the files in which Firebase keeps the installation, the ID the push token is issued for.
     */
    private static final String FIREBASE_INSTALLATION_FILE_PREFIX = "PersistedInstallation.";

    /**
     * Preferences in which Firebase caches the push token.
     */
    private static final String FIREBASE_MESSAGING_PREFERENCES_NAME = "com.google.android.gms.appid";

    /**
     * If GmsCore was already checked for, so the dialog is shown once per app launch.
     */
    private static boolean checked;

    private GmsCoreSupport() {
    }

    /**
     * Shows a dialog that asks to install GmsCore, if it is missing.
     *
     * @param appName       Name of the app in English, such as "Wykop".
     * @param appNamePolish Name of the app in the genitive case in Polish, such as "Wykopu".
     */
    public static void checkGmsCore(Activity activity, String appName, String appNamePolish) {
        if (checked) return;
        checked = true;

        try {
            activity.getPackageManager().getPackageInfo(GMS_CORE_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.i(TAG, "GmsCore is not installed");
            // Show the dialog once the activity is created.
            new Handler(Looper.getMainLooper()).post(
                    () -> showNotInstalledDialog(activity, appName, appNamePolish));
        } catch (Exception ex) {
            Log.e(TAG, "checkGmsCore failure", ex);
        }
    }

    private static void showNotInstalledDialog(Activity activity, String appName, String appNamePolish) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // The dialog follows the language of the device.
        final boolean polish = "pl".equals(Locale.getDefault().getLanguage());

        // Use the dialog style of the device, which looks right with any theme of the activity.
        final boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        new AlertDialog.Builder(activity, night
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(polish ? "Brak MicroG" : "MicroG is not installed")
                .setMessage(polish
                        ? "Logowanie przez Google i powiadomienia w tej zmodyfikowanej wersji "
                        + appNamePolish + " działają przez MicroG-RE, które nie jest zainstalowane."
                        : "Signing in with Google and notifications in this patched "
                        + appName + " work through MicroG-RE, which is not installed.")
                .setPositiveButton(polish ? "Pobierz" : "Download", (dialog, which) -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GMS_CORE_DOWNLOAD_URL)));
                    } catch (Exception ex) {
                        Log.e(TAG, "Could not open the download page", ex);
                    }
                })
                .setNegativeButton(polish ? "Nie teraz" : "Not now", null)
                .show();
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
