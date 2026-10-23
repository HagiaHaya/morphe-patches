package app.template.extension.slack.settings;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
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

/**
 * A screen of the settings of the patches, a list of rows built from framework views,
 * as the AndroidX classes of Slack are shrunk.
 */
// Slack requires Android 12L.
@TargetApi(Build.VERSION_CODES.S_V2)
abstract class SettingsScreen extends Activity {

    LinearLayout list;

    ScrollView scrollView;

    /**
     * Title of the section whose header is added before its first row, so sections
     * without rows have no header.
     */
    private String pendingSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        scrollView = new ScrollView(this);
        scrollView.addView(list);
        scrollView.setClipToPadding(false);
        // Slack is drawn edge to edge, so the content starts behind the status bar
        // and the action bar, which the top inset includes. Keep it clear of them
        // and of the navigation bar.
        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        setContentView(scrollView);

        // Dark system bar icons on the light theme.
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getInsetsController().setSystemBarsAppearance(night ? 0 : lightBars, lightBars);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    Switch addSwitch(String key, String title, String summary) {
        Switch toggle = new Switch(this);
        toggle.setChecked(Settings.isEnabled(key));
        toggle.setOnCheckedChangeListener((button, checked) -> Settings.setEnabled(key, checked));
        // The row toggles the switch, which would otherwise take the click.
        toggle.setClickable(false);
        toggle.setFocusable(false);

        addRow(title, summary, toggle, view -> toggle.toggle());
        return toggle;
    }

    View addRow(String title, String summary, View widget, View.OnClickListener onClick) {
        addPendingSection();

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
        titleView.setTag("title");
        texts.addView(titleView);

        TextView summaryView = new TextView(this);
        summaryView.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
        summaryView.setTextColor(color(android.R.attr.textColorSecondary));
        summaryView.setText(summary);
        summaryView.setTag("summary");
        summaryView.setVisibility(summary == null || summary.isEmpty() ? View.GONE : View.VISIBLE);
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

    /**
     * Adds a row that opens another screen.
     */
    View addLink(String title, String summary, Class<? extends Activity> screen) {
        return addRow(title, summary, null, view -> {
            try {
                startActivity(new Intent(this, screen));
            } catch (Exception ex) {
                Log.e(Settings.TAG, "Failed to open " + screen.getName(), ex);
            }
        });
    }

    /**
     * Starts a section, whose header is added with its first row.
     */
    void startSection(String title) {
        pendingSection = title;
    }

    private void addPendingSection() {
        if (pendingSection != null) {
            String title = pendingSection;
            pendingSection = null;
            addHeader(title);
        }
    }

    void addHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSmall));
        header.setTextColor(color(android.R.attr.colorAccent));
        header.setPadding(dp(20), dp(20), dp(20), dp(4));
        list.addView(header);
    }

    void addNote(String text) {
        addPendingSection();

        TextView note = new TextView(this);
        note.setText(text);
        note.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
        note.setTextColor(color(android.R.attr.textColorSecondary));
        note.setPadding(dp(20), dp(8), dp(20), dp(8));
        list.addView(note);
    }

    static void setSummary(View row, String summary) {
        TextView summaryView = row.findViewWithTag("summary");
        summaryView.setText(summary);
        summaryView.setVisibility(summary == null || summary.isEmpty() ? View.GONE : View.VISIBLE);
    }

    int attribute(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId;
    }

    ColorStateList color(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId != 0
                ? getColorStateList(value.resourceId)
                : ColorStateList.valueOf(value.data);
    }

    int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
