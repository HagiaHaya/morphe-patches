package app.template.extension.slack.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import app.template.extension.slack.MessageArchiveActivity;
import app.template.extension.slack.MessageArchiveSearchActivity;

/**
 * Settings of the patches, opened from the preferences of Slack.
 */
@SuppressWarnings("unused")
public final class SettingsActivity extends SettingsScreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("HagiaHaya Patches");

        addPrivacy();
        addMessages();
        addRestart();
    }

    private void addPrivacy() {
        startSection("Privacy");
        if (PatchedFeatures.silentTyping()) {
            addSwitch(Settings.SILENT_TYPING, "Silent typing",
                    "Others don't see that you are typing. You still see when they are.");
        }
        if (PatchedFeatures.blockTelemetry()) {
            addSwitch(Settings.BLOCK_TELEMETRY, "Block telemetry",
                    "Doesn't send usage tracking, performance traces and crash reports to Slack.");
        }
    }

    private void addMessages() {
        startSection("Messages");
        if (PatchedFeatures.keepMessageHistory()) {
            addSwitch(Settings.KEEP_MESSAGE_HISTORY, "Keep message history",
                    "Keeps a copy of the messages Slack loads, and shows the copies where the server "
                            + "hides older messages.");
            addLink("Message archive", "Export or delete the copies of the messages.",
                    MessageArchiveActivity.class);
            addLink("Search archive", "Search the copies of the messages.", MessageArchiveSearchActivity.class);
        }
        if (PatchedFeatures.hideHistoryLimitBanner()) {
            addSwitch(Settings.HIDE_HISTORY_LIMIT_BANNER, "Hide history limit banner",
                    "Hides the banner about messages older than 90 days being hidden on the free plan.");
        }
    }

    private void addRestart() {
        startSection("");
        addRow("Restart Slack", "Most changes take effect after restarting the app.", null, view -> restart());
    }

    private void restart() {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent != null && launchIntent.getComponent() != null) {
                startActivity(Intent.makeRestartActivityTask(launchIntent.getComponent()));
            }
        } catch (Exception ex) {
            Log.e(Settings.TAG, "Failed to restart", ex);
        }
        System.exit(0);
    }
}
