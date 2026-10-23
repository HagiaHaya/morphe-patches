package app.template.extension.slack;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.slack.eithernet.ApiResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.template.extension.slack.settings.PatchedFeatures;
import app.template.extension.slack.settings.Settings;
import io.reactivex.rxjava3.functions.Function;
import slack.commons.json.JsonInflater;
import slack.foundation.auth.LoggedInUser;
import slack.messages.impl.MessagePersistenceHelperImpl;
import slack.model.Message;
import slack.model.MessageState;
import slack.model.PersistedMessageObj;
import slack.services.api.conversations.ConversationRequest;
import slack.services.api.conversations.MsgHistory;
import slack.services.api.conversations.RepliesApiResponse;

/**
 * Keeps the messages others delete and the earlier versions of messages others edit in {@link MessageLog},
 * and shows them below the messages as attachments with a colored bar, like Slack shows link previews:
 * red for a deleted message and yellow for each earlier version.
 * <p>
 * Deleted messages stay where they were: Slack keeps the copy it cached, and they are added back
 * to the pages of history the server returns without them.
 */
@SuppressWarnings("unused")
// Slack requires Android 12L.
@TargetApi(Build.VERSION_CODES.S_V2)
public final class MessageLoggerPatch {

    private static final String TAG = "MorpheSlack";

    /**
     * Marks the attachments added, so they are replaced instead of added again.
     */
    private static final String CALLBACK_ID = "hagiahaya_message_log";

    private static final String DELETED_COLOR = "E01E5A";

    private static final String EDITED_COLOR = "ECB22E";

    /**
     * IDs of the attachments added, above the ones of Slack.
     */
    private static final int FIRST_ATTACHMENT_ID = 900_000;

    private static final String LATEST_TS = "9999999999.999999";

    private static volatile JsonInflater jsonInflater;

    /**
     * Injection point. Called when Slack creates its JSON parser.
     */
    public static void onJsonInflaterCreated(JsonInflater inflater) {
        jsonInflater = inflater;
    }

    /**
     * Replaced by the patch with the creation of the state of synced messages,
     * which has no constructor of its own.
     */
    private static MessageState newSyncedState() {
        return null;
    }

    private static boolean keepDeleted() {
        return Settings.isEnabled(Settings.KEEP_DELETED_MESSAGES);
    }

    private static boolean keepEdits() {
        return Settings.isEnabled(Settings.KEEP_EDIT_HISTORY);
    }

    /**
     * @return If the message was kept after it was deleted, so the message archive keeps it too.
     */
    static boolean isLoggedAsDeleted(String channelId, String ts) {
        try {
            MessageLog log = PatchedFeatures.messageLogger() && keepDeleted() ? log() : null;
            return log != null && channelId != null && ts != null && log.deletedAt(channelId, ts) != null;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read the message log", ex);
            return false;
        }
    }

    /**
     * @return If the message is from the user signed in to the workspace, whose own deletions
     * and edits are not logged.
     */
    private static boolean isOwn(MessagePersistenceHelperImpl helper, String userId) {
        if (userId == null) {
            return false;
        }
        try {
            return userId.equals(((LoggedInUser) helper.loggedInUserLazy.get()).user);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get the user signed in", ex);
            return false;
        }
    }

    private static MessageLog log() {
        MessageLog log = MessageLog.get();
        if (log == null) {
            Context context = Settings.getContext();
            if (context != null) {
                MessageLog.initialize(context);
                log = MessageLog.get();
            }
        }
        return log;
    }

    // region Logging

