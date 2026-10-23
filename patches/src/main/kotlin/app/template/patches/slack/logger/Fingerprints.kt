package app.template.patches.slack.logger

import app.morphe.patcher.Fingerprint

internal const val MESSAGE_PERSISTENCE_HELPER_CLASS = "Lslack/messages/impl/MessagePersistenceHelperImpl;"

internal const val MESSAGE_EVENT_HANDLER_CLASS = "Lslack/services/messages/eventhandlers/MessageEventHandler;"

internal const val SOCKET_EVENT_WRAPPER_CLASS = "Lslack/rtm/events/SocketEventWrapper;"

internal const val MESSAGE_DELETED_EVENT_CLASS = "Lslack/corelib/rtm/msevents/MessageDeletedEvent;"

internal const val CONVERSATION_REQUEST_CLASS = "Lslack/services/api/conversations/ConversationRequest;"

internal const val GET_THREAD_COROUTINE_CLASS = "Lslack/reply/impl/ReplyRepositoryImpl\$getThreadRx\$1;"

// The methods below are found by name and parameters only, as the patch keeping the message
// history may have wrapped them already, and the wrappers compose.

/**
 * The JSON parser of Slack, a single instance.
 */
internal object JsonInflaterConstructorFingerprint : Fingerprint(
    definingClass = "Lslack/commons/json/JsonInflaterImpl;",
    name = "<init>",
)

/**
 * Replaces a cached message and updates the screens showing it, as when it is edited.
 */
internal object UpdateMessageFingerprint : Fingerprint(
    definingClass = MESSAGE_PERSISTENCE_HELPER_CLASS,
    name = "updateMessage",
    returnType = "Ljava/util/Optional;",
    parameters = listOf("Ljava/lang/String;", "Lslack/model/Message;", "Lslack/model/MessageState;", "Z"),
)

/**
 * Removes a message deleted by someone, from the message_deleted event of the web socket.
 */
internal object RemoveMessageFingerprint : Fingerprint(
    definingClass = MESSAGE_EVENT_HANDLER_CLASS,
    name = "removeMessage",
    returnType = "V",
    parameters = listOf(SOCKET_EVENT_WRAPPER_CLASS),
)

/**
 * Requests a page of conversations.history.
 */
internal object HistoryFingerprint : Fingerprint(
    definingClass = "Lslack/services/api/conversations/ConversationHistoryApiImpl;",
    name = "history",
    returnType = "Lio/reactivex/rxjava3/core/Single;",
    parameters = listOf(CONVERSATION_REQUEST_CLASS, "Lslack/telemetry/tracing/TraceContext;", "Z"),
)

/**
 * Coroutine requesting conversations.replies for a thread.
 */
internal object GetThreadFingerprint : Fingerprint(
    definingClass = GET_THREAD_COROUTINE_CLASS,
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)
