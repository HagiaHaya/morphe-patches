package app.template.patches.slack.history

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

// Slack is not obfuscated, so classes and methods are found by name. The parameters are
// listed in full, so patching fails if a query gains a column instead of reading the wrong one.
// Statements that cache messages are found by their SQL, as R8 inlined some of them.

/**
 * A statement that caches messages, found by the start of its SQL. SQLDelight binds the arguments
 * with a lambda that captures them in the order of the columns, which every method running the
 * statement creates right before it loads the SQL. R8 inlined some of the queries into other
 * methods, so the lambdas are hooked rather than the query methods.
 *
 * @param binderParameters Leading parameters of the constructor of the lambda,
 * which are passed on to the extension.
 */
internal class CacheStatement(val sqlPrefix: String, val binderParameters: List<String>)

private val MESSAGE_COLUMNS = listOf(
    "Ljava/lang/String;", // local_id
    "Ljava/lang/String;", // ts
    "Ljava/lang/String;", // channel_id
    "Ljava/lang/String;", // client_msg_id
    "Ljava/lang/String;", // msg_send_state
    "Ljava/lang/Long;", // ephemeral_msg_type
    "Ljava/lang/String;", // calls_room_id
    "Ljava/lang/String;", // thread_ts
    "Ljava/lang/String;", // message_json
)

/**
 * Caches a message of a conversation in the `messages` table of Slack, as a plain insert
 * and as an upsert that keeps the newer copy:
 *
 * ```
 * INSERT INTO messages(local_id, ts, channel_id, client_msg_id, msg_send_state, ephemeral_msg_type,
 *     calls_room_id, thread_ts, message_json, reply_broadcast, subtype, user_id, local_ts, updated_timestamp)
 * ```
 */
internal val insertMessageStatement = CacheStatement("INSERT INTO messages(local_id, ts, channel_id,", MESSAGE_COLUMNS)

/**
 * Caches a message of a conversation unless it is cached already, such as from a push notification.
 * Its columns are those of [insertMessageStatement].
 */
internal val insertOrIgnoreMessageStatement =
    CacheStatement("INSERT OR IGNORE INTO messages(local_id, ts, channel_id,", MESSAGE_COLUMNS)

private const val MESSAGES_QUERIES_CLASS = "Lslack/persistence/messages/MessagesQueries;"

/**
 * Updates a cached message of a conversation, such as when it is edited, gets a reaction,
 * or a message sent from the app gets its timestamp from the server:
 *
 * ```
 * UPDATE messages
 * SET ts = ?, client_msg_id = ?, msg_send_state = ?,
 *     ephemeral_msg_type = ?, message_json = ?,
 *     calls_room_id = ?, thread_ts = ?, subtype = ?, user_id = ?
 * WHERE local_id = ?
 * ```
 */
internal object UpdateMessageByLocalIdFingerprint : Fingerprint(
    definingClass = MESSAGES_QUERIES_CLASS,
    name = "updateMessageByLocalId",
    parameters = listOf(
        "Ljava/lang/String;", // ts
        "Ljava/lang/String;", // client_msg_id
        "Ljava/lang/String;", // msg_send_state
        "Ljava/lang/Long;", // ephemeral_msg_type
        "Ljava/lang/String;", // message_json
        "Ljava/lang/String;", // calls_room_id
        "Ljava/lang/String;", // thread_ts
        "Ljava/lang/String;", // subtype
        "Ljava/lang/String;", // user_id
        "Ljava/lang/String;", // local_id
    ),
    filters = listOf(
        string("UPDATE messages\nSET ts = ?, client_msg_id = ?, msg_send_state = ?,", StringComparisonType.STARTS_WITH),
    ),
)

private const val THREAD_MESSAGE_QUERIES_CLASS = "Lslack/persistence/threads/ThreadMessageQueries;"

