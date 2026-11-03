package app.template.extension.pepper;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import app.template.extension.pepper.api.PepperApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static app.template.extension.pepper.DealMenu.text;

/**
 * Loads all comments of a deal, including the replies, and searches them as the user types.
 * Built from framework views, because the AndroidX classes of the app are obfuscated.
 */
@SuppressWarnings("unused")
public final class CommentSearchActivity extends Activity {

    static final String EXTRA_THREAD_ID = "hagiahaya:thread_id";

    private static final String TAG = "MorphePepper";

    private static final int PAGE_SIZE = 25;

    private static final int MAX_PAGES = 100;

    private static final int MAX_RESULTS = 200;

    /**
     * The representation of the comments the app requests, which includes their plain text.
     */
    private static final String JSON_FORMAT = "user=full,badge=user,formatted_text=html,message=with_code";

    private static final class Comment {
        final String author;
        final long postedMillis;
        final String text;
        final String searchableText;
        final String permalink;

        Comment(String author, long postedMillis, String text, String permalink) {
            this.author = author;
            this.postedMillis = postedMillis;
            this.text = text;
            this.searchableText = searchable(author + "\n" + text);
            this.permalink = permalink;
        }
    }

    private final List<Comment> comments = new ArrayList<>();

    private volatile boolean destroyed;

    private boolean loading = true;

    private String error;

    private EditText query;

    private TextView status;

