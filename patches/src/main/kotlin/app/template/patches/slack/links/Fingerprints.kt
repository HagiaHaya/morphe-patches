package app.template.patches.slack.links

import app.morphe.patcher.Fingerprint

internal const val RICH_TEXT_ENCODER_CLASS = "Lslack/services/textformatting/impl/encoder/RichTextEncoderImpl;"

/**
 * Encodes the text of the message composer into the chunks of rich text Slack sends,
 * with a link chunk for every link span and every link it detects in the text.
 */
internal object EncodeFormattedChunksFingerprint : Fingerprint(
    definingClass = RICH_TEXT_ENCODER_CLASS,
    name = "encodeFormattedChunks",
    returnType = "Ljava/util/List;",
    parameters = listOf("Ljava/lang/CharSequence;", "Z"),
)
