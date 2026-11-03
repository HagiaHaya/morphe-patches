package app.template.extension.pepper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Wakes the app up to check for price errors, as Android freezes it in the background.
 * Added to the manifest by the patch.
 */
public final class PriceErrorAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync();
        PriceErrorAlertsPatch.onAlarm(result::finish);
    }
}
