package app.template.extension.slack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copies of the messages Slack has cached, kept in a database of their own.
 * <p>
 * Slack clears the messages it cached for a conversation whenever it syncs the conversation,
 * and deletes its whole database on sign out, so its cache only holds what the server
 * still returns. On a free workspace the server hides messages older than 90 days.
 * <p>
 * Each message is kept as the JSON Slack stores in its own database, so it can be parsed
 * back into a message of the app. Timestamps are compared as text: they have 10 digits
 * of seconds until the year 2286.
 */
final class MessageArchive extends SQLiteOpenHelper {

    private static final String TAG = "MorpheSlack";

    private static final String DATABASE_NAME = "hagiahaya_message_archive.db";

    /**
     * Version 2 adds the text column.
     */
    private static final int DATABASE_VERSION = 2;

    private static final String UPSERT_MESSAGE =
            "INSERT INTO message(channel_id, ts, thread_ts, in_channel, json, archived_at, text)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT(channel_id, ts) DO UPDATE SET"
                    + " thread_ts = excluded.thread_ts,"
                    + " in_channel = max(in_channel, excluded.in_channel),"
                    + " json = excluded.json,"
                    + " archived_at = excluded.archived_at,"
                    + " text = excluded.text";

    private static final String INSERT_MESSAGE_IF_ABSENT =
            "INSERT OR IGNORE INTO message(channel_id, ts, thread_ts, in_channel, json, archived_at, text)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static MessageArchive instance;

