package app.template.extension.slack.settings;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.template.extension.slack.FeatureFlagsPatch;

/**
 * Lists the feature flags of Slack and overrides them.
 */
@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.S_V2)
public final class FeatureFlagsActivity extends SettingsScreen {

    private final List<FeatureFlagsPatch.Flag> flags = new ArrayList<>();

    private final List<FeatureFlagsPatch.Flag> shown = new ArrayList<>();

    private FlagAdapter adapter;

    private View resetRow;

    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Feature flags");

        // The flags are many, so they are listed by a ListView below the settings
        // instead of in the scrolling list of the other screens.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        addSwitch(Settings.FEATURE_FLAGS, "Override feature flags",
                "Slack turns features and experiments on and off with these flags. Turning on a flag "
                        + "of a feature the server doesn't offer to the workspace can break the app. "
                        + "Restart Slack after a change.");
        resetRow = addRow("Reset all overrides", null, null, view -> new AlertDialog.Builder(this)
                .setTitle("Reset all overrides?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    FeatureFlagsPatch.clearOverrides();
                    updateResetRow();
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        updateResetRow();

        EditText search = new EditText(this);
        search.setHint("Search flags");
        search.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(dp(20), dp(8), dp(20), dp(8));
        root.addView(search, searchParams);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                query = text.toString().trim().toLowerCase(Locale.ROOT);
                filter();
            }
        });

        ListView listView = new ListView(this);
        adapter = new FlagAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> choose(shown.get(position)));
        root.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);

        flags.addAll(FeatureFlagsPatch.getFlags());
        if (flags.isEmpty()) {
            addNote("The flags are listed once Slack has loaded a workspace.");
        }
        filter();
    }

    private void updateResetRow() {
        int count = FeatureFlagsPatch.getOverrideCount();
        setSummary(resetRow, count == 1 ? "1 flag is overridden." : count + " flags are overridden.");
    }

    private void filter() {
        shown.clear();
        // Overridden flags first, so they are easy to find again.
        for (int pass = 0; pass < 2; pass++) {
            for (FeatureFlagsPatch.Flag flag : flags) {
                boolean overridden = FeatureFlagsPatch.getOverride(flag.key) != null;
                if (overridden == (pass == 0) && (query.isEmpty() || flag.key.contains(query))) {
                    shown.add(flag);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void choose(FeatureFlagsPatch.Flag flag) {
        Boolean override = FeatureFlagsPatch.getOverride(flag.key);
        String[] choices = {"Slack's value (" + (flag.value ? "on" : "off") + ")", "On", "Off"};
        int selected = override == null ? 0 : override ? 1 : 2;
        new AlertDialog.Builder(this)
                .setTitle(flag.key)
                .setSingleChoiceItems(choices, selected, (dialog, which) -> {
                    FeatureFlagsPatch.setOverride(flag.key, which == 0 ? null : which == 1);
                    updateResetRow();
                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                })
                .show();
    }

    private final class FlagAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = (LinearLayout) convertView;
            if (row == null) {
                row = new LinearLayout(FeatureFlagsActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(20), dp(10), dp(20), dp(10));

                TextView title = new TextView(FeatureFlagsActivity.this);
                title.setTextAppearance(attribute(android.R.attr.textAppearanceListItem));
                title.setTag("title");
                row.addView(title);

                TextView summary = new TextView(FeatureFlagsActivity.this);
                summary.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
                summary.setTextColor(color(android.R.attr.textColorSecondary));
                summary.setTag("summary");
                row.addView(summary);
            }

            FeatureFlagsPatch.Flag flag = shown.get(position);
            Boolean override = FeatureFlagsPatch.getOverride(flag.key);
            String value = flag.value ? "On" : "Off";
            ((TextView) row.findViewWithTag("title")).setText(flag.key);
            ((TextView) row.findViewWithTag("summary")).setText(override == null
                    ? value
                    : (override ? "Turned on" : "Turned off") + " (Slack: " + value.toLowerCase(Locale.ROOT) + ")");
            return row;
        }
    }
}
