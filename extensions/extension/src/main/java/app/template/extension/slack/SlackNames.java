package app.template.extension.slack;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Names of the conversations and users Slack has cached, read from its databases,
 * one per signed in workspace, named after the ID of the workspace.
 * Names that can not be read fall back to the IDs.
 */
public final class SlackNames {

    private static final String TAG = "MorpheSlack";

    private static final String DATABASE_PREFIX = "org_";

    private static final class Conversation {
        final String teamId;
        /**
         * Name of a channel, or the ID of the other user of a direct message.
         */
        final String nameOrUser;
        final String type;

        Conversation(String teamId, String nameOrUser, String type) {
            this.teamId = teamId;
            this.nameOrUser = nameOrUser;
            this.type = type;
        }
    }

    private final Map<String, Conversation> conversations = new HashMap<>();

    private final Map<String, String> users = new HashMap<>();

    private SlackNames() {
    }

    /**
     * Reads the names. Slack keeps writing its databases meanwhile, so they are opened read only.
     */
    public static SlackNames load(Context context) {
        SlackNames names = new SlackNames();
        File[] files = context.getDatabasePath(DATABASE_PREFIX).getParentFile().listFiles();
        if (files == null) {
            return names;
        }
        for (File file : files) {
            String name = file.getName();
            // Skip the journals of the databases.
            if (!name.startsWith(DATABASE_PREFIX) || name.contains("-")) {
                continue;
            }
            String teamId = name.substring(DATABASE_PREFIX.length());
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
                names.readConversations(db, teamId);
                names.readUsers(db);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to read the names of " + name, ex);
            }
        }
        return names;
    }

    private void readConversations(SQLiteDatabase db, String teamId) {
        try (Cursor cursor = db.rawQuery("SELECT conversation_id, name_or_user, type FROM conversation", null)) {
            while (cursor.moveToNext()) {
                conversations.put(cursor.getString(0),
                        new Conversation(teamId, cursor.getString(1), cursor.getString(2)));
            }
        }
    }

    private void readUsers(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery(
                "SELECT id, profile_display_name, profile_real_name, name FROM users", null)) {
            while (cursor.moveToNext()) {
                for (int column = 1; column <= 3; column++) {
                    String name = cursor.getString(column);
                    if (name != null && !name.isEmpty()) {
                        users.put(cursor.getString(0), name);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @return The ID of the workspace whose database lists the conversation, or null.
     */
    public String teamIdOf(String conversationId) {
        Conversation conversation = conversations.get(conversationId);
        return conversation != null ? conversation.teamId : null;
    }

    /**
     * @return "#channel" for channels and the name of the other user for direct messages.
     */
    public String conversationName(String conversationId) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null || conversation.nameOrUser == null) {
            return conversationId;
        }
        if ("DM".equals(conversation.type)) {
            return userName(conversation.nameOrUser);
        }
        if ("MPDM".equals(conversation.type)) {
            // Group conversations are named after their members: mpdm-anna--bob--carol-1
            String members = conversation.nameOrUser.replaceFirst("^mpdm-", "").replaceFirst("-\\d+$", "");
            return members.replace("--", ", ");
        }
        return "#" + conversation.nameOrUser;
    }

    public String userName(String userId) {
        String name = users.get(userId);
        return name != null ? name : userId;
    }
}
