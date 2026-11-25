package app.template.extension.slack;

import app.template.extension.slack.settings.Settings;

@SuppressWarnings("unused")
public final class SilentTypingPatch {

    /**
     * Injection point. Called before Slack tells the others that the user is typing.
     *
     * @return If the typing indicator is not sent.
     */
    public static boolean hideTyping() {
        return Settings.isEnabled(Settings.SILENT_TYPING);
    }
}
