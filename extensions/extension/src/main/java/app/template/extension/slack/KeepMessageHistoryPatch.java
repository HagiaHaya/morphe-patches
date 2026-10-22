package app.template.extension.slack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.slack.eithernet.ApiResult;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.functions.Function;
import slack.api.response.ResponseMetadata;
import slack.commons.json.JsonInflater;
import slack.model.Message;
import slack.services.api.conversations.ConversationRequest;
import slack.services.api.conversations.MsgHistory;
import slack.services.api.conversations.RepliesApiResponse;

/**
 * Keeps a copy of every message Slack caches in {@link MessageArchive}, and shows the archived
 * messages where the server hides older ones, as it does on free workspaces after 90 days.
 * <p>
 * conversations.history answers with {@code is_limited} when it hid older messages. Then the
 * archived messages older than the oldest message returned are added to the response, and the
 * response points to the next page with a cursor of its own, which is answered from the archive.
 */
@SuppressWarnings("unused")
public final class KeepMessageHistoryPatch {

    private static final String TAG = "MorpheSlack";

    private static final String PREFERENCES_NAME = "hagiahaya_patches";

    private static final String ENABLED_KEY = "keep_message_history";

    /**
     * Prefix of the cursors of the pages answered from the archive,
     * followed by the timestamp the page ends before.
     */
    private static final String CURSOR_PREFIX = "hagiahaya_archive:";

    /**
     * Prefix of the decoded cursors of conversations.history, followed by the timestamp
     * of the first message of the next page, without the dot.
     */
    private static final String SERVER_CURSOR_PREFIX = "next_ts:";

    /**
     * Value of {@code msg_send_state} of messages synced from the server.
     */
    private static final int SYNCED = 1;

    /**
     * Value of {@code msg_send_state} of messages from the server Slack did not sync
     * the history around yet, such as the ones of push notifications.
     */
    private static final int UNSYNCED = 2;

    private static SharedPreferences preferences;