    /**
     * Writes run on a thread of their own, as they are copies of writes Slack makes
     * to its database, mostly while it holds a transaction.
     */
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HagiaHayaMessageArchive");
        thread.setDaemon(true);
        return thread;
    });

    private final ConcurrentLinkedQueue<Write> pendingWrites = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean flushScheduled = new AtomicBoolean();

    private MessageArchive(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new MessageArchive(context.getApplicationContext());
        }
    }

    /**
     * @return The archive, or null before the app is created.
     */
    static synchronized MessageArchive get() {
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE message("
                + "channel_id TEXT NOT NULL,"
                + " ts TEXT NOT NULL,"
                // Timestamp of the parent message for replies and for parents with replies.
                + " thread_ts TEXT,"
                // If the message is shown in the conversation, and not only in its thread.
                + " in_channel INTEGER NOT NULL,"
                + " json TEXT NOT NULL,"
                + " archived_at INTEGER NOT NULL,"
                // What the message is searched by, see searchableText.
                + " text TEXT,"
                + " PRIMARY KEY (channel_id, ts)"
                + ") WITHOUT ROWID");
        db.execSQL("CREATE INDEX message_thread ON message(channel_id, thread_ts)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE message ADD COLUMN text TEXT");
            try (Cursor cursor = db.query("message", new String[]{"channel_id", "ts", "json"},
                    null, null, null, null, null)) {
                SQLiteStatement statement = db.compileStatement(
                        "UPDATE message SET text = ? WHERE channel_id = ? AND ts = ?");
                while (cursor.moveToNext()) {
                    statement.bindString(1, searchableText(cursor.getString(2)));
                    statement.bindString(2, cursor.getString(0));
                    statement.bindString(3, cursor.getString(1));
                    statement.executeUpdateDelete();
                }
                statement.close();
            }
        }
    }

    /**
     * @return The text of a message, its attachments and the names of its files, in lower case.
     */
    static String searchableText(String json) {
        StringBuilder text = new StringBuilder();
        try {
            JSONObject message = new JSONObject(json);
            text.append(message.optString("text"));
            JSONArray attachments = message.optJSONArray("attachments");
            for (int i = 0; attachments != null && i < attachments.length(); i++) {
                JSONObject attachment = attachments.optJSONObject(i);
                if (attachment != null) {
                    for (String key : new String[]{"pretext", "title", "text", "fallback"}) {
                        text.append('\n').append(attachment.optString(key));
                    }
                }
            }
            JSONArray files = message.optJSONArray("files");
            for (int i = 0; files != null && i < files.length(); i++) {
                JSONObject file = files.optJSONObject(i);
                if (file != null) {
                    text.append('\n').append(file.optString("title")).append('\n').append(file.optString("name"));
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read the text of a message", ex);
        }
        // Slack escapes these in the text of messages.
        return text.toString().replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .toLowerCase(Locale.ROOT);
    }

    // region Writes

    private interface Write {
        void run(SQLiteDatabase db);
    }

    private void enqueue(Write write) {
        pendingWrites.add(write);
        if (flushScheduled.compareAndSet(false, true)) {
            writer.execute(this::flush);
        }
    }

    /**
     * Runs the queued writes in one transaction, as Slack writes a page of messages at once.
     */
    private void flush() {
        flushScheduled.set(false);
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                Write write;
                while ((write = pendingWrites.poll()) != null) {
                    write.run(db);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to archive messages", ex);
        }
    }

    /**
     * Archives a message, or updates the archived copy.
     *
     * @param inChannel If the message is shown in the conversation, and not only in its thread.
     * @param replace   If an archived copy is replaced, otherwise it is kept.
     */
    void save(String channelId, String ts, String threadTs, boolean inChannel, String json, boolean replace) {
        long archivedAt = System.currentTimeMillis();
        String text = searchableText(json);
        enqueue(db -> {
            SQLiteStatement statement = db.compileStatement(replace ? UPSERT_MESSAGE : INSERT_MESSAGE_IF_ABSENT);
            statement.bindString(1, channelId);
            statement.bindString(2, ts);
            if (threadTs == null) {
                statement.bindNull(3);
            } else {
                statement.bindString(3, threadTs);
            }
            statement.bindLong(4, inChannel ? 1 : 0);
            statement.bindString(5, json);
            statement.bindLong(6, archivedAt);
            statement.bindString(7, text);
            statement.executeInsert();
            statement.close();
        });
    }

    /**
     * Updates the archived copies of a message whose conversation is unknown.
     */
    void update(String ts, String threadTs, String json) {
        long archivedAt = System.currentTimeMillis();
        enqueue(db -> {
            ContentValues values = new ContentValues();
            values.put("thread_ts", threadTs);
            values.put("json", json);
            values.put("archived_at", archivedAt);
            values.put("text", searchableText(json));
            db.update("message", values, "ts = ?", new String[]{ts});
        });
    }

    void delete(String channelId, String ts) {
        enqueue(db -> db.delete("message", "channel_id = ? AND ts = ?", new String[]{channelId, ts}));
    }

    void clear() {
        enqueue(db -> db.delete("message", null, null));
    }

    // endregion

    // region Reads

    /**
     * @return The JSON of the messages of a conversation, newest first.
     */
    List<String> conversationPage(String channelId, String beforeTs, boolean inclusive, String afterTs, int limit) {
        StringBuilder selection = new StringBuilder("channel_id = ? AND in_channel = 1");
        List<String> args = new ArrayList<>();
        args.add(channelId);
        if (beforeTs != null) {
            selection.append(inclusive ? " AND ts <= ?" : " AND ts < ?");
            args.add(beforeTs);
        }
        if (afterTs != null) {
            selection.append(" AND ts > ?");
            args.add(afterTs);
        }
        return queryJson(selection.toString(), args, "ts DESC", String.valueOf(limit));
    }

    /**
     * @return The JSON of the messages of a conversation after a timestamp, oldest first.
     */
    List<String> conversationPageAfter(String channelId, String afterTs, boolean inclusive, String beforeTs, int limit) {
        StringBuilder selection = new StringBuilder("channel_id = ? AND in_channel = 1");
        List<String> args = new ArrayList<>();
        args.add(channelId);
        selection.append(inclusive ? " AND ts >= ?" : " AND ts > ?");
        args.add(afterTs);
        if (beforeTs != null) {
            selection.append(" AND ts < ?");
            args.add(beforeTs);
        }
        return queryJson(selection.toString(), args, "ts ASC", String.valueOf(limit));
    }

    /**
     * @return The JSON of the parent and the replies of a thread, oldest first.
     */
    /**
     * @return The JSON of a message, or null if it is not archived.
     */
    String message(String channelId, String ts) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT json FROM message WHERE channel_id = ? AND ts = ?", new String[]{channelId, ts})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    List<String> thread(String channelId, String threadTs) {
        List<String> args = new ArrayList<>();
        args.add(channelId);
        args.add(threadTs);
        args.add(threadTs);
        return queryJson("channel_id = ? AND (thread_ts = ? OR ts = ?)", args, "ts ASC", null);
    }

    private List<String> queryJson(String selection, List<String> args, String orderBy, String limit) {
        List<String> json = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("message", new String[]{"json"}, selection,
                args.toArray(new String[0]), null, null, orderBy, limit)) {
            while (cursor.moveToNext()) {
                json.add(cursor.getString(0));
            }
        }
        return json;
    }

    static final class Match {
        final String channelId;
        final String ts;
        /**
         * Timestamp of the parent message, or null if the message is not in a thread.
         */
        final String threadTs;
        /**
         * If the message is shown in the conversation, and not only in its thread.
         */
        final boolean inChannel;
        final String json;

        Match(String channelId, String ts, String threadTs, boolean inChannel, String json) {
            this.channelId = channelId;
            this.ts = ts;
            this.threadTs = threadTs;
            this.inChannel = inChannel;
            this.json = json;
        }
    }

    /**
     * @return The messages whose text contains all words of the query, newest first.
     */
    List<Match> search(String query, int limit) {
        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append("text LIKE ? ESCAPE '\\'");
            args.add("%" + word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        List<Match> matches = new ArrayList<>();
        if (args.isEmpty()) {
            return matches;
        }
        try (Cursor cursor = getReadableDatabase().query("message",
                new String[]{"channel_id", "ts", "thread_ts", "in_channel", "json"},
                selection.toString(), args.toArray(new String[0]), null, null, "ts DESC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                matches.add(new Match(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getInt(3) != 0, cursor.getString(4)));
            }
        }
        return matches;
    }

    static final class Stats {
        final long messages;
        final long conversations;
        /**
         * Timestamp of the oldest message, or null if there are none.
         */
        final String oldestTs;

        Stats(long messages, long conversations, String oldestTs) {
            this.messages = messages;
            this.conversations = conversations;
            this.oldestTs = oldestTs;
        }
    }

    Stats stats() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT count(*), count(DISTINCT channel_id), min(ts) FROM message", null)) {
            cursor.moveToFirst();
            return new Stats(cursor.getLong(0), cursor.getLong(1), cursor.getString(2));
        }
    }

    /**
     * Writes all archived messages as a JSON object that lists the messages
     * of each conversation, keyed by conversation ID, oldest first.
     */
    void export(OutputStream output) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.write("{\"exported_at\":" + System.currentTimeMillis() / 1000 + ",\"conversations\":{");
        try (Cursor cursor = getReadableDatabase().query("message", new String[]{"channel_id", "json"},
                null, null, null, null, "channel_id, ts")) {
            String channelId = null;
            while (cursor.moveToNext()) {
                String messageChannelId = cursor.getString(0);
                if (messageChannelId.equals(channelId)) {
                    writer.write(",");
                } else {
                    if (channelId != null) {
                        writer.write("],");
                    }
                    channelId = messageChannelId;
                    writer.write("\n" + JSONObject.quote(channelId) + ":[");
                }
                writer.write("\n");
                // Stored as JSON already.
                writer.write(cursor.getString(1));
            }
            if (channelId != null) {
                writer.write("]");
            }
        }
        writer.write("}}\n");
        writer.flush();
    }

    // endregion
}
