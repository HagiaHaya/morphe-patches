package app.template.extension.wykop;

import android.util.Log;
import android.webkit.WebView;

final class WebViewStyle {

    private static final String TAG = "MorpheWykop";

    private WebViewStyle() {
    }

    /**
     * @param id  Id of the style element, so the stylesheet is added once per page.
     * @param css The stylesheet, which must not contain single quotes or backslashes.
     * @return A script that adds the stylesheet to the page, if it was not added yet.
     */
    static String script(String id, String css) {
        return "(function() {"
                + "  if (document.getElementById('" + id + "')) return;"
                + "  var parent = document.head || document.documentElement;"
                + "  if (!parent) return;"
                + "  var style = document.createElement('style');"
                + "  style.id = '" + id + "';"
                + "  style.textContent = '" + css + "';"
                + "  parent.appendChild(style);"
                + "})();";
    }

    /**
     * Runs a script of {@link #script(String, String)} on the page shown by the WebView.
     */
    static void add(WebView webView, String script) {
        try {
            webView.evaluateJavascript(script, null);
        } catch (Exception ex) {
            Log.e(TAG, "Could not add the stylesheet", ex);
        }
    }
}
