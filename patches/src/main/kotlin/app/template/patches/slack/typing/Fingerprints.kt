package app.template.patches.slack.typing

import app.morphe.patcher.Fingerprint

/**
 * Sends the "is typing" indicator of the user over the web socket,
 * unless "Display typing indicators" is turned off in the advanced preferences.
 */
internal object SendTypingMessageFingerprint : Fingerprint(
    definingClass = "Lslack/services/usertyping/UserTypingHandler;",
    name = "sendTypingMessage",
    returnType = "V",
    parameters = listOf(),
)
