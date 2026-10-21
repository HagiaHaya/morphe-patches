package app.template.extension.wykop;

import android.webkit.WebView;

@SuppressWarnings("unused")
public final class HideNewsletterPromptPatch {

    /**
     * Section the stream shows the prompt in. It keeps the height of the prompt when the prompt
     * is scrolled out of view. The newsletter sign-up page uses the class too, without the attribute.
     */
    private static final String STREAM_SECTION_SELECTOR = "section.newsletter[data-scroll-closest]";

    /**
     * The prompt itself, wherever else it is shown, matched by the id of its newsletter switch.
     * Kept as a separate rule, as WebView versions without :has() support would drop the whole rule.
     */
    private static final String PROMPT_SELECTOR = "article:has(#switch-newsletter-cta)";

    private static final String HIDE_NEWSLETTER_PROMPT_SCRIPT = WebViewStyle.script("morphe-hide-newsletter-prompt",
            STREAM_SECTION_SELECTOR + " { display: none !important; } "
                    + PROMPT_SELECTOR + " { display: none !important; }");

    /**
     * Injection point. Called on the main thread when a page starts and finishes loading.
     */
    public static void hideNewsletterPrompt(WebView webView) {
        WebViewStyle.add(webView, HIDE_NEWSLETTER_PROMPT_SCRIPT);
    }
}
