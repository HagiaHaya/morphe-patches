package app.template.extension.slack;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searches the message archive, which keeps messages the server no longer returns,
 * such as the ones older than 90 days that search of a free workspace leaves out.
 * Opening a result shows the message in Slack, which adds the archived messages to the conversation.
 */
@SuppressWarnings("unused")
public final class MessageArchiveSearchActivity extends Activity {

    private static final String TAG = "MorpheSlack";

    private static final int RESULT_LIMIT = 200;

    private static final long SEARCH_DELAY_MILLIS = 250;

    /**
     * Mentions, channel links, special mentions and links in the text of messages:
     * {@code <@U123>}, {@code <#C123|general>}, {@code <!here>}, {@code <https://example.com|label>}.
     */
    private static final Pattern ENTITY = Pattern.compile("<([@#!]?)([^<>|]+)(?:\\|([^<>]*))?>");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ResultAdapter adapter = new ResultAdapter();

    private EditText query;

    private TextView status;

    private SlackNames names;

    /**
     * Increased by each search, so results of a previous query are dropped.
     */
    private int searchId;

    private final Runnable search = this::search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? android.R.style.Theme_DeviceDefault_DayNight
                : android.R.style.Theme_DeviceDefault_Light);
        super.onCreate(savedInstanceState);

        setTitle("Search message archive");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        // The app is drawn edge to edge, see MessageArchiveActivity.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });

        query = new EditText(this);
        query.setHint("Search archived messages");
        query.setSingleLine(true);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                mainHandler.removeCallbacks(search);
                mainHandler.postDelayed(search, SEARCH_DELAY_MILLIS);
            }
        });
        LinearLayout.LayoutParams queryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        queryParams.setMargins(dp(16), dp(8), dp(16), 0);
        root.addView(query, queryParams);

        status = new TextView(this);
        status.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
        status.setTextColor(color(android.R.attr.textColorSecondary));
        status.setPadding(dp(20), dp(8), dp(20), dp(8));
        root.addView(status);

        ListView results = new ListView(this);
        results.setAdapter(adapter);
        results.setOnItemClickListener((parent, view, position, id) -> open(adapter.getItem(position)));
        root.addView(results, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Dark system bar icons on the light theme.
            boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(night ? 0 : lightBars, lightBars);
        }

        query.requestFocus();
        showCount();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(search);
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCount() {
        MessageArchive archive = MessageArchive.get();
        if (archive == null) {
            status.setText("The archive is not available.");
            return;
        }
        executor.execute(() -> {
            String text;
            try {
                long messages = archive.stats().messages;
                text = messages == 0
                        ? "The archive is empty. Slack archives the messages it loads from now on."
                        : "Searches the " + messages + (messages == 1 ? " archived message." : " archived messages.");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to count the archived messages", ex);
                text = "Failed to read the archive.";
            }
            String result = text;
            mainHandler.post(() -> status.setText(result));
        });
    }

    private void search() {
        String text = query.getText().toString().trim();
        int id = ++searchId;
        if (text.isEmpty()) {
            adapter.setResults(new ArrayList<>(), new String[0]);
            showCount();
            return;
        }
        MessageArchive archive = MessageArchive.get();
        if (archive == null) {
            return;
        }
        String[] words = text.toLowerCase(Locale.ROOT).split("\\s+");
        executor.execute(() -> {
            List<Result> results = new ArrayList<>();
            String summary;
            try {
                if (names == null) {
                    names = SlackNames.load(this);
                }
                for (MessageArchive.Match match : archive.search(text, RESULT_LIMIT)) {
                    results.add(toResult(match));
                }
                summary = results.isEmpty() ? "No archived messages match."
                        : results.size() >= RESULT_LIMIT ? "Showing the newest " + RESULT_LIMIT + " matches."
                        : results.size() + (results.size() == 1 ? " match." : " matches.");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to search the archive", ex);
                summary = "Failed to search the archive.";
            }
            String shownSummary = summary;
            mainHandler.post(() -> {
                if (id != searchId) {
                    return;
                }
                adapter.setResults(results, words);
                status.setText(shownSummary);
            });
        });
    }

    private static final class Result {
        final MessageArchive.Match match;
        final String teamId;
        final String header;
        final String text;

        Result(MessageArchive.Match match, String teamId, String header, String text) {
            this.match = match;
            this.teamId = teamId;
            this.header = header;
            this.text = text;
        }
    }

    private Result toResult(MessageArchive.Match match) {
        String author = null;
        String text = "";
        try {
            JSONObject message = new JSONObject(match.json);
            author = authorOf(message);
            text = displayText(message);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read an archived message", ex);
        }

        StringBuilder header = new StringBuilder(names.conversationName(match.channelId));
        if (!match.inChannel) {
            header.append(" · thread");
        }
        if (author != null) {
            header.append(" · ").append(author);
        }
        header.append(" · ").append(formatTs(match.ts));
        return new Result(match, names.teamIdOf(match.channelId), header.toString(), text);
    }

    private String authorOf(JSONObject message) {
        JSONObject profile = message.optJSONObject("user_profile");
        if (profile != null) {
            for (String key : new String[]{"display_name", "real_name", "name"}) {
                String name = profile.optString(key);
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }
        String user = message.optString("user");
        if (!user.isEmpty()) {
            return names.userName(user);
        }
        String username = message.optString("username");
        if (!username.isEmpty()) {
            return username;
        }
        JSONObject bot = message.optJSONObject("bot_profile");
        return bot != null && !bot.optString("name").isEmpty() ? bot.optString("name") : null;
    }

    /**
     * @return The text of a message as Slack shows it, or the names of its files and attachments.
     */
    private String displayText(JSONObject message) {
        String text = message.optString("text");
        if (text.isEmpty()) {
            List<String> parts = new ArrayList<>();
            JSONArray files = message.optJSONArray("files");
            for (int i = 0; files != null && i < files.length(); i++) {
                JSONObject file = files.optJSONObject(i);
                if (file != null) {
                    parts.add("File: " + (file.optString("title").isEmpty() ? file.optString("name") : file.optString("title")));
                }
            }
            JSONArray attachments = message.optJSONArray("attachments");
            for (int i = 0; attachments != null && i < attachments.length(); i++) {
                JSONObject attachment = attachments.optJSONObject(i);
                if (attachment != null && !attachment.optString("fallback").isEmpty()) {
                    parts.add(attachment.optString("fallback"));
                }
            }
            text = TextUtils.join("\n", parts);
        }

        StringBuffer shown = new StringBuffer();
        Matcher entity = ENTITY.matcher(text);
        while (entity.find()) {
            String kind = entity.group(1);
            String target = entity.group(2);
            String label = entity.group(3);
            String replacement;
            if ("@".equals(kind)) {
                replacement = "@" + (label != null ? label : names.userName(target));
            } else if ("#".equals(kind)) {
                replacement = label != null ? "#" + label : names.conversationName(target);
            } else if ("!".equals(kind)) {
                // <!here>, <!channel>, <!subteam^S123|@team>
                replacement = label != null ? label : "@" + target;
            } else {
                replacement = label != null ? label : target;
            }
            entity.appendReplacement(shown, Matcher.quoteReplacement(replacement));
        }
        entity.appendTail(shown);
        return shown.toString().replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    private static String formatTs(String ts) {
        try {
            long millis = (long) (Double.parseDouble(ts) * 1000);
            return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(millis));
        } catch (Exception ex) {
            return ts;
        }
    }

    /**
     * Opens the message in Slack through its deep link, which also opens replies in their thread.
     */
    private void open(Result result) {
        MessageArchive.Match match = result.match;
        Uri.Builder link = new Uri.Builder().scheme("slack").authority("channel");
        if (result.teamId != null) {
            link.appendQueryParameter("team", result.teamId);
        }
        link.appendQueryParameter("id", match.channelId).appendQueryParameter("ts", match.ts);
        if (!match.inChannel && match.threadTs != null) {
            link.appendQueryParameter("thread_ts", match.threadTs);
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, link.build()).setPackage(getPackageName()));
        } catch (Exception ex) {
            Log.e(TAG, "Failed to open an archived message", ex);
            Toast.makeText(this, "Failed to open the message.", Toast.LENGTH_LONG).show();
        }
    }

    private final class ResultAdapter extends BaseAdapter {
        private List<Result> results = new ArrayList<>();
        private String[] words = new String[0];

        void setResults(List<Result> results, String[] words) {
            this.results = results;
            this.words = words;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public Result getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = (LinearLayout) convertView;
            if (row == null) {
                row = new LinearLayout(MessageArchiveSearchActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(20), dp(12), dp(20), dp(12));

                TextView header = new TextView(MessageArchiveSearchActivity.this);
                header.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSecondary));
                header.setTextColor(color(android.R.attr.textColorSecondary));
                header.setSingleLine(true);
                header.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(header);

                TextView text = new TextView(MessageArchiveSearchActivity.this);
                text.setTextAppearance(attribute(android.R.attr.textAppearanceListItem));
                text.setMaxLines(4);
                text.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(text);
            }
            Result result = getItem(position);
            ((TextView) row.getChildAt(0)).setText(result.header);
            ((TextView) row.getChildAt(1)).setText(highlight(result.text, words));
            return row;
        }
    }

    /**
     * @return The text with the words of the query in bold.
     */
    private static CharSequence highlight(String text, String[] words) {
        SpannableString highlighted = new SpannableString(text);
        String lowerCase = text.toLowerCase(Locale.ROOT);
        // Lower casing can change the length of text, then the positions would not match.
        if (lowerCase.length() != text.length()) {
            return text;
        }
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            for (int index = lowerCase.indexOf(word); index >= 0; index = lowerCase.indexOf(word, index + word.length())) {
                highlighted.setSpan(new StyleSpan(Typeface.BOLD), index, index + word.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return highlighted;
    }

    private int attribute(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId;
    }

    private ColorStateList color(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId != 0
                ? getColorStateList(value.resourceId)
                : ColorStateList.valueOf(value.data);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
