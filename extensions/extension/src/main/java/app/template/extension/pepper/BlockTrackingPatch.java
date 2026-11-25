package app.template.extension.pepper;

import android.util.Log;

import app.template.extension.pepper.settings.Settings;

import java.util.UUID;

/**
 * Stops the analytics of Pepper itself, and can send a random device ID instead of one derived
 * from the Android ID.
 */
@SuppressWarnings("unused")
public final class BlockTrackingPatch {

    private static final String TAG = "MorphePepper";

    /**
     * Parts of the addresses of the analytics of Pepper: reports of visited, shared and opened deals,
     * and the Ocular event tracker.
     */
    private static final String[] BLOCKED_URLS = {
            "/rest_api/v2/analytics-event-report",
            "://ocular.",
    };

    private static final String DEVICE_ID = "device_id";

    /**
     * Injection point. Called when a call of OkHttp starts. Cancels calls to the analytics of Pepper,
     * which then fail before connecting. The app sends them without waiting for an answer.
     */
    public static void onCallStarted(Object call, Object request) {
        try {
            // Request{method=POST, url=https://www.pepper.pl/rest_api/v2/analytics-event-report?..., ...}
            String description = String.valueOf(request);
            int urlStart = description.indexOf("url=");
            if (urlStart < 0) {
                return;
            }
            int urlEnd = description.indexOf(", ", urlStart);
            String url = description.substring(urlStart, urlEnd < 0 ? description.length() : urlEnd);
            for (String blocked : BLOCKED_URLS) {
                if (url.contains(blocked)) {
                    call.getClass().getMethod("cancel").invoke(call);
                    return;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "onCallStarted failure", ex);
        }
    }

    /**
     * Injection point. Called instead of deriving the device ID, which the app sends with each
     * request and registers the device with, from the Android ID.
     *
     * @return A random device ID, kept until the app data is cleared, or null for the original one.
     */
    public static synchronized String getDeviceId() {
        try {
            if (!Settings.getBoolean(Settings.RANDOM_DEVICE_ID, false)) {
                return null;
            }
            String deviceId = Settings.getString(DEVICE_ID);
            if (deviceId.isEmpty()) {
                // The same format as the original, an MD5 in lowercase hexadecimal.
                deviceId = UUID.randomUUID().toString().replace("-", "");
                Settings.setString(DEVICE_ID, deviceId);
            }
            return deviceId;
        } catch (Exception ex) {
            Log.e(TAG, "getDeviceId failure", ex);
            return null;
        }
    }
}
