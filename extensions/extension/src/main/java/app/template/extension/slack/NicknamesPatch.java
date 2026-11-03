package app.template.extension.slack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import app.template.extension.slack.settings.Settings;
import slack.model.Member;

/**
 * Shows nicknames chosen by the user instead of the names of other members,
 * only on this device.
 */
@SuppressWarnings("unused")
public final class NicknamesPatch {

    private static final String TAG = "MorpheSlack";

    private static final String PREFERENCES_NAME = "hagiahaya_nicknames";

    private static volatile Map<String, String> nicknames;

    private static SharedPreferences preferences() {
        Context context = Settings.getContext();
        return context == null ? null : context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static Map<String, String> nicknames() {
        Map<String, String> loaded = nicknames;
        if (loaded != null) {
            return loaded;
        }
        SharedPreferences preferences = preferences();
        if (preferences == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new ConcurrentHashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getValue() instanceof String) {
                map.put(entry.getKey(), (String) entry.getValue());
            }
        }
        nicknames = map;
        return map;
    }

    /**
     * Injection point. Called whenever Slack resolves the names it shows for a member.
     *
     * @return The nickname of the member, which Slack shows instead of the main name,
     * keeping the main name as the secondary one, or null if the member has none.
     */
    public static String getNickname(Member member) {
        try {
            if (member == null || !Settings.isEnabled(Settings.NICKNAMES)) {
                return null;
            }
            Map<String, String> map = nicknames();
            return map.isEmpty() ? null : map.get(member.getId());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get a nickname", ex);
            return null;
        }
    }

    /**
     * @return The nicknames by user ID, sorted by ID.
     */
    public static Map<String, String> getNicknames() {
        return new TreeMap<>(nicknames());
    }

    /**
     * @param nickname The nickname, or null or empty to remove it.
     */
    public static void setNickname(String userId, String nickname) {
        SharedPreferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        Map<String, String> map = nicknames();
        if (nickname == null || nickname.trim().isEmpty()) {
            map.remove(userId);
            preferences.edit().remove(userId).apply();
        } else {
            map.put(userId, nickname.trim());
            preferences.edit().putString(userId, nickname.trim()).apply();
        }
    }
}
