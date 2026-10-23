package app.template.extension.slack.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.jvm.functions.Function1;
import slack.features.settings.circuit.PreferenceSettings$Event;
import slack.uikit.components.SKImageResource;
import slack.uikit.components.list.data.SKListAccessories;
import slack.uikit.components.list.viewmodels.SKListDividerPresentationObject;
import slack.uikit.components.list.viewmodels.SKListGenericPresentationObject;
import slack.uikit.components.list.viewmodels.SKListHeaderPresentationObject;
import slack.uikit.text.CharSequenceResource;

/**
 * Adds a section that opens the settings of the patches to the preferences of Slack,
 * before the section about the app.
 */
@SuppressWarnings("unused")
public final class PreferencesEntry {

    private static final String ABOUT_HEADER_ID = "id_about_title";

    /**
     * An entry of the section about the app, whose style the added entry copies.
     */
    private static final String STYLE_TEMPLATE_ID = "id_about_open_source";

    private static final String HEADER_ID = "id_hagiahaya_patches_title";

    private static final String ENTRY_ID = "id_hagiahaya_patches";

    private static final String DIVIDER_ID = "id_hagiahaya_patches_divider";

    /**
     * Bit mask of the arguments of the divider Slack leaves at their defaults.
     */
    private static final int DIVIDER_DEFAULTS = 6;

    /**
     * Injection point. Called with the entries of the preferences screen.
     */
    public static List<Object> addEntry(List<Object> items) {
        try {
            int aboutIndex = -1;
            SKListHeaderPresentationObject aboutHeader = null;
            SKListGenericPresentationObject template = null;
            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                if (item instanceof SKListHeaderPresentationObject) {
                    String id = ((SKListHeaderPresentationObject) item).id;
                    if (HEADER_ID.equals(id)) {
                        return items;
                    }
                    if (ABOUT_HEADER_ID.equals(id)) {
                        aboutIndex = i;
                        aboutHeader = (SKListHeaderPresentationObject) item;
                    }
                } else if (item instanceof SKListGenericPresentationObject
                        && STYLE_TEMPLATE_ID.equals(((SKListGenericPresentationObject) item).id)) {
                    template = (SKListGenericPresentationObject) item;
                }
            }
            Context context = Settings.getContext();
            if (aboutHeader == null || template == null || context == null) {
                return items;
            }

            int icon = context.getResources().getIdentifier("settings", "drawable", context.getPackageName());

            List<Object> result = new ArrayList<>(items);
            result.addAll(aboutIndex, Arrays.asList(
                    new SKListHeaderPresentationObject(HEADER_ID, new CharSequenceResource("HagiaHaya Patches"),
                            null, null, aboutHeader.sectionHeaderId, aboutHeader.belongsToSectionId,
                            aboutHeader.isSticky, null, aboutHeader.options, aboutHeader.accessories),
                    new SKListGenericPresentationObject(ENTRY_ID, new CharSequenceResource("Patch settings"),
                            new CharSequenceResource("Turn the features of the patches on or off"),
                            icon != 0 ? new SKImageResource.Icon(icon, null, null) : null, null, null,
                            template.type, template.options, new SKListAccessories(null, null, null),
                            template.sectionHeaderId, template.belongsToSectionId),
                    new SKListDividerPresentationObject(DIVIDER_ID, DIVIDER_DEFAULTS)
            ));
            return result;
        } catch (Exception ex) {
            Log.e(Settings.TAG, "Failed to add the settings of the patches", ex);
            return items;
        }
    }

    /**
     * Injection point. Called with the handler of the events of the preferences screen.
     *
     * @return A handler that opens the settings of the patches when their entry is tapped.
     */
    public static Function1 wrapEventSink(Function1 eventSink) {
        return event -> {
            if (event instanceof PreferenceSettings$Event.ClickItem
                    && ENTRY_ID.equals(((PreferenceSettings$Event.ClickItem) event).itemId)) {
                openSettings();
                return null;
            }
            return eventSink.invoke(event);
        };
    }

    private static void openSettings() {
        try {
            Activity activity = Settings.getResumedActivity();
            if (activity != null) {
                activity.startActivity(new Intent(activity, SettingsActivity.class));
            } else {
                Context context = Settings.getContext();
                context.startActivity(new Intent(context, SettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Exception ex) {
            Log.e(Settings.TAG, "Failed to open the settings of the patches", ex);
        }
    }
}