    /**
     * Injection point. Called when someone deleted a message, before Slack removes it.
     *
     * @return If the message is kept, marked as deleted, instead of being removed.
     */
    public static boolean keepDeletedMessage(MessagePersistenceHelperImpl helper, String channelId, String ts) {
        try {
            JsonInflater inflater = jsonInflater;
            MessageLog log = log();
            if (!keepDeleted() || inflater == null || log == null || channelId == null || ts == null) {
                return false;
            }

            PersistedMessageObj persisted = (PersistedMessageObj) helper.getMessage(channelId, ts).orElse(null);
            String json;
            MessageState state;
            if (persisted != null) {
                json = inflater.deflate(Message.class, persisted.getModelObj());
                state = persisted.msgState;
            } else {
                // Slack caches the replies of threads elsewhere, but the archive may have them.
                MessageArchive archive = MessageArchive.get();
                json = archive != null ? archive.message(channelId, ts) : null;
                state = newSyncedState();
            }
            if (json == null) {
                return false;
            }

            JSONObject message = new JSONObject(json);
            if (isOwn(helper, message.optString("user", null))) {
                return false;
            }
            log.saveDeleted(channelId, ts, message.optString("thread_ts", null), message.optString("user", null),
                    message.optString("text", null), withoutDecoration(message).toString(), System.currentTimeMillis());

            // The update decorates the message as deleted, and shows it on the screens showing it.
            helper.updateMessage(channelId, (Message) inflater.inflate(Message.class, json), state, true);
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to keep a deleted message", ex);
            return false;
        }
    }

    /**
     * Injection point. Called when Slack replaces a cached message, such as when it is edited.
     *
     * @return The message, decorated with its earlier versions if it was edited,
     * and as deleted if it was deleted.
     */
    public static Message onMessageUpdate(MessagePersistenceHelperImpl helper, String channelId, Message message) {
        try {
            JsonInflater inflater = jsonInflater;
            MessageLog log = log();
            if (inflater == null || log == null || message == null || channelId == null || message.ts == null) {
                return message;
            }

            if (keepEdits() && message.text != null && !isOwn(helper, message.user)) {
                PersistedMessageObj persisted = (PersistedMessageObj) helper.getMessage(channelId, message.ts).orElse(null);
                Message previous = persisted != null ? (Message) persisted.getModelObj() : null;
                if (previous != null && previous.text != null && !previous.text.equals(message.text)) {
                    log.saveEdit(channelId, message.ts, message.threadTs, message.user, previous.text,
                            System.currentTimeMillis());
                }
            }

            if (!log.isLogged(channelId, message.ts)) {
                return message;
            }
            Message decorated = decorate(inflater, log, channelId, message);
            return decorated != null ? decorated : message;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to log a message update", ex);
            return message;
        }
    }

    // endregion

    // region Decorating

