package app.template.extension.slack.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import app.template.extension.slack.MessageLog;
import app.template.extension.slack.SlackNames;

/**
 * Lists the messages others deleted and the earlier versions of edited messages,
 * and opens them in Slack.
 */
@SuppressWarnings("unused")
public final class MessageLogActivity extends SettingsScreen {

    private static final int LIMIT = 200;

    /**
     * Longest text shown of a message.
     */
    private static final int MAX_TEXT_LENGTH = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Message log");

        MessageLog.initialize(this);
        MessageLog log = MessageLog.get();
        SlackNames names = SlackNames.load(this);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        addRow("Clear the log", "Forgets the deleted messages and the earlier versions. "
                + "Deleted messages Slack has cached stay until it reloads their conversation.", null, view ->
                new AlertDialog.Builder(this)
                        .setTitle("Clear the log?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            log.clear();
                            recreate();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());

        startSection("Deleted messages");
        List<MessageLog.Deleted> deleted = log.recentDeleted(LIMIT);
        if (deleted.isEmpty()) {
            addNote("No deleted messages yet.");
        }
        for (MessageLog.Deleted message : deleted) {
            String title = names.userName(message.userId) + " in " + names.conversationName(message.channelId);
            addRow(title, shorten(message.text) + "\nDeleted on " + dateFormat.format(new Date(message.deletedAt)),
                    null, view -> open(names, message.channelId, message.ts, message.threadTs));
        }

        startSection("Earlier versions");
        List<MessageLog.Edit> edits = log.recentEdits(LIMIT);
        if (edits.isEmpty()) {
            addNote("No edited messages yet.");
        }
        for (MessageLog.Edit edit : edits) {
            String title = names.userName(edit.userId) + " in " + names.conversationName(edit.channelId);
            addRow(title, shorten(edit.previousText) + "\nEdited on " + dateFormat.format(new Date(edit.editedAt)),
                    null, view -> open(names, edit.channelId, edit.ts, edit.threadTs));
        }
    }

    private static String shorten(String text) {
        if (text == null || text.isEmpty()) {
            return "(no text)";
        }
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "…" : text;
    }

    private void open(SlackNames names, String channelId, String ts, String threadTs) {
        Uri.Builder link = new Uri.Builder().scheme("slack").authority("channel");
        String teamId = names.teamIdOf(channelId);
        if (teamId != null) {
            link.appendQueryParameter("team", teamId);
        }
        link.appendQueryParameter("id", channelId).appendQueryParameter("ts", ts);
        if (threadTs != null && !threadTs.equals(ts)) {
            link.appendQueryParameter("thread_ts", threadTs);
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, link.build()).setPackage(getPackageName()));
        } catch (Exception ex) {
            Log.e(Settings.TAG, "Failed to open a logged message", ex);
            Toast.makeText(this, "Failed to open the message.", Toast.LENGTH_LONG).show();
        }
    }
}
