package app.template.extension.slack;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The users Slack has cached, read from its databases, one per signed in workspace.
 */
public final class SlackUsers {

    private static final String TAG = "MorpheSlack";

    private static final String DATABASE_PREFIX = "org_";

    public static final class User {
        public final String id;
        /**
         * The name Slack shows: the display name, or the full name if there is none.
         */
        public final String name;
        public final String realName;

        User(String id, String name, String realName) {
            this.id = id;
            this.name = name;
            this.realName = realName;
        }
    }

    private SlackUsers() {
    }

    /**
     * Reads the users, sorted by name. Slack keeps writing its databases meanwhile,
     * so they are opened read only.
     */
    public static List<User> load(Context context) {
        List<User> users = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        File[] files = context.getDatabasePath(DATABASE_PREFIX).getParentFile().listFiles();
        if (files == null) {
            return users;
        }
        for (File file : files) {
            String name = file.getName();
            // Skip the journals of the databases.
            if (!name.startsWith(DATABASE_PREFIX) || name.contains("-")) {
                continue;
            }
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                 Cursor cursor = db.rawQuery(
                         "SELECT id, profile_display_name, profile_real_name, name FROM users", null)) {
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    if (id == null || !ids.add(id)) {
                        continue;
                    }
                    String displayName = cursor.getString(1);
                    String realName = cursor.getString(2);
                    String userName = cursor.getString(3);
                    String shown = !isEmpty(displayName) ? displayName : !isEmpty(realName) ? realName : userName;
                    users.add(new User(id, isEmpty(shown) ? id : shown, realName));
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to read the users of " + name, ex);
            }
        }
        Collections.sort(users, (a, b) -> a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT)));
        return users;
    }

    private static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }
}