/**
 * Caches a message of a thread in the `message_threads` table, as a plain insert
 * and as an upsert that keeps the newer copy:
 *
 * ```
 * INSERT INTO message_threads(ts, team_id, channel_id, client_msg_id, local_id, msg_send_state,
 *   ephemeral_msg_type, thread_ts, message_blob, is_reply_broadcast, event_sub_type, local_ts,
 *   updated_timestamp) VALUES (...)
 * ```
 */
internal val insertThreadMessageStatement = CacheStatement(
    "INSERT INTO message_threads(",
    listOf(
        "Ljava/lang/String;", // ts
        "Ljava/lang/String;", // team_id
        "Ljava/lang/String;", // channel_id
        "Ljava/lang/String;", // client_msg_id
        "Ljava/lang/String;", // local_id
        THREAD_MESSAGE_QUERIES_CLASS, // The queries, whose adapters convert the columns.
        "I", // msg_send_state
        "Ljava/lang/Integer;", // ephemeral_msg_type
        "Ljava/lang/String;", // thread_ts
        "Ljava/lang/String;", // message_blob
    ),
)

/**
 * Updates a cached message of a thread:
 *
 * ```
 * UPDATE message_threads
 * SET message_blob = ?
 * WHERE team_id = ? AND local_id = ?
 * ```
 */
internal object UpdateThreadMessageBlobFingerprint : Fingerprint(
    definingClass = THREAD_MESSAGE_QUERIES_CLASS,
    name = "update_message_blob",
    parameters = listOf(
        "Ljava/lang/String;", // message_blob
        "Ljava/lang/String;", // team_id
        "Ljava/lang/String;", // local_id
    ),
    filters = listOf(
        string("UPDATE message_threads\nSET message_blob = ?", StringComparisonType.STARTS_WITH),
    ),
)

internal const val MESSAGE_DELETED_EVENT_CLASS = "Lslack/corelib/rtm/msevents/MessageDeletedEvent;"

/**
 * Handles a message_deleted event of the real time messaging socket. It deletes the message
 * from the cache only if it is cached:
 *
 * ```
 * val event = jsonInflater.inflate(wrapper.jsonData, MessageDeletedEvent::class.java)
 * val channel = event.getChannel()
 * val deletedTs = event.getDeletedTs()
 * ```
 */
internal object RemoveMessageEventFingerprint : Fingerprint(
    definingClass = "Lslack/services/messages/eventhandlers/MessageEventHandler;",
    name = "removeMessage",
    returnType = "V",
    parameters = listOf("Lslack/rtm/events/SocketEventWrapper;"),
    filters = listOf(
        methodCall(definingClass = MESSAGE_DELETED_EVENT_CLASS, name = "getChannel"),
        methodCall(definingClass = MESSAGE_DELETED_EVENT_CLASS, name = "getDeletedTs"),
    ),
)

internal const val CONVERSATION_REQUEST_CLASS = "Lslack/services/api/conversations/ConversationRequest;"

/**
 * Requests a page of conversations.history. Fetching the latest messages of a conversation
 * and paging back through older ones both go through it:
 *
 * ```
 * fun history(request: ConversationRequest, traceContext: TraceContext, background: Boolean): Single<MsgHistory>
 * ```
 */
internal object ConversationHistoryFingerprint : Fingerprint(
    definingClass = "Lslack/services/api/conversations/ConversationHistoryApiImpl;",
    name = "history",
    returnType = "Lio/reactivex/rxjava3/core/Single;",
    parameters = listOf(
        CONVERSATION_REQUEST_CLASS,
        "Lslack/telemetry/tracing/TraceContext;",
        "Z",
    ),
)

internal const val GET_THREAD_COROUTINE_CLASS = "Lslack/reply/impl/ReplyRepositoryImpl\$getThreadRx\$1;"

/**
 * Coroutine requesting conversations.replies for a thread. It returns the ApiResult
 * of the request, either right away or when it resumes after the response.
 */
internal object GetThreadInvokeSuspendFingerprint : Fingerprint(
    definingClass = GET_THREAD_COROUTINE_CLASS,
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(
            definingClass = "Lslack/services/api/conversations/AuthedConversationsApi;",
            name = "conversationsReplies\$default",
        ),
    ),
)
