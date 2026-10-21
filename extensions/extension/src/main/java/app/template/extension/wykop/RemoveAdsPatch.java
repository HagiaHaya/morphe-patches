package app.template.extension.wykop;

import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.util.Locale;

@SuppressWarnings("unused")
public final class RemoveAdsPatch {

    private static final String TAG = "MorpheWykop";

    /**
     * Hosts serving the ads on wykop.pl. Subdomains are blocked as well.
     */
    private static final String[] AD_HOSTS = {
            // Społeczności ad network. Loads the ad slots, consent popup and header bidding.
            "spolecznosci.net",
            "spolecznosci.pl",
            // Blockthrough, which re-inserts ads for users with an ad blocker.
            "btloader.com",
            "blockthrough.com",
            // Ad servers.
            "smartadserver.com",
            "doubleclick.net",
            "googlesyndication.com",
            "googletagservices.com",
            "googleadservices.com",
            "adservice.google.com",
            // Prebid bidders configured for wykop.pl.
            "4dex.io",
            "adform.net",
            "adnxs.com",
            "adnxs-simple.com",
            "adquery.io",
            "connectad.io",
            "creativecdn.com",
            "kueezrtb.com",
            "openx.net",
            "presage.io",
            "pubmatic.com",
            "r2b2.io",
            "rubiconproject.com",
            "sparteo.com",
            "ssp.wp.pl",
            "teads.tv",
            "visx.net",
    };

    /**
     * Ad slots of the website. They keep a fixed height when empty, and also
     * contain the sponsored posts served from wykop.pl itself.
     */
    private static final String AD_SLOT_SELECTORS = "section.pub-slot-wrapper,"
            + "section.opt-pub-slot,"
            + "section.tri-pub-slot,"
            + "section.premium-pub,"
            + ".spolecznoscinet,"
            + "[id^=\"spolecznosci-\"]";

    /**
     * Ad the website shows on mobile in place of the ad networks, which it does not load in the app.
     * Its class name changes every day, so it is matched by the markup of its component instead.
     * Kept as a separate rule, as WebView versions without :has() support would drop the whole rule.
     */
    private static final String ANTI_ADBLOCK_AD_SELECTOR = "section:has("
            + "> div.wrapper:only-child > img:first-child + a[target=\"_blank\"]:last-child)";

    private static final String STYLE_ELEMENT_ID = "morphe-remove-ads";

    private static final String HIDE_AD_SLOTS_SCRIPT = "(function() {"
            + "  if (document.getElementById('" + STYLE_ELEMENT_ID + "')) return;"
            + "  var parent = document.head || document.documentElement;"
            + "  if (!parent) return;"
            + "  var style = document.createElement('style');"
            + "  style.id = '" + STYLE_ELEMENT_ID + "';"
            + "  style.textContent = '" + AD_SLOT_SELECTORS + " { display: none !important; }"
            + " " + ANTI_ADBLOCK_AD_SELECTOR + " { display: none !important; }';"
            + "  parent.appendChild(style);"
            + "})();";

    /**
     * Injection point. Called on a background thread for every request of the WebView.
     *
     * @return An empty response if the request is to an ad server, or null to load it normally.
     */
    public static WebResourceResponse blockAdRequest(String url) {
        try {
            String host = Uri.parse(url).getHost();
            if (host != null && isAdHost(host.toLowerCase(Locale.ROOT))) {
                Log.d(TAG, "Blocked ad request: " + url);
                return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream(new byte[0]));
            }
        } catch (Exception ex) {
            Log.e(TAG, "blockAdRequest failure", ex);
        }

        return null;
    }

    /**
     * Injection point. Called on the main thread when a page starts and finishes loading.
     * The website navigates without reloading the page, so the stylesheet stays in place.
     */
    public static void hideAdSlots(WebView webView) {
        try {
            webView.evaluateJavascript(HIDE_AD_SLOTS_SCRIPT, null);
        } catch (Exception ex) {
            Log.e(TAG, "hideAdSlots failure", ex);
        }
    }

    /**
     * @return If the host is one of {@link #AD_HOSTS} or a subdomain of one.
     */
    private static boolean isAdHost(String host) {
        for (String adHost : AD_HOSTS) {
            if (host.endsWith(adHost)) {
                int prefixLength = host.length() - adHost.length();
                if (prefixLength == 0 || host.charAt(prefixLength - 1) == '.') {
                    return true;
                }
            }
        }

        return false;
    }
}
