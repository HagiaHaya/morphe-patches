package app.template.extension.slack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The messages others deleted and the earlier versions of edited messages, as the app saw them,
 * kept in a database of their own. Timestamps of messages are compared as text, like Slack does.
 */
public final class MessageLog extends SQLiteOpenHelper {

    private static final String TAG = "MorpheSlack";

    private static final String DATABASE_NAME = "hagiahaya_message_log.db";

    private static final int DATABASE_VERSION = 1;

    private static MessageLog instance;

    /**
     * Keys of the messages with an entry in the log, so most messages are decorated
     * without reading the database.
     */
    private final Set<String> loggedKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * A deleted message, as it was before it was deleted.
     */
    public static final class Deleted {
        public final String channelId;
        public final String ts;
        public final String threadTs;
        public final String userId;
        public final String text;
        public final String json;
        public final long deletedAt;

        Deleted(String channelId, String ts, String threadTs, String userId, String text, String json, long deletedAt) {
            this.channelId = channelId;
            this.ts = ts;
            this.threadTs = threadTs;
            this.userId = userId;
            this.text = text;
            this.json = json;
            this.deletedAt = deletedAt;
        }
    }

    /**
     * The text of a message before an edit.
     */
    public static final class Edit {
        public final String channelId;
        public final String ts;
        public final String threadTs;
        public final String userId;
        public final String previousText;
        public final long editedAt;

        Edit(String channelId, String ts, String threadTs, String userId, String previousText, long editedAt) {
            this.channelId = channelId;
            this.ts = ts;
            this.threadTs = threadTs;
            this.userId = userId;
            this.previousText = previousText;
            this.editedAt = editedAt;
        }
    }

    private MessageLog(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new MessageLog(context.getApplicationContext());
            instance.loadKeys();
        }
    }

    /**
     * @return The log, or null before the app is created.
     */
    public static synchronized MessageLog get() {
        return instance;
    }

    private static String key(String channelId, String ts) {
        return channelId + '/' + ts;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE deleted ("
                + "channel_id TEXT NOT NULL, ts TEXT NOT NULL, thread_ts TEXT, user_id TEXT, text TEXT,"
                + " json TEXT NOT NULL, deleted_at INTEGER NOT NULL, PRIMARY KEY (channel_id, ts))");
        db.execSQL("CREATE TABLE edit ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, channel_id TEXT NOT NULL, ts TEXT NOT NULL,"
                + " thread_ts TEXT, user_id TEXT, previous_text TEXT NOT NULL, edited_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX edit_message ON edit (channel_id, ts)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void loadKeys() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            for (String table : new String[]{"deleted", "edit"}) {
                try (Cursor cursor = db.rawQuery("SELECT DISTINCT channel_id, ts FROM " + table, null)) {
                    while (cursor.moveToNext()) {
                        loggedKeys.add(key(cursor.getString(0), cursor.getString(1)));
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read the message log", ex);
        }
    }

    boolean isLogged(String channelId, String ts) {
        return loggedKeys.contains(key(channelId, ts));
    }

    boolean hasEntries() {
        return !loggedKeys.isEmpty();
    }

    synchronized void saveDeleted(String channelId, String ts, String threadTs, String userId, String text,
                                  String json, long deletedAt) {
        ContentValues values = new ContentValues();
        values.put("channel_id", channelId);
        values.put("ts", ts);
        values.put("thread_ts", threadTs);
        values.put("user_id", userId);
        values.put("text", text);
        values.put("json", json);
        values.put("deleted_at", deletedAt);
        getWritableDatabase().insertWithOnConflict("deleted", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        loggedKeys.add(key(channelId, ts));
    }

    synchronized void saveEdit(String channelId, String ts, String threadTs, String userId, String previousText,
                               long editedAt) {
        ContentValues values = new ContentValues();
        values.put("channel_id", channelId);
        values.put("ts", ts);
        values.put("thread_ts", threadTs);
        values.put("user_id", userId);
        values.put("previous_text", previousText);
        values.put("edited_at", editedAt);
        getWritableDatabase().insert("edit", null, values);
        loggedKeys.add(key(channelId, ts));
    }

    /**
     * @return When the message was deleted, or null if it was not.
     */
    Long deletedAt(String channelId, String ts) {
        if (!isLogged(channelId, ts)) {
            return null;
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT deleted_at FROM deleted WHERE channel_id = ? AND ts = ?", new String[]{channelId, ts})) {
            return cursor.moveToFirst() ? cursor.getLong(0) : null;
        }
    }

    /**
     * @return The earlier versions of the message, oldest first.
     */
    List<Edit> edits(String channelId, String ts) {
        List<Edit> edits = new ArrayList<>();
        if (!isLogged(channelId, ts)) {
            return edits;
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT channel_id, ts, thread_ts, user_id, previous_text, edited_at FROM edit"
                        + " WHERE channel_id = ? AND ts = ? ORDER BY edited_at", new String[]{channelId, ts})) {
            while (cursor.moveToNext()) {
                edits.add(readEdit(cursor));
            }
        }
        return edits;
    }

    /**
     * @param threadTs The thread whose replies are listed, or null for the messages of the conversation,
     *                 which leaves out the replies.
     * @return The messages deleted from the conversation or thread with a timestamp in the range,
     * inclusive, by timestamp.
     */
    Map<String, Deleted> deletedBetween(String channelId, String threadTs, String fromTs, String toTs) {
        Map<String, Deleted> deleted = new HashMap<>();
        String thread = threadTs == null
                ? " AND (thread_ts IS NULL OR thread_ts = ts)"
                : " AND thread_ts = ?";
        List<String> arguments = new ArrayList<>(Arrays.asList(channelId, fromTs, toTs));
        if (threadTs != null) {
            arguments.add(threadTs);
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT channel_id, ts, thread_ts, user_id, text, json, deleted_at FROM deleted"
                        + " WHERE channel_id = ? AND ts >= ? AND ts <= ?" + thread,
                arguments.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                Deleted message = readDeleted(cursor);
                deleted.put(message.ts, message);
            }
        }
        return deleted;
    }

    /**
     * @return The most recently deleted messages, newest first.
     */
    public List<Deleted> recentDeleted(int limit) {
        List<Deleted> deleted = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT channel_id, ts, thread_ts, user_id, text, json, deleted_at FROM deleted"
                        + " ORDER BY deleted_at DESC LIMIT " + limit, null)) {
            while (cursor.moveToNext()) {
                deleted.add(readDeleted(cursor));
            }
        }
        return deleted;
    }

    /**
     * @return The most recent edits, newest first.
     */
    public List<Edit> recentEdits(int limit) {
        List<Edit> edits = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT channel_id, ts, thread_ts, user_id, previous_text, edited_at FROM edit"
                        + " ORDER BY edited_at DESC LIMIT " + limit, null)) {
            while (cursor.moveToNext()) {
                edits.add(readEdit(cursor));
            }
        }
        return edits;
    }

    public synchronized int count(String table) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT count(*) FROM " + table, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public synchronized void clear() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("deleted", null, null);
        db.delete("edit", null, null);
        loggedKeys.clear();
    }

    private static Deleted readDeleted(Cursor cursor) {
        return new Deleted(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.getString(4), cursor.getString(5), cursor.getLong(6));
    }

    private static Edit readEdit(Cursor cursor) {
        return new Edit(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.getString(4), cursor.getLong(5));
    }
}
