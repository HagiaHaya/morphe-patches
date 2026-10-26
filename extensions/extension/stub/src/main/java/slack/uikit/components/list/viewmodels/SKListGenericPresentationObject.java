package slack.uikit.components.list.viewmodels;

import slack.uikit.components.SKImageResource;
import slack.uikit.components.list.data.SKListAccessories;
import slack.uikit.components.list.data.SKListGenericEntityType;
import slack.uikit.components.list.data.SKListItemGenericOptions;
import slack.uikit.model.BundleWrapper;
import slack.uikit.text.TextResource;

/**
 * An entry of a list of Slack, with a title, a subtitle and an icon.
 */
public final class SKListGenericPresentationObject {
    public String id;
    public SKListGenericEntityType type;
    public SKListItemGenericOptions options;
    public String sectionHeaderId;
    public String belongsToSectionId;

    public SKListGenericPresentationObject(String id, TextResource title, TextResource subtitle, SKImageResource icon,
                                           TextResource contentDescription, BundleWrapper bundleWrapper,
                                           SKListGenericEntityType type, SKListItemGenericOptions options,
                                           SKListAccessories accessories, String sectionHeaderId,
                                           String belongsToSectionId) {
    }
}
