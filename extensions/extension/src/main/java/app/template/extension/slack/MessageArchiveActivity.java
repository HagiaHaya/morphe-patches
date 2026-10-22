package app.template.extension.slack;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings and maintenance of the message archive, opened from the shortcuts of the app icon.
 * Built from framework views, as the Compose UI of Slack can not be reused here.
 */
@SuppressWarnings("unused")
public final class MessageArchiveActivity extends Activity {

    private static final String TAG = "MorpheSlack";

    private static final int EXPORT_REQUEST_CODE = 1;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LinearLayout list;

    private TextView statsSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? android.R.style.Theme_DeviceDefault_DayNight
                : android.R.style.Theme_DeviceDefault_Light);
        super.onCreate(savedInstanceState);

        setTitle("Message archive");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list);
        scrollView.setClipToPadding(false);
        // The app is drawn edge to edge, so the content starts behind the status bar
        // and the action bar, which the top inset includes. Keep it clear of them
        // and of the navigation bar.
        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });
        setContentView(scrollView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Dark system bar icons on the light theme.
            boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(night ? 0 : lightBars, lightBars);
        }

        addHeader("Message history");
        addSwitch();

        addHeader("Archive");
        statsSummary = addRow("Archived messages", "Counting…", null, null)
                .findViewWithTag("summary");
        addRow("Search",
                "Searches the archived messages, including the ones the server hides.",
                null, view -> startActivity(new Intent(this, MessageArchiveSearchActivity.class)));
        addRow("Export",
                "Saves the archived messages as a JSON file, listed by conversation ID.",
                null, view -> export());
        addRow("Delete archive",
                "Deletes the archived messages from this device. Slack keeps showing the messages "
                        + "the server returns.",
                null, view -> confirmDelete());

        addNote("Only the messages Slack loads are archived, from the moment the patched app runs. "
                + "On a free workspace, open a conversation at least once every 90 days "
                + "to archive its messages before the server hides them.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addSwitch() {
        Switch toggle = new Switch(this);
        toggle.setChecked(KeepMessageHistoryPatch.isEnabled());
        toggle.setOnCheckedChangeListener((button, checked) -> KeepMessageHistoryPatch.setEnabled(checked));
        // The row toggles the switch, which would otherwise take the click.
        toggle.setClickable(false);
        toggle.setFocusable(false);

        addRow("Keep message history",
                "Keeps a copy of the messages Slack loads, and shows them where the server "
                        + "hides older messages, such as after 90 days on the free plan.",
                toggle, view -> toggle.toggle());
    }

    private void updateStats() {
        MessageArchive archive = MessageArchive.get();
        if (archive == null) {
            statsSummary.setText("The archive is not available.");
            return;
        }
        executor.execute(() -> {
            String summary;
            try {
                MessageArchive.Stats stats = archive.stats();
                if (stats.messages == 0) {
                    summary = "No messages yet.";
                } else {
                    summary = stats.messages + (stats.messages == 1 ? " message" : " messages")
                            + " in " + stats.conversations
                            + (stats.conversations == 1 ? " conversation" : " conversations")
                            + ", since " + formatTs(stats.oldestTs) + ".";
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to count the archived messages", ex);
                summary = "Failed to read the archive.";
            }
            String text = summary;
            mainHandler.post(() -> statsSummary.setText(text));
        });
    }

    private static String formatTs(String ts) {
        try {
            long millis = (long) (Double.parseDouble(ts) * 1000);
            return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(millis));
        } catch (Exception ex) {
            return ts;
        }
    }

    private void export() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "slack-message-archive-" + date + ".json");
        try {
            startActivityForResult(intent, EXPORT_REQUEST_CODE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to choose an export file", ex);
            Toast.makeText(this, "No app to save files with.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != EXPORT_REQUEST_CODE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        MessageArchive archive = MessageArchive.get();
        if (archive == null) {
            return;
        }
        executor.execute(() -> {
            String message;
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) {
                    throw new IllegalStateException("No output stream for " + uri);
                }
                archive.export(output);
                message = "Exported the archive.";
            } catch (Exception ex) {
                Log.e(TAG, "Failed to export the archive", ex);
                message = "Failed to export the archive.";
            }
            String text = message;
            mainHandler.post(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete archive?")
                .setMessage("The archived messages are deleted from this device. "
                        + "Messages the server no longer returns can not be restored.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    MessageArchive archive = MessageArchive.get();
                    if (archive != null) {
                        archive.clear();
                    }
                    // The deletion is queued behind the pending writes.
                    mainHandler.postDelayed(this::updateStats, 500);
                })
                .show();
    }

    private View addRow(String title, String summary, View widget, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        if (onClick != null) {
            row.setBackgroundResource(attribute(android.R.attr.selectableItemBackground));
            row.setOnClickListener(onClick);
        }

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView titleView = new TextView(this);
        titleView.setTextAppearance(attribute(android.R.attr.textAppearanceListItem));
        titleView.setText(title);
        texts.addView(titleView);

        TextView summaryView = new TextView(this);
        summaryView.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
        summaryView.setTextColor(color(android.R.attr.textColorSecondary));
        summaryView.setText(summary);
        summaryView.setTag("summary");
        texts.addView(summaryView);

        if (widget != null) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(dp(16));
            row.addView(widget, params);
        }

        list.addView(row);
        return row;
    }

    private void addHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSmall));
        header.setTextColor(color(android.R.attr.colorAccent));
        header.setPadding(dp(20), dp(20), dp(20), dp(4));
        list.addView(header);
    }

    private void addNote(String text) {
        TextView note = new TextView(this);
        note.setText(text);
        note.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
        note.setTextColor(color(android.R.attr.textColorSecondary));
        note.setPadding(dp(20), dp(16), dp(20), dp(16));
        list.addView(note);
    }

    private int attribute(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId;
    }

    private ColorStateList color(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId != 0
                ? getColorStateList(value.resourceId)
                : ColorStateList.valueOf(value.data);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
