package app.template.patches.slack.markdown

import app.morphe.patcher.Fingerprint

/**
 * Formats the text of the message composer after the user typed the closing marker
 * of Slack markup, such as the second asterisk of *bold*.
 */
internal object MarkdownTagResultFingerprint : Fingerprint(
    definingClass = "Lslack/services/autotag/inline/MarkdownAutoInlineTagProvider;",
    name = "tagResult",
    returnType = "Z",
    parameters = listOf(
        "Landroid/text/Editable;",
        "I",
        "Lslack/services/slacktextview/SlackTextView\$autoInlineTagView\$1;",
    ),
)
