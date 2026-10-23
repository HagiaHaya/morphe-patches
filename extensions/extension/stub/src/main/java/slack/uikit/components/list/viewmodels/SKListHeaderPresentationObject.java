package slack.uikit.components.list.viewmodels;

import slack.uikit.components.SKImageResource;
import slack.uikit.components.list.data.SKListAccessories;
import slack.uikit.components.list.data.SKListItemDefaultOptions;
import slack.uikit.model.BundleWrapper;
import slack.uikit.text.TextResource;

/**
 * The header of a section of a list of Slack.
 */
public final class SKListHeaderPresentationObject {
    public String id;
    public SKImageResource icon;
    public String sectionHeaderId;
    public String belongsToSectionId;
    public boolean isSticky;
    public BundleWrapper bundleWrapper;
    public SKListItemDefaultOptions options;
    public SKListAccessories accessories;

    public SKListHeaderPresentationObject(String id, TextResource title, TextResource subtitle, SKImageResource icon,
                                          String sectionHeaderId, String belongsToSectionId, boolean isSticky,
                                          BundleWrapper bundleWrapper, SKListItemDefaultOptions options,
                                          SKListAccessories accessories) {
    }
}
