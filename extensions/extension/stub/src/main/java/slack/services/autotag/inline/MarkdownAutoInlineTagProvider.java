package slack.services.autotag.inline;

import slack.theming.darkmode.DarkModeHelperImpl;

/**
 * Formats the text of the message composer as the user types Slack markup, such as *bold*.
 */
public final class MarkdownAutoInlineTagProvider {
    public DarkModeHelperImpl darkModeContext;
    /**
     * If the last change of the text was a paste, which is not formatted.
     */
    public boolean pastedText;
    public int prevTextLength;
}
