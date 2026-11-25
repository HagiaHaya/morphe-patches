package app.template.extension.pepper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Patterns the user entered in the settings, one per line, where * stands for any text.
 * Letter case is ignored.
 */
final class TextPatterns {

    private final List<Pattern> patterns = new ArrayList<>();

    /**
     * @param wholeText If a pattern must match the whole text, or any part of it.
     */
    TextPatterns(List<String> lines, boolean wholeText) {
        for (String line : lines) {
            StringBuilder regex = new StringBuilder();
            for (String part : line.split("\\*", -1)) {
                if (regex.length() > 0 || line.startsWith("*")) {
                    regex.append(".*");
                }
                regex.append(Pattern.quote(part));
            }
            String finalRegex = wholeText ? regex.toString() : ".*" + regex + ".*";
            patterns.add(Pattern.compile(finalRegex,
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL));
        }
    }

    boolean isEmpty() {
        return patterns.isEmpty();
    }

    boolean matches(String text) {
        if (text == null) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }
}