    private LinearLayout results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? android.R.style.Theme_DeviceDefault_DayNight
                : android.R.style.Theme_DeviceDefault_Light);
        super.onCreate(savedInstanceState);

        setTitle(text("Szukaj w komentarzach", "Search comments"));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        // The app is drawn edge to edge, so keep the content clear of the bars and the keyboard.
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });

        query = new EditText(this);
        query.setSingleLine(true);
        query.setHint(text("Szukane słowa", "Words to search for"));
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                showResults();
            }
        });
        LinearLayout.LayoutParams queryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        queryParams.setMargins(dp(16), dp(8), dp(16), 0);
        content.addView(query, queryParams);

        status = new TextView(this);
        status.setPadding(dp(20), dp(8), dp(20), dp(8));
        content.addView(status);

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(results);
        content.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Dark system bar icons on the light theme.
            boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(night ? 0 : lightBars, lightBars);
        }

        long threadId = getIntent().getLongExtra(EXTRA_THREAD_ID, -1);
        showResults();
        new Thread(() -> load(threadId), "CommentSearch").start();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
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

    private void load(long threadId) {
        try {
            List<Long> parents = new ArrayList<>();
            loadPages("thread/" + threadId + "/comments?sort_by=most_helpful&exclude_inactive=false", parents);
            for (int i = 0; i < parents.size() && !destroyed; i++) {
                loadPages("comment/" + parents.get(i) + "/children?", null);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load the comments", ex);
            error = ex.getMessage();
        }
        runOnUiThread(() -> {
            loading = false;
            showResults();
        });
    }

    /**
     * Loads the comments of all pages of a list.
     *
     * @param parents Receives the IDs of the comments that have replies, or null.
     */
    private void loadPages(String path, List<Long> parents) throws Exception {
        String after = null;
        for (int page = 0; page < MAX_PAGES && !destroyed; page++) {
            String url = path + (path.endsWith("?") ? "" : "&") + "limit=" + PAGE_SIZE
                    + (after == null ? "" : "&after=" + URLEncoder.encode(after, "UTF-8"));
            JSONObject response = new JSONObject(PepperApi.get(url, JSON_FORMAT));

            JSONArray data = response.optJSONArray("data");
            List<Comment> loaded = new ArrayList<>();
            for (int i = 0; data != null && i < data.length(); i++) {
                JSONObject comment = data.getJSONObject(i);
                JSONObject poster = comment.optJSONObject("poster");
                loaded.add(new Comment(
                        poster != null ? poster.optString("username") : "",
                        TimeUnit.SECONDS.toMillis(comment.optLong("posted")),
                        // The plain text still has line breaks and entities of HTML.
                        Html.fromHtml(comment.optString("content_unformatted"), Html.FROM_HTML_MODE_LEGACY)
                                .toString().trim(),
                        comment.optString("permalink")));
                if (parents != null && comment.optInt("children_count") > 0) {
                    parents.add(comment.optLong("comment_id"));
                }
            }
            runOnUiThread(() -> {
                comments.addAll(loaded);
                showResults();
            });

            JSONObject cursors = response.optJSONObject("cursors");
            JSONObject next = cursors != null ? cursors.optJSONObject("next") : null;
            after = next != null && next.optBoolean("has_more") ? next.optString("after", null) : null;
            if (TextUtils.isEmpty(after) || data == null || data.length() == 0) {
                return;
            }
        }
    }

    private void showResults() {
        results.removeAllViews();

        String[] words = searchable(query.getText().toString()).trim().split("\\s+");
        boolean searching = words.length > 0 && !words[0].isEmpty();

        List<Comment> found = new ArrayList<>();
        if (searching) {
            for (Comment comment : comments) {
                boolean matches = true;
                for (String word : words) {
                    if (!comment.searchableText.contains(word)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    found.add(comment);
                }
            }
            Collections.sort(found, Comparator.comparingLong(comment -> comment.postedMillis));
        }

        String progress = loading
                ? text("Wczytywanie komentarzy… ", "Loading the comments… ") + comments.size()
                : error != null
                ? text("Nie udało się wczytać wszystkich komentarzy: ", "Could not load all comments: ") + error
                : text("Komentarze: ", "Comments: ") + comments.size();
        status.setText(searching
                ? progress + "\n" + text("Znalezione: ", "Found: ") + found.size()
                + (found.size() > MAX_RESULTS ? text(", pokazano ", ", showing ") + MAX_RESULTS : "")
                : progress);

        for (int i = 0; i < found.size() && i < MAX_RESULTS; i++) {
            results.addView(resultView(found.get(i), words));
        }
    }

    private TextView resultView(Comment comment, String[] words) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append(comment.author).append(" · ").append(DateUtils.formatDateTime(this, comment.postedMillis,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append('\n');
        int textStart = text.length();
        text.append(comment.text);

        // Searchable text keeps the length of each character, so the positions match.
        String searchableText = searchable(comment.text);
        int highlight = (accentColor() & 0x00FFFFFF) | 0x55000000;
        for (String word : words) {
            for (int index = searchableText.indexOf(word); index >= 0 && !word.isEmpty();
                 index = searchableText.indexOf(word, index + word.length())) {
                text.setSpan(new BackgroundColorSpan(highlight), textStart + index,
                        textStart + index + word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(attribute(android.R.attr.textAppearanceListItemSmall));
        view.setPadding(dp(20), dp(12), dp(20), dp(12));
        view.setBackgroundResource(attribute(android.R.attr.selectableItemBackground));
        view.setOnClickListener(clicked -> openComment(comment));
        view.setOnLongClickListener(clicked -> {
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(comment.author, comment.text));
            Toast.makeText(this, text("Skopiowano komentarz.", "Comment copied."), Toast.LENGTH_SHORT).show();
            return true;
        });
        return view;
    }

    /**
     * Opens the link of the comment in the app, which shows it on the screen of the deal.
     */
    private void openComment(Comment comment) {
        if (TextUtils.isEmpty(comment.permalink)) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(comment.permalink)).setPackage(getPackageName()));
        } catch (Exception ex) {
            Log.e(TAG, "Failed to open the comment", ex);
        }
    }

    /**
     * @return The text in lower case without diacritics, with the same length.
     */
    private static String searchable(String text) {
        StringBuilder searchable = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = Character.toLowerCase(text.charAt(i));
            if (character == 'ł') {
                character = 'l';
            } else if (character > 127) {
                String decomposed = Normalizer.normalize(String.valueOf(character), Normalizer.Form.NFD);
                character = decomposed.isEmpty() ? character : decomposed.charAt(0);
            }
            searchable.append(character);
        }
        return searchable.toString();
    }

    private int accentColor() {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
        return value.resourceId != 0 ? getColor(value.resourceId) : value.data;
    }

    private int attribute(int attribute) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        return value.resourceId;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
