package app.template.extension.slack.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.template.extension.slack.NicknamesPatch;
import app.template.extension.slack.SlackUsers;

/**
 * Lists the nicknames of other members and adds, changes or removes them.
 */
@SuppressWarnings("unused")
public final class NicknamesActivity extends SettingsScreen {

    private List<SlackUsers.User> users;

    private final Map<String, SlackUsers.User> usersById = new HashMap<>();

    /**
     * Index in the list of the first row of a nickname, which are rebuilt after every change.
     */
    private int firstNicknameRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Nicknames");

        users = SlackUsers.load(this);
        for (SlackUsers.User user : users) {
            usersById.put(user.id, user);
        }

        addSwitch(Settings.NICKNAMES, "Show nicknames",
                "Shows the nicknames below instead of the names of the members, only on this device. "
                        + "Restart Slack to update the names already shown.");
        addRow("Add a nickname", "Choose a member and give them a nickname.", null, view -> pickUser());
        addHeader("Nicknames");
        firstNicknameRow = list.getChildCount();
        showNicknames();
    }

    private void showNicknames() {
        list.removeViews(firstNicknameRow, list.getChildCount() - firstNicknameRow);
        Map<String, String> nicknames = NicknamesPatch.getNicknames();
        if (nicknames.isEmpty()) {
            addNote("No nicknames yet.");
            return;
        }
        for (Map.Entry<String, String> entry : nicknames.entrySet()) {
            String userId = entry.getKey();
            addRow(entry.getValue(), describe(userId), null, view -> editNickname(userId));
        }
    }

    private String describe(String userId) {
        SlackUsers.User user = usersById.get(userId);
        if (user == null) {
            return userId;
        }
        boolean showRealName = user.realName != null && !user.realName.isEmpty() && !user.realName.equals(user.name);
        return user.name + (showRealName ? " (" + user.realName + ")" : "") + " · " + userId;
    }

    private void pickUser() {
        if (users.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("Slack has not loaded any members yet. Open a conversation and try again.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        List<SlackUsers.User> shown = new ArrayList<>(users);
        List<String> labels = new ArrayList<>();
        for (SlackUsers.User user : shown) {
            labels.add(label(user));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        EditText search = new EditText(this);
        search.setHint("Search members");
        search.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(dp(20), dp(8), dp(20), dp(8));
        content.addView(search, searchParams);
        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        content.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(400)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose a member")
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                String query = text.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                labels.clear();
                for (SlackUsers.User user : users) {
                    String label = label(user);
                    if (query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query)) {
                        shown.add(user);
                        labels.add(label);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            editNickname(shown.get(position).id);
        });
    }

    private static String label(SlackUsers.User user) {
        boolean showRealName = user.realName != null && !user.realName.isEmpty() && !user.realName.equals(user.name);
        return user.name + (showRealName ? " (" + user.realName + ")" : "");
    }

    private void editNickname(String userId) {
        String current = NicknamesPatch.getNicknames().get(userId);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(current);
        input.setSelection(input.getText().length());
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), 0);
        container.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Nickname of " + describe(userId))
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    NicknamesPatch.setNickname(userId, input.getText().toString());
                    showNicknames();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (current != null) {
            builder.setNeutralButton("Remove", (dialog, which) -> {
                NicknamesPatch.setNickname(userId, null);
                showNicknames();
            });
        }
        AlertDialog dialog = builder.show();
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