    /**
     * Conversation of the messages Slack cached recently, by their local ID. Updates of
     * messages only name the local ID, and messages sent from the app only get their
     * timestamp from the server with an update.
     */
    private static final Map<String, String> channelIdsByLocalId = new LinkedHashMap<String, String>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 2000;
        }
    };

    /**
     * Oldest message timestamp conversations.history returned per conversation, for an empty
     * page whose request has no timestamp to start from.
     */
    private static final Map<String, String> oldestServerTs = Collections.synchronizedMap(new HashMap<>());

    /**
     * Injection point. Called when the app is created.
     */
    public static void initialize(Context context) {
        try {
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            MessageArchive.initialize(context);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to initialize the message archive", ex);
        }
    }

    static boolean isEnabled() {
        return preferences == null || preferences.getBoolean(ENABLED_KEY, true);
    }

    static void setEnabled(boolean enabled) {
        if (preferences != null) {
            preferences.edit().putBoolean(ENABLED_KEY, enabled).apply();
        }
    }

    private static MessageArchive archive() {
        return isEnabled() ? MessageArchive.get() : null;
    }

    private static boolean isFromServer(int sendState, Number ephemeralType) {
        return (sendState == SYNCED || sendState == UNSYNCED)
                && (ephemeralType == null || ephemeralType.intValue() == 0);
    }

    private static int parseSendState(String sendState) {
        try {
            return Integer.parseInt(sendState);
        } catch (Exception ex) {
            return -1;
        }
    }

    // region Archiving

    private static void rememberChannelId(String localId, String channelId) {
        if (localId != null && channelId != null) {
            synchronized (channelIdsByLocalId) {
                channelIdsByLocalId.put(localId, channelId);
            }
        }
    }

    private static String channelIdOf(String localId) {
        synchronized (channelIdsByLocalId) {
            return channelIdsByLocalId.get(localId);
        }
    }

    private static void saveMessage(String localId, String ts, String channelId, String sendState,
                                    Long ephemeralType, String threadTs, String json, boolean replace) {
        try {
            rememberChannelId(localId, channelId);
            MessageArchive archive = archive();
            if (archive == null || ts == null || channelId == null || json == null
                    || !isFromServer(parseSendState(sendState), ephemeralType)) {
                return;
            }
            archive.save(channelId, ts, threadTs, true, json, replace);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to archive a message", ex);
        }
    }

    /**
     * Injection point. Called when Slack caches a message of a conversation.
     * The parameters are the columns of the messages table, up to message_json.
     */
    public static void onMessageInserted(String localId, String ts, String channelId, String clientMsgId,
                                         String sendState, Long ephemeralType, String callsRoomId,
                                         String threadTs, String json) {
        saveMessage(localId, ts, channelId, sendState, ephemeralType, threadTs, json, true);
    }

    /**
     * Injection point. Called when Slack caches a message of a conversation unless it is
     * cached already, such as a message from a push notification, which can lack details.
     */
    public static void onMessageInsertedIfAbsent(String localId, String ts, String channelId, String clientMsgId,
                                                 String sendState, Long ephemeralType, String callsRoomId,
                                                 String threadTs, String json) {
        saveMessage(localId, ts, channelId, sendState, ephemeralType, threadTs, json, false);
    }

    /**
     * Injection point. Called when Slack updates a cached message of a conversation,
     * such as when it is edited, gets a reaction, or a message sent from the app is delivered.
     */
    public static void onMessageUpdated(String ts, String clientMsgId, String sendState, Long ephemeralType,
                                        String json, String callsRoomId, String threadTs, String subtype,
                                        String userId, String localId) {
        try {
            MessageArchive archive = archive();
            if (archive == null || ts == null || json == null
                    || !isFromServer(parseSendState(sendState), ephemeralType)) {
                return;
            }
            String channelId = channelIdOf(localId);
            if (channelId != null) {
                archive.save(channelId, ts, threadTs, true, json, true);
            } else {
                archive.update(ts, threadTs, json);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to archive a message update", ex);
        }
    }

    /**
     * Injection point. Called when Slack caches a message of a thread, or updates the cached copy.
     * The parameters are the columns of the message_threads table up to message_blob, and the queries
     * of the table after local_id.
     */
    public static void onThreadMessageInserted(String ts, String teamId, String channelId, String clientMsgId,
                                               String localId, Object queries, int sendState,
                                               Integer ephemeralType, String threadTs, String json) {
        try {
            rememberChannelId(localId, channelId);
            MessageArchive archive = archive();
            if (archive == null || ts == null || channelId == null || json == null
                    || !isFromServer(sendState, ephemeralType)) {
                return;
            }
            archive.save(channelId, ts, threadTs, false, json, true);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to archive a thread message", ex);
        }
    }

    /**
     * Injection point. Called when Slack updates a cached message of a thread,
     * such as when it is edited or gets a reaction.
     */
    public static void onThreadMessageUpdated(String json, String teamId, String localId) {
        try {
            MessageArchive archive = archive();
            if (archive == null || json == null) {
                return;
            }
            JSONObject message = new JSONObject(json);
            String ts = message.optString("ts", null);
            String threadTs = message.optString("thread_ts", null);
            if (ts == null) {
                return;
            }
            String channelId = channelIdOf(localId);
            if (channelId != null) {
                archive.save(channelId, ts, threadTs, false, json, true);
            } else {
                archive.update(ts, threadTs, json);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to archive a thread message update", ex);
        }
    }

    /**
     * Injection point. Called when a message is deleted, including messages Slack
     * no longer caches.
     */
    public static void onMessageDeleted(String channelId, String ts) {
        try {
            MessageArchive archive = archive();
            if (archive != null && channelId != null && ts != null) {
                archive.delete(channelId, ts);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to delete an archived message", ex);
        }
    }

    // endregion

    // region Showing archived messages

    private static List<Object> inflateMessages(JsonInflater inflater, List<String> json) {
        List<Object> messages = new ArrayList<>(json.size());
        for (String messageJson : json) {
            try {
                messages.add(inflater.inflate(Message.class, messageJson));
            } catch (Exception ex) {
                Log.e(TAG, "Failed to parse an archived message", ex);
            }
        }
        return messages;
    }

    private static String tsOf(Object message) {
        return message instanceof Message ? ((Message) message).ts : null;
    }

    private static String oldestTs(List<Object> messages) {
        String oldest = null;
        for (Object message : messages) {
            String ts = tsOf(message);
            if (ts != null && (oldest == null || ts.compareTo(oldest) < 0)) {
                oldest = ts;
            }
        }
        return oldest;
    }

    /**
     * @return The timestamp of the first message of the page a cursor of conversations.history
     * points to, or null if the cursor has another format.
     */
    private static String decodeServerCursor(String cursor) {
        try {
            String decoded = new String(Base64.decode(cursor, Base64.DEFAULT), StandardCharsets.UTF_8);
            if (!decoded.startsWith(SERVER_CURSOR_PREFIX)) {
                return null;
            }
            String digits = decoded.substring(SERVER_CURSOR_PREFIX.length());
            if (!digits.matches("\\d{7,}")) {
                return null;
            }
            return digits.substring(0, digits.length() - 6) + "." + digits.substring(digits.length() - 6);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int limitOf(ConversationRequest request) {
        // Slack asks for 100 messages if it has no limit.
        return request.limit < 1 ? 100 : request.limit;
    }

    /**
     * Builds a response from the server response and the archived messages before a timestamp.
     *
     * @param room How many archived messages are added. If there are more, the response
     *             points to the next page with an archive cursor.
     * @return The response, or null if there are no archived messages before the timestamp.
     */
    private static MsgHistory withArchivedMessages(MessageArchive archive, JsonInflater inflater, MsgHistory history,
                                                   List<Object> serverMessages, String channelId, String beforeTs,
                                                   boolean inclusive, String afterTs, int room) {
        List<String> archived = archive.conversationPage(channelId, beforeTs, inclusive, afterTs, room + 1);
        if (archived.isEmpty()) {
            return null;
        }
        boolean hasMore = archived.size() > room;
        List<Object> messages = new ArrayList<>(serverMessages);
        messages.addAll(inflateMessages(inflater, hasMore ? archived.subList(0, room) : archived));

        String oldest = oldestTs(messages);
        ResponseMetadata metadata = hasMore && oldest != null ? new ResponseMetadata(CURSOR_PREFIX + oldest) : null;

        return new MsgHistory(
                true,
                null,
                messages,
                hasMore,
                // Show that the server hides older messages once the archive has no more.
                !hasMore,
                history != null ? history.deleted : null,
                metadata,
                history != null ? history.mutationTimestamps : null,
                history != null ? history.latestUpdates : null,
                history != null ? history.unchangedMessages : null
        );
    }

    /**
     * Builds a response of the messages right after a timestamp: the archived ones the server hid,
     * then the ones it returned.
     *
     * @return The response, or null if there are no archived messages after the timestamp.
     */
    private static MsgHistory withArchivedMessagesAfter(MessageArchive archive, JsonInflater inflater, MsgHistory history,
                                                        List<Object> serverMessages, String channelId, String afterTs,
                                                        boolean inclusive, String oldestServer, int limit) {
        // Oldest first.
        List<String> archived = archive.conversationPageAfter(channelId, afterTs, inclusive, oldestServer, limit + 1);
        if (archived.isEmpty()) {
            return null;
        }
        List<Object> messages = inflateMessages(inflater, archived);
        List<Object> newer = new ArrayList<>(serverMessages);
        Collections.sort(newer, (a, b) -> {
            String aTs = tsOf(a);
            String bTs = tsOf(b);
            return aTs == null || bTs == null ? 0 : aTs.compareTo(bTs);
        });
        messages.addAll(newer);

        boolean hasMore = messages.size() > limit || history.hasMore;
        if (messages.size() > limit) {
            messages = new ArrayList<>(messages.subList(0, limit));
        }
        // Newest first, as the server lists them.
        Collections.reverse(messages);

        return new MsgHistory(
                true,
                null,
                messages,
                hasMore,
                // The window starts at the requested timestamp, nothing before it is hidden.
                false,
                history.deleted,
                // The cursor of the server does not continue from the messages kept.
                null,
                history.mutationTimestamps,
                history.latestUpdates,
                history.unchangedMessages
        );
    }

    /**
     * Injection point. Answers a request for a page of the archive, before the request is sent.
     *
     * @return The page, or null to send the request.
     */
    public static MsgHistory getArchivedHistory(JsonInflater inflater, ConversationRequest request) {
        try {
            String cursor = request.cursor;
            if (cursor == null || !cursor.startsWith(CURSOR_PREFIX)) {
                return null;
            }
            String beforeTs = cursor.substring(CURSOR_PREFIX.length());
            MessageArchive archive = archive();
            MsgHistory page = archive == null ? null : withArchivedMessages(archive, inflater, null,
                    Collections.emptyList(), request.channelId, beforeTs, false, request.oldestTs, limitOf(request));
            if (page != null) {
                return page;
            }
            // Archiving was turned off or the archive was deleted since the previous page.
            return new MsgHistory(true, null, new ArrayList<>(), false, true,
                    null, null, null, null, null);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load archived messages", ex);
            return null;
        }
    }

    /**
     * Injection point.
     *
     * @return A mapping of the response of conversations.history,
     * which adds the archived messages the server hid.
     */
    public static Function getHistoryMapper(JsonInflater inflater, ConversationRequest request) {
        return response -> {
            try {
                return addArchivedMessages(inflater, request, (MsgHistory) response);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to add archived messages", ex);
                return response;
            }
        };
    }

    private static MsgHistory addArchivedMessages(JsonInflater inflater, ConversationRequest request, MsgHistory history) {
        MessageArchive archive = archive();
        if (archive == null || !history.ok || request.channelId == null) {
            return history;
        }
        String channelId = request.channelId;
        List<Object> serverMessages = history.messages != null ? history.messages : Collections.emptyList();

        String oldestServer = oldestTs(serverMessages);
        if (oldestServer != null) {
            synchronized (oldestServerTs) {
                String oldestKnown = oldestServerTs.get(channelId);
                if (oldestKnown == null || oldestServer.compareTo(oldestKnown) < 0) {
                    oldestServerTs.put(channelId, oldestServer);
                }
            }
        }

        if (!history.isLimited) {
            return history;
        }

        // Only a start: the messages right after it, as when Slack shows the messages
        // around one it jumps to. Then has_more tells if there are newer ones.
        if (request.oldestTs != null && request.latestTs == null && request.cursor == null) {
            MsgHistory merged = withArchivedMessagesAfter(archive, inflater, history, serverMessages, channelId,
                    request.oldestTs, request.inclusive, oldestServer, limitOf(request));
            return merged != null ? merged : history;
        }

        // The server can return more.
        if (history.hasMore) {
            return history;
        }

        String beforeTs;
        boolean inclusive;
        if (oldestServer != null) {
            beforeTs = oldestServer;
            inclusive = false;
        } else if (request.latestTs != null) {
            beforeTs = request.latestTs;
            inclusive = request.inclusive;
        } else if (request.cursor != null && decodeServerCursor(request.cursor) != null) {
            beforeTs = decodeServerCursor(request.cursor);
            inclusive = true;
        } else if (request.cursor != null) {
            // The next page of a response whose oldest message is known.
            beforeTs = oldestServerTs.get(channelId);
            inclusive = false;
            if (beforeTs == null) {
                return history;
            }
        } else {
            // The latest messages of a conversation whose messages are all hidden.
            beforeTs = null;
            inclusive = false;
        }

        MsgHistory merged = withArchivedMessages(archive, inflater, history, serverMessages, channelId,
                beforeTs, inclusive, request.oldestTs, Math.max(limitOf(request) - serverMessages.size(), 0));
        return merged != null ? merged : history;
    }

    /**
     * Injection point. Adds the archived messages of a thread the server did not return
     * to the response of conversations.replies, or answers with them if it failed.
     *
     * @param result    {@link ApiResult} of the request, or the marker of a suspended coroutine.
     * @param inclusive If replies at oldestTs and latestTs are included.
     */
    public static Object addArchivedReplies(Object result, JsonInflater inflater, String channelId,
                                            String threadTs, String oldestTs, String latestTs, boolean inclusive) {
        try {
            MessageArchive archive = archive();
            if (archive == null || !(result instanceof ApiResult) || channelId == null || threadTs == null) {
                return result;
            }

            RepliesApiResponse response = null;
            if (result instanceof ApiResult.Success) {
                Object value = ((ApiResult.Success) result).value;
                if (!(value instanceof RepliesApiResponse)) {
                    return result;
                }
                response = (RepliesApiResponse) value;
            }
            List<Object> serverMessages = response != null && response.messages != null
                    ? response.messages : Collections.emptyList();

            // Only add messages older than the ones returned, so messages deleted while the app
            // was not running stay deleted where the server still returns the thread.
            Set<String> serverTs = new HashSet<>();
            for (Object message : serverMessages) {
                serverTs.add(tsOf(message));
            }
            String oldestServer = oldestTs(serverMessages);

            List<String> archivedJson = new ArrayList<>();
            for (String json : archive.thread(channelId, threadTs)) {
                String ts = new JSONObject(json).optString("ts", null);
                if (ts == null || serverTs.contains(ts)
                        || (oldestServer != null && ts.compareTo(oldestServer) >= 0)) {
                    continue;
                }
                // The parent is listed first whatever the range, as the server does.
                if (!ts.equals(threadTs) && (
                        (oldestTs != null && (inclusive ? ts.compareTo(oldestTs) < 0 : ts.compareTo(oldestTs) <= 0))
                        || (latestTs != null && (inclusive ? ts.compareTo(latestTs) > 0 : ts.compareTo(latestTs) >= 0)))) {
                    continue;
                }
                archivedJson.add(json);
            }
            if (archivedJson.isEmpty()) {
                return result;
            }

            List<Object> messages = new ArrayList<>(serverMessages);
            messages.addAll(inflateMessages(inflater, archivedJson));
            // Replies are listed oldest first, after the parent.
            Collections.sort(messages, (a, b) -> {
                String aTs = tsOf(a);
                String bTs = tsOf(b);
                return aTs == null || bTs == null ? 0 : aTs.compareTo(bTs);
            });

            RepliesApiResponse merged = response != null
                    ? new RepliesApiResponse(response.subscribed, response.lastRead, response.unreadCount, messages,
                    response.hasMore, response.responseMetadata, response.latestUpdates, response.unchangedMessages)
                    : new RepliesApiResponse(false, null, 0, messages, false, null, null, null);
            return new ApiResult.Success(merged, Collections.emptyMap());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add archived replies", ex);
            return result;
        }
    }

    // endregion
}
