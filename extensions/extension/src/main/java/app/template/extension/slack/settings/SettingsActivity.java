package app.template.extension.slack.settings;

import android.os.Bundle;

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
        addComposer();
        addPeople();
        addAdvanced();
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
        if (PatchedFeatures.sanitizeLinks()) {
            addSwitch(Settings.SANITIZE_LINKS, "Sanitize links",
                    "Removes tracking parameters, such as utm_source or fbclid, from links in the messages you send.");
        }
        if (PatchedFeatures.anonymizeFileNames()) {
            addSwitch(Settings.ANONYMIZE_FILE_NAMES, "Anonymize file names",
                    "Uploads files under a random name, keeping the extension.");
        }
    }

    private void addMessages() {
        startSection("Messages");
        if (PatchedFeatures.messageLogger()) {
            addSwitch(Settings.KEEP_DELETED_MESSAGES, "Keep deleted messages",
                    "Messages others delete stay in the conversation, marked as deleted.");
            addSwitch(Settings.KEEP_EDIT_HISTORY, "Keep edit history",
                    "Messages others edit show their earlier versions below them.");
            addLink("Message log", "Deleted messages and earlier versions of edited messages.",
                    MessageLogActivity.class);
        }
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

    private void addComposer() {
        startSection("Message composer");
        if (PatchedFeatures.standardMarkdown()) {
            addSwitch(Settings.STANDARD_MARKDOWN, "Standard Markdown",
                    "Formats **bold**, *italic*, ~~strikethrough~~, `code` and [text](link) as you type, "
                            + "instead of Slack's *bold* and _italic_.");
        }
    }

    private void addPeople() {
        startSection("People");
        if (PatchedFeatures.nicknames()) {
            addLink("Nicknames", "Give members nicknames that only you see.", NicknamesActivity.class);
        }
    }

    private void addAdvanced() {
        startSection("Advanced");
        if (PatchedFeatures.featureFlags()) {
            addLink("Feature flags", "Turn features and experiments of Slack on or off.", FeatureFlagsActivity.class);
        }
    }

    private void addRestart() {
        startSection("");
        addRow("Restart Slack", "Most changes take effect after restarting the app.", null, view -> restart());
    }
}
