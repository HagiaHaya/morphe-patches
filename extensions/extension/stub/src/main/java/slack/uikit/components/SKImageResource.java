package slack.uikit.components;

import slack.uikit.text.ParcelableTextResource;
import slack.uikit.theme.SKColor;

/**
 * An image of the user interface of Slack.
 */
public abstract class SKImageResource {
    /**
     * A drawable resource.
     */
    public static final class Icon extends SKImageResource {
        public Icon(int iconResId, SKColor tintColor, ParcelableTextResource contentDescription) {
        }
    }
}
