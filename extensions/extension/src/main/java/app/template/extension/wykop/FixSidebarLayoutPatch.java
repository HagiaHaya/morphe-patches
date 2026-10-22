package app.template.extension.wykop;

import android.webkit.WebView;

@SuppressWarnings("unused")
public final class FixSidebarLayoutPatch {

    /**
     * Sidebar the website slides in on mobile. It is fixed 5px from the edges of the WebView,
     * which the app draws behind the status bar and the navigation bar.
     */
    private static final String SIDEBAR_SELECTOR = "aside.left-panel.mobile";

    /**
     * Night mode switch at the bottom of the sidebar. It is fixed to the bottom
     * of the WebView, so it has to be moved up by the same inset as the sidebar.
     */
    private static final String SIDEBAR_FOOTER_SELECTOR = SIDEBAR_SELECTOR + " > footer";

    /**
     * Moves the sidebar by the safe area insets the website already pads its header with.
     * The margin keeps it clear of a display cutout in landscape without
     * overriding left, which the website animates to slide the sidebar in.
     */
    private static final String FIX_SIDEBAR_LAYOUT_SCRIPT = WebViewStyle.script("morphe-fix-sidebar-layout",
            SIDEBAR_SELECTOR + " {"
                    + " top: calc(5px + env(safe-area-inset-top)) !important;"
                    + " bottom: calc(5px + env(safe-area-inset-bottom)) !important;"
                    + " margin-left: env(safe-area-inset-left) !important; } "
                    + SIDEBAR_FOOTER_SELECTOR + " {"
                    + " top: calc(100% - 50px - env(safe-area-inset-bottom)) !important; }");

    /**
     * Injection point. Called on the main thread when a page starts and finishes loading.
     */
    public static void fixSidebarLayout(WebView webView) {
        WebViewStyle.add(webView, FIX_SIDEBAR_LAYOUT_SCRIPT);
    }
}
