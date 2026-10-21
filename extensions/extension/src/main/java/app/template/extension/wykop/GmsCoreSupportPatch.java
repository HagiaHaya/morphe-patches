package app.template.extension.wykop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Locale;

@SuppressWarnings("unused")
public final class GmsCoreSupportPatch {

    private static final String TAG = "MorpheWykop";

    private static final String GMS_CORE_PACKAGE_NAME = "app.revanced.android.gms";

    private static final String GMS_CORE_DOWNLOAD_URL =
            "https://github.com/MorpheApp/MicroG-RE/releases/latest";

    /**
     * If GmsCore was already checked for, so the dialog is shown once per app launch.
     */
    private static boolean checked;

    /**
     * Injection point. Called when the main activity is created.
     */
    public static void checkGmsCore(Activity activity) {
        if (checked) return;
        checked = true;

        try {
            activity.getPackageManager().getPackageInfo(GMS_CORE_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.i(TAG, "GmsCore is not installed");
            // Show the dialog once the activity is created.
            new Handler(Looper.getMainLooper()).post(() -> showNotInstalledDialog(activity));
        } catch (Exception ex) {
            Log.e(TAG, "checkGmsCore failure", ex);
        }
    }

    private static void showNotInstalledDialog(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // The app is in Polish only, but the dialog follows the language of the device.
        final boolean polish = "pl".equals(Locale.getDefault().getLanguage());

        // The app uses an old theme for its activity, so use the dialog style of the device.
        final boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        new AlertDialog.Builder(activity, night
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(polish ? "Brak MicroG" : "MicroG is not installed")
                .setMessage(polish
                        ? "Logowanie przez Google i powiadomienia w tej zmodyfikowanej wersji"
                        + " Wykopu działają przez MicroG-RE, które nie jest zainstalowane."
                        : "Signing in with Google and notifications in this patched Wykop"
                        + " work through MicroG-RE, which is not installed.")
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
}