    private static JSONObject withoutDecoration(JSONObject message) throws JSONException {
        JSONArray attachments = message.optJSONArray("attachments");
        if (attachments == null) {
            return message;
        }
        JSONArray kept = new JSONArray();
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject attachment = attachments.optJSONObject(i);
            if (attachment == null || !CALLBACK_ID.equals(attachment.optString("callback_id"))) {
                kept.put(attachments.get(i));
            }
        }
        if (kept.length() == 0) {
            message.remove("attachments");
        } else {
            message.put("attachments", kept);
        }
        return message;
    }

    private static String formatTime(long time) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(time));
    }

    private static JSONObject attachment(int index, String color, String text, String footer, String fallback)
            throws JSONException {
        return new JSONObject()
                .put("id", FIRST_ATTACHMENT_ID + index)
                .put("callback_id", CALLBACK_ID)
                .put("color", color)
                .put("text", text)
                .put("footer", footer)
                .put("fallback", fallback)
                .put("mrkdwn_in", new JSONArray().put("text"));
    }

    /**
     * Adds the earlier versions and the deletion of a message from the log, replacing the ones added before.
     *
     * @return The decorated JSON, or null if the log has nothing to add.
     */
    private static JSONObject decorate(MessageLog log, String channelId, JSONObject message) throws JSONException {
        String ts = message.optString("ts", null);
        if (ts == null || !log.isLogged(channelId, ts)) {
            return null;
        }
        List<MessageLog.Edit> edits = keepEdits() ? log.edits(channelId, ts) : Collections.emptyList();
        Long deletedAt = keepDeleted() ? log.deletedAt(channelId, ts) : null;
        if (edits.isEmpty() && deletedAt == null) {
            return null;
        }

        withoutDecoration(message);
        JSONArray attachments = message.optJSONArray("attachments");
        if (attachments == null) {
            attachments = new JSONArray();
        }
        int index = 0;
        for (MessageLog.Edit edit : edits) {
            attachments.put(attachment(index++, EDITED_COLOR, edit.previousText,
                    "Before the edit on " + formatTime(edit.editedAt), "Before the edit: " + edit.previousText));
        }
        if (deletedAt != null) {
            attachments.put(attachment(index, DELETED_COLOR, "_This message was deleted._",
                    "Deleted on " + formatTime(deletedAt), "Deleted"));
        }
        message.put("attachments", attachments);
        return message;
    }

    private static Message decorate(JsonInflater inflater, MessageLog log, String channelId, Object message)
            throws JSONException {
        JSONObject decorated = decorate(log, channelId, new JSONObject(inflater.deflate(Message.class, message)));
        return decorated != null ? (Message) inflater.inflate(Message.class, decorated.toString()) : null;
    }

    // endregion

    // region Showing in the history

    private static String tsOf(Object message) {
        return message instanceof Message ? ((Message) message).ts : null;
    }

    /**
     * Decorates the messages of a page, and adds the deleted messages in the range of timestamps.
     *
     * @return The messages, newest first if not ascending, or null if nothing changed.
     */
    private static List<Object> addToPage(JsonInflater inflater, MessageLog log, String channelId, String threadTs,
                                          List<Object> messages, String fromTs, String toTs,
                                          Set<String> excluded, boolean ascending) throws JSONException {
        boolean changed = false;
        List<Object> result = new ArrayList<>(messages.size());
        Set<String> timestamps = new HashSet<>();
        for (Object message : messages) {
            String ts = tsOf(message);
            timestamps.add(ts);
            Message decorated = ts != null && log.isLogged(channelId, ts)
                    ? decorate(inflater, log, channelId, message) : null;
            result.add(decorated != null ? decorated : message);
            changed |= decorated != null;
        }

        if (keepDeleted() && fromTs != null && toTs != null) {
            for (Map.Entry<String, MessageLog.Deleted> entry
                    : log.deletedBetween(channelId, threadTs, fromTs, toTs).entrySet()) {
                String ts = entry.getKey();
                if (timestamps.contains(ts) || excluded.contains(ts)) {
                    continue;
                }
                JSONObject decorated = decorate(log, channelId, new JSONObject(entry.getValue().json));
                if (decorated != null) {
                    result.add(inflater.inflate(Message.class, decorated.toString()));
                    changed = true;
                }
            }
            if (changed) {
                Collections.sort(result, (a, b) -> {
                    String aTs = tsOf(a);
                    String bTs = tsOf(b);
                    int order = aTs == null || bTs == null ? 0 : aTs.compareTo(bTs);
                    return ascending ? order : -order;
                });
            }
        }
        return changed ? result : null;
    }

    /**
     * @return The timestamps a request leaves out: its bounds, unless they are inclusive.
     */
    private static Set<String> excludedBounds(String oldestTs, String latestTs, boolean inclusive) {
        Set<String> excluded = new HashSet<>();
        if (!inclusive) {
            excluded.add(oldestTs);
            excluded.add(latestTs);
        }
        return excluded;
    }

    /**
     * Injection point.
     *
     * @return A mapping of the response of conversations.history, which adds the messages from the log.
     */
    public static Function getHistoryMapper(ConversationRequest request) {
        return response -> {
            try {
                return addToHistory(request, (MsgHistory) response);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to add logged messages", ex);
                return response;
            }
        };
    }

    private static MsgHistory addToHistory(ConversationRequest request, MsgHistory history) throws JSONException {
        JsonInflater inflater = jsonInflater;
        MessageLog log = log();
        if (inflater == null || log == null || !log.hasEntries() || !history.ok || request.channelId == null) {
            return history;
        }
        List<Object> messages = history.messages != null ? history.messages : Collections.emptyList();

        String oldest = null;
        String newest = null;
        for (Object message : messages) {
            String ts = tsOf(message);
            if (ts != null) {
                oldest = oldest == null || ts.compareTo(oldest) < 0 ? ts : oldest;
                newest = newest == null || ts.compareTo(newest) > 0 ? ts : newest;
            }
        }

        // The range of timestamps the page covers, so a deleted message is added to one page only.
        String fromTs;
        String toTs;
        if (request.oldestTs != null && request.latestTs == null && request.cursor == null) {
            // The messages right after a timestamp, as when Slack opens a conversation at a message.
            fromTs = request.oldestTs;
            toTs = history.hasMore ? newest : LATEST_TS;
        } else {
            toTs = request.latestTs != null ? request.latestTs : request.cursor == null ? LATEST_TS : newest;
            fromTs = history.hasMore ? oldest : request.oldestTs != null ? request.oldestTs : "0";
        }

        List<Object> result = addToPage(inflater, log, request.channelId, null, messages, fromTs, toTs,
                excludedBounds(request.oldestTs, request.latestTs, request.inclusive), false);
        if (result == null) {
            return history;
        }

        // Slack deletes the cached messages the server lists as deleted.
        List<String> deleted = null;
        if (history.deleted != null) {
            deleted = new ArrayList<>();
            for (String ts : history.deleted) {
                if (!log.isLogged(request.channelId, ts)) {
                    deleted.add(ts);
                }
            }
        }

        return new MsgHistory(history.ok, history.error, result, history.hasMore, history.isLimited, deleted,
                history.metadata, history.mutationTimestamps, history.latestUpdates, history.unchangedMessages);
    }

    /**
     * Injection point. Adds the messages from the log to the response of conversations.replies.
     *
     * @param result    {@link ApiResult} of the request, or the marker of a suspended coroutine.
     * @param inclusive If replies at oldestTs and latestTs are included.
     */
    public static Object addLoggedReplies(Object result, String channelId, String threadTs, String oldestTs,
                                          String latestTs, boolean inclusive) {
        try {
            JsonInflater inflater = jsonInflater;
            MessageLog log = log();
            if (inflater == null || log == null || !log.hasEntries() || !(result instanceof ApiResult.Success)
                    || channelId == null || threadTs == null) {
                return result;
            }
            Object value = ((ApiResult.Success) result).value;
            if (!(value instanceof RepliesApiResponse)) {
                return result;
            }
            RepliesApiResponse response = (RepliesApiResponse) value;
            List<Object> messages = response.messages != null ? response.messages : Collections.emptyList();

            // Replies are listed oldest first, after the parent.
            String newest = null;
            for (Object message : messages) {
                String ts = tsOf(message);
                if (ts != null && !ts.equals(threadTs)) {
                    newest = newest == null || ts.compareTo(newest) > 0 ? ts : newest;
                }
            }
            String fromTs = oldestTs != null ? oldestTs : threadTs;
            String toTs = latestTs != null ? latestTs : response.hasMore && newest != null ? newest : LATEST_TS;
            List<Object> merged = addToPage(inflater, log, channelId, threadTs, messages, fromTs, toTs,
                    excludedBounds(oldestTs, latestTs, inclusive), true);
            if (merged == null) {
                return result;
            }
            RepliesApiResponse withLog = new RepliesApiResponse(response.subscribed, response.lastRead,
                    response.unreadCount, merged, response.hasMore, response.responseMetadata,
                    response.latestUpdates, response.unchangedMessages);
            return new ApiResult.Success(withLog, Collections.emptyMap());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to add logged replies", ex);
            return result;
        }
    }

    // endregion
}
