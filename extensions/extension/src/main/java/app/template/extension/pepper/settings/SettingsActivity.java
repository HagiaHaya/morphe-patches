package app.template.extension.pepper.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LocaleManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
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

import java.util.Locale;

/**
 * Settings of the patches, opened from the settings of Pepper.
 * Built from framework views, because the AndroidX classes of the app are obfuscated.
 */
@SuppressWarnings("unused")
public final class SettingsActivity extends Activity {

    private static final String TAG = "MorphePepper";

    private static final String TITLE = "HagiaHaya Patches";

    /**
     * Language tags of the app languages offered, besides the system default.
     */
    private static final String[] LANGUAGE_TAGS = {"pl", "en"};

    private static final String[] LANGUAGE_NAMES = {"Polski", "English"};

    private boolean polish;

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? android.R.style.Theme_DeviceDefault_DayNight
                : android.R.style.Theme_DeviceDefault_Light);
        super.onCreate(savedInstanceState);

        // The default locale follows the language chosen for the app.
        polish = "pl".equals(Locale.getDefault().getLanguage());

        setTitle(TITLE);
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

        addFeatures();
        addLanguage();
        addRestart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addFeatures() {
        addHeader(text("Funkcje", "Features"));

        if (PatchedFeatures.removeAds()) {
            addSwitch(Settings.REMOVE_ADS,
                    text("Usuń reklamy", "Remove ads"),
                    text("Bez reklam między okazjami i na stronach okazji.",
                            "No ads between deals and on the deal pages."));
        }
        if (PatchedFeatures.sanitizeDealLinks()) {
            addSwitch(Settings.SANITIZE_DEAL_LINKS,
                    text("Bezpośrednie linki do sklepów", "Sanitize deal links"),
                    text("Otwiera stronę sklepu bez przekierowań śledzących i linków afiliacyjnych.",
                            "Opens the store page without tracking redirects and affiliate links."));
        }
        if (PatchedFeatures.sanitizeSharingLinks()) {
            addSwitch(Settings.SANITIZE_SHARING_LINKS,
                    text("Czyste linki do udostępniania", "Sanitize sharing links"),
                    text("Udostępnia bezpośredni link do okazji, a nie link śledzący.",
                            "Shares the direct link to a deal instead of a tracking link."));
        }
        if (PatchedFeatures.unlockCustomAppIcons()) {
            addSwitch(Settings.UNLOCK_CUSTOM_APP_ICONS,
                    text("Odblokuj ikony aplikacji", "Unlock custom app icons"),
                    text("Wszystkie ikony, także sezonowe, niezależnie od poziomu w Pepper Club.",
                            "All icons, including the seasonal ones, regardless of the Pepper Club level."));
        }
        if (PatchedFeatures.spoofContributorLevel()) {
            addContributorLevel();
        }
    }

    private void addContributorLevel() {
        String[] levels = {
                text("Zdobyty", "Earned"),
                text("Srebro", "Silver"),
                text("Złoto", "Gold"),
                text("Platyna", "Platinum"),
        };
        String description = text(
                "Zmienia tylko poziom wyświetlany w aplikacji. Nagrody przyznaje Pepper.",
                "Only changes the level the app shows. Pepper still grants the rewards of the level earned.");

        TextView[] summary = new TextView[1];
        View row = addRow(text("Poziom w Pepper Club", "Pepper Club level"),
                levels[Settings.getContributorLevel()] + "\n" + description, null, view ->
                        new AlertDialog.Builder(this)
                                .setTitle(text("Poziom w Pepper Club", "Pepper Club level"))
                                .setSingleChoiceItems(levels, Settings.getContributorLevel(), (dialog, which) -> {
                                    Settings.setContributorLevel(which);
                                    summary[0].setText(levels[which] + "\n" + description);
                                    dialog.dismiss();
                                })
                                .show());
        summary[0] = row.findViewWithTag("summary");
    }

    /**
     * Pepper declares its languages, so Android 13 and newer can switch the language of the app alone.
     */
    private void addLanguage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        addHeader(text("Język", "Language"));

        LocaleManager localeManager = getSystemService(LocaleManager.class);
        String[] choices = {text("Domyślny systemu", "System default"), LANGUAGE_NAMES[0], LANGUAGE_NAMES[1]};

        LocaleList appLocales = localeManager.getApplicationLocales();
        int selected = 0;
        if (!appLocales.isEmpty()) {
            String language = appLocales.get(0).getLanguage();
            for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
                if (LANGUAGE_TAGS[i].equals(language)) {
                    selected = i + 1;
                }
            }
        }

        String summary = choices[selected];
        if (!hasPolishTranslation()) {
            summary += "\n" + text("Ta wersja aplikacji nie zawiera polskiego tłumaczenia.",
                    "This build of the app has no Polish translation.");
        }

        int current = selected;
        addRow(text("Język aplikacji", "App language"), summary, null, view ->
                new AlertDialog.Builder(this)
                        .setTitle(text("Język aplikacji", "App language"))
                        .setSingleChoiceItems(choices, current, (dialog, which) -> {
                            dialog.dismiss();
                            // Android recreates the activities of the app in the new language.
                            localeManager.setApplicationLocales(which == 0
                                    ? LocaleList.getEmptyLocaleList()
                                    : LocaleList.forLanguageTags(LANGUAGE_TAGS[which - 1]));
                        })
                        .show());
    }

    /**
     * @return If the app has Polish strings, which are in a split APK that may be missing.
     */
    private boolean hasPolishTranslation() {
        try {
            int stringId = getResources().getIdentifier("custom_app_icon", "string", getPackageName());
            if (stringId == 0) {
                return true;
            }

            Configuration configuration = new Configuration(getResources().getConfiguration());
            configuration.setLocale(Locale.forLanguageTag("pl"));
            String polishText = createConfigurationContext(configuration).getString(stringId);

            configuration.setLocale(Locale.ROOT);
            String defaultText = createConfigurationContext(configuration).getString(stringId);

            return !polishText.equals(defaultText);
        } catch (Exception ex) {
            Log.e(TAG, "hasPolishTranslation failure", ex);
            return true;
        }
    }

    private void addRestart() {
        addHeader("");
        addRow(text("Uruchom ponownie", "Restart Pepper"),
                text("Zmiany funkcji zaczną działać po ponownym uruchomieniu aplikacji.",
                        "Changes to the features take effect after restarting the app."),
                null, view -> restart());
    }

    private void restart() {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent != null && launchIntent.getComponent() != null) {
                startActivity(Intent.makeRestartActivityTask(launchIntent.getComponent()));
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to restart", ex);
        }
        System.exit(0);
    }

    private void addSwitch(String key, String title, String summary) {
        Switch toggle = new Switch(this);
        toggle.setChecked(Settings.isEnabled(key));
        toggle.setOnCheckedChangeListener((button, checked) -> Settings.setEnabled(key, checked));
        // The row toggles the switch, which would otherwise take the click.
        toggle.setClickable(false);
        toggle.setFocusable(false);

        addRow(title, summary, toggle, view -> toggle.toggle());
    }

    private View addRow(String title, String summary, View widget, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        row.setBackgroundResource(attribute(android.R.attr.selectableItemBackground));
        row.setOnClickListener(onClick);

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

    private String text(String polishText, String englishText) {
        return polish ? polishText : englishText;
    }
}
