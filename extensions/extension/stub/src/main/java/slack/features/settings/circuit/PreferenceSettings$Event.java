package slack.features.settings.circuit;

/**
 * An event of the Preferences screen of Slack.
 */
public interface PreferenceSettings$Event {
    /**
     * A tap on an entry of the list.
     */
    final class ClickItem implements PreferenceSettings$Event {
        public String itemId;
    }
}
