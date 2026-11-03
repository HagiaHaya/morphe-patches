package app.template.extension.slack;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.template.extension.slack.settings.Settings;
import slack.services.autotag.inline.MarkdownAutoInlineTagProvider;
import slack.services.slacktextview.SlackTextView$autoInlineTagView$1;
import slack.textformatting.spans.CodeStyleSpan;
import slack.textformatting.spans.ItalicStyleSpan;
import slack.textformatting.spans.LinkStyleSpan;
import slack.textformatting.spans.StrikethroughStyleSpan;

/**
 * Formats the message composer as the user types standard Markdown instead of Slack markup:
 * **bold** or __bold__, *italic* or _italic_, ~~strikethrough~~ or ~strikethrough~, `code`
 * and [text](link). Like Slack, the markers are replaced by the formatting when the closing
 * marker is typed.
 */
@SuppressWarnings("unused")
public final class StandardMarkdownPatch {

    private static final String TAG = "MorpheSlack";

    /**
     * Use the formatting of Slack.
     */
    private static final int ORIGINAL = -1;

    private static final int NOT_FORMATTED = 0;

    private static final int FORMATTED = 1;

    private static final int BOLD = 0;
    private static final int ITALIC = 1;
    private static final int STRIKETHROUGH = 2;
    private static final int CODE = 3;
    private static final int LINK = 4;

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\[\\]]+)]\\((https?://[^\\s()]+)\\)$");

    /**
     * Class names of the spans of code, inside which nothing is formatted.
     */
    private static final String CODE_SPAN = "slack.textformatting.spans.CodeStyleSpan";
    private static final String PREFORMATTED_SPAN = "slack.textformatting.spans.PreformattedStyleSpan";

    private static final class Match {
        final int type;
        final int start;
        final int innerStart;
        final int innerEnd;
        final String link;

        Match(int type, int start, int innerStart, int innerEnd, String link) {
            this.type = type;
            this.start = start;
            this.innerStart = innerStart;
            this.innerEnd = innerEnd;
            this.link = link;
        }
    }

    /**
     * Replaced by the patch with the creation of the bold span of Slack,
     * which has no constructor of its own.
     */
    private static Object newBoldSpan() {
        return null;
    }

    /**
     * Injection point. Called after the user typed a character in the message composer.
     *
     * @param cursor The position after the character typed.
     * @return 1 if the text was formatted, 0 if not, or -1 to let Slack format it.
     */
    public static int tagResult(MarkdownAutoInlineTagProvider provider, Editable editable, int cursor,
                                SlackTextView$autoInlineTagView$1 view) {
        if (!Settings.isEnabled(Settings.STANDARD_MARKDOWN)) {
            return ORIGINAL;
        }
        try {
            // Only while typing, not when deleting or pasting.
            if (provider.pastedText || editable.length() < provider.prevTextLength
                    || cursor < 1 || cursor > editable.length()) {
                return NOT_FORMATTED;
            }
            char typed = editable.charAt(cursor - 1);
            if ("*_~`)".indexOf(typed) < 0) {
                return NOT_FORMATTED;
            }

            int lineStart = 0;
            for (int i = cursor - 1; i > 0; i--) {
                if (editable.charAt(i - 1) == '\n') {
                    lineStart = i;
                    break;
                }
            }
            String line = editable.subSequence(lineStart, cursor).toString();
            Match match = match(line, typed);
            if (match == null || isInCode(editable, lineStart + match.start, cursor)) {
                return NOT_FORMATTED;
            }

            Object span = createSpan(provider, match, line);
            if (span == null) {
                return NOT_FORMATTED;
            }
            SpannableStringBuilder inner = new SpannableStringBuilder(
                    editable.subSequence(lineStart + match.innerStart, lineStart + match.innerEnd));
            inner.setSpan(span, 0, inner.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.replaceText(lineStart + match.start, cursor, inner);
            return FORMATTED;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to format Markdown", ex);
            return NOT_FORMATTED;
        }
    }

    private static Match match(String line, char typed) {
        switch (typed) {
            case '*': {
                Match bold = matchDelimited(line, "**", BOLD);
                return bold != null ? bold : matchDelimited(line, "*", ITALIC);
            }
            case '_': {
                Match bold = matchDelimited(line, "__", BOLD);
                return bold != null ? bold : matchDelimited(line, "_", ITALIC);
            }
            case '~': {
                Match strikethrough = matchDelimited(line, "~~", STRIKETHROUGH);
                return strikethrough != null ? strikethrough : matchDelimited(line, "~", STRIKETHROUGH);
            }
            case '`':
                return matchDelimited(line, "`", CODE);
            case ')': {
                Matcher matcher = LINK_PATTERN.matcher(line);
                if (!matcher.find()) {
                    return null;
                }
                return new Match(LINK, matcher.start(), matcher.start(1), matcher.end(1), matcher.group(2));
            }
            default:
                return null;
        }
    }

    /**
     * Finds the text between an opening marker and the closing marker at the end of the line,
     * after the rules of CommonMark: the text does not start or end with a space, a single
     * marker is not part of a double one, and underscores only open and close at word boundaries.
     */
    private static Match matchDelimited(String line, String marker, int type) {
        int length = marker.length();
        int close = line.length() - length;
        if (close < 1 || !line.endsWith(marker) || isEscaped(line, close)) {
            return null;
        }
        char markerChar = marker.charAt(0);
        boolean single = length == 1;
        // The closing marker of *italic* is not the end of **bold**.
        if (single && line.charAt(close - 1) == markerChar) {
            return null;
        }
        if (Character.isWhitespace(line.charAt(close - 1))) {
            return null;
        }

        for (int open = line.lastIndexOf(marker, close - 1); open >= 0; open = line.lastIndexOf(marker, open - 1)) {
            int innerStart = open + length;
            if (innerStart >= close || isEscaped(line, open)) {
                continue;
            }
            char before = open > 0 ? line.charAt(open - 1) : ' ';
            char first = line.charAt(innerStart);
            if (Character.isWhitespace(first)) {
                continue;
            }
            if (single && (before == markerChar || first == markerChar)) {
                continue;
            }
            if (!single && before == markerChar) {
                continue;
            }
            if (markerChar == '_' && Character.isLetterOrDigit(before)) {
                continue;
            }
            return new Match(type, open, innerStart, close, null);
        }
        return null;
    }

    private static boolean isEscaped(String line, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static boolean isInCode(Editable editable, int start, int end) {
        for (Object span : editable.getSpans(start, end, Object.class)) {
            String name = span.getClass().getName();
            if (name.equals(CODE_SPAN) || name.equals(PREFORMATTED_SPAN)) {
                return true;
            }
        }
        return false;
    }

    private static Object createSpan(MarkdownAutoInlineTagProvider provider, Match match, String line) {
        switch (match.type) {
            case BOLD:
                return newBoldSpan();
            case ITALIC:
                return new ItalicStyleSpan();
            case STRIKETHROUGH:
                return new StrikethroughStyleSpan();
            case CODE:
                return new CodeStyleSpan(themedContext(provider));
            case LINK:
                return new LinkStyleSpan(themedContext(provider), match.link,
                        line.substring(match.innerStart, match.innerEnd), null, null, false, false);
            default:
                return null;
        }
    }

    private static Context themedContext(MarkdownAutoInlineTagProvider provider) {
        return provider.darkModeContext.getThemedContext();
    }
}
