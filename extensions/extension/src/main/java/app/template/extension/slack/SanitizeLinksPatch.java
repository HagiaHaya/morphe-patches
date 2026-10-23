package app.template.extension.slack;

import android.net.Uri;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import app.template.extension.slack.settings.Settings;

/**
 * Removes tracking parameters from the links of the messages the user sends,
 * after the rules of ClearURLs: parameters that only track where a link was shared from,
 * or which ad or campaign it came from. The rest of the link is kept as it was written.
 */
@SuppressWarnings("unused")
public final class SanitizeLinksPatch {

    private static final String TAG = "MorpheSlack";

    /**
     * Prefixes of tracking parameters of any site.
     */
    private static final String[] GLOBAL_PREFIXES = {"utm_", "pk_", "mtm_", "hsa_", "__hs", "oly_"};

    /**
     * Tracking parameters of any site: ad click IDs and newsletter and analytics trackers.
     */
    private static final Set<String> GLOBAL_PARAMETERS = new HashSet<>(Arrays.asList(
            "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "ysclid",
            "twclid", "ttclid", "li_fat_id", "igshid", "igsh", "srsltid", "epik",
            "mc_cid", "mc_eid", "mkt_tok", "_hsenc", "_hsmi", "hsctatracking", "vero_id", "vero_conv",
            "_ga", "_gl", "_openstat", "rb_clickid", "s_cid", "wickedid", "ss_source", "ss_campaign_id"
    ));

    /**
     * Tracking parameters of sites, by the domain of the site, which includes its subdomains.
     * A name ending with '*' is a prefix.
     */
    private static final Map<String, Set<String>> SITE_PARAMETERS = new HashMap<>();

    /**
     * Domains of Amazon, whose links also carry tracking in a "ref=" path segment.
     */
    private static final Set<String> AMAZON_DOMAINS = new HashSet<>(Arrays.asList(
            "amazon.com", "amazon.co.uk", "amazon.de", "amazon.fr", "amazon.it", "amazon.es", "amazon.nl",
            "amazon.pl", "amazon.se", "amazon.com.be", "amazon.ca", "amazon.com.mx", "amazon.com.br",
            "amazon.co.jp", "amazon.in", "amazon.com.au", "amazon.sg", "amazon.ae", "amazon.sa",
            "amazon.com.tr", "amazon.eg"
    ));

    static {
        site(new String[]{"youtube.com", "youtu.be"}, "si", "feature", "pp", "kw");
        site(new String[]{"spotify.com"}, "si", "nd");
        site(new String[]{"twitter.com", "x.com"}, "s", "t", "ref_src", "ref_url", "cxt");
        site(new String[]{"instagram.com"}, "img_index");
        site(new String[]{"facebook.com"}, "mibextid", "__tn__", "__cft__*", "rdid", "sfnsn", "ref");
        site(new String[]{"tiktok.com"}, "is_from_webapp", "sender_device", "sender_web_id", "_r", "_t",
                "is_copy_url", "web_id", "u_code", "preview_pb", "share_app_name", "share_link_id",
                "share_item_id", "sharer_language", "social_sharing", "tt_from", "checksum", "_d");
        site(new String[]{"linkedin.com"}, "trk", "trackingid", "refid", "lipi", "midtoken", "midsig", "eid",
                "trkemail", "original_referer", "rcm");
        site(new String[]{"reddit.com"}, "share_id", "ref_source", "ref_campaign", "rdt", "correlation_id");
        site(new String[]{"medium.com"}, "source");
        site(new String[]{"aliexpress.com", "aliexpress.us"}, "spm", "scm", "algo_*", "aff_*", "gatewayadapt",
                "btsid", "pvid", "ws_ab_test", "curpagelogid", "sourcetype", "pdp_npi", "pdp_ext_f", "_t", "af");
        site(new String[]{"ebay.com", "ebay.co.uk", "ebay.de", "ebay.fr", "ebay.it", "ebay.es", "ebay.pl"},
                "_trkparms", "_trksid", "amdata", "mkevt", "mkcid", "mkrid", "campid", "toolid", "customid", "_from");
        site(AMAZON_DOMAINS.toArray(new String[0]), "pd_rd_*", "pf_rd_*", "ref", "ref_", "qid", "sr", "srs",
                "crid", "sprefix", "dib", "dib_tag", "content-id", "linkcode", "creativeasin", "ascsubtag",
                "aaxitk", "hsa_cr_id", "dchild", "camp", "creative", "spia", "ms3_c", "refrid", "_encoding");
    }

    private static void site(String[] domains, String... parameters) {
        Set<String> set = new HashSet<>(Arrays.asList(parameters));
        for (String domain : domains) {
            SITE_PARAMETERS.put(domain, set);
        }
    }

    /**
     * Injection point. Called with the link of a link element of a message being sent.
     *
     * @return The link without tracking parameters.
     */
    public static String sanitize(String link) {
        if (link == null || !Settings.isEnabled(Settings.SANITIZE_LINKS)) {
            return link;
        }
        try {
            return sanitizeLink(link, 0);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to sanitize " + link, ex);
            return link;
        }
    }

    /**
     * Injection point. Called with the text of a link element of a message being sent.
     *
     * @return The sanitized link if the text is the link itself, otherwise the text.
     */
    public static String sanitizeText(String link, String text) {
        if (text == null || !text.equals(link)) {
            return text;
        }
        return sanitize(text);
    }

    private static String sanitizeLink(String link, int depth) {
        Uri uri = Uri.parse(link);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || host == null) {
            return link;
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        // Links that only redirect to another one are replaced with it.
        String target = redirectTarget(uri, host);
        if (target != null && depth < 3) {
            return sanitizeLink(target, depth + 1);
        }

        Set<String> siteParameters = siteParameters(host);
        if (isGoogleSearch(host, uri)) {
            siteParameters = new HashSet<>(Arrays.asList("ved", "ei", "sa", "usg", "sxsrf", "gs_lcp", "gs_lp",
                    "oq", "aqs", "sourceid", "ie", "bih", "biw", "uact", "sclient", "iflsig", "rlz", "gs_ssp",
                    "dpr", "prmd", "sca_esv", "sca_upv", "ictx", "fbs", "client"));
        }

        // Split by hand, so the parameters kept stay encoded as they were.
        int fragmentStart = link.indexOf('#');
        String fragment = fragmentStart >= 0 ? link.substring(fragmentStart) : "";
        String rest = fragmentStart >= 0 ? link.substring(0, fragmentStart) : link;
        int queryStart = rest.indexOf('?');
        String base = queryStart >= 0 ? rest.substring(0, queryStart) : rest;
        String query = queryStart >= 0 ? rest.substring(queryStart + 1) : null;

        if (AMAZON_DOMAINS.contains(host)) {
            base = base.replaceAll("/ref=[^/?#]*", "");
        }

        StringBuilder kept = new StringBuilder();
        if (query != null) {
            for (String parameter : query.split("&")) {
                if (parameter.isEmpty()) {
                    continue;
                }
                int equals = parameter.indexOf('=');
                String name = Uri.decode(equals >= 0 ? parameter.substring(0, equals) : parameter)
                        .toLowerCase(Locale.ROOT);
                if (isTracking(name, siteParameters)) {
                    continue;
                }
                if (kept.length() > 0) {
                    kept.append('&');
                }
                kept.append(parameter);
            }
        }

        return kept.length() > 0 ? base + "?" + kept + fragment : base + fragment;
    }

    private static Set<String> siteParameters(String host) {
        for (String domain = host; domain != null; ) {
            Set<String> parameters = SITE_PARAMETERS.get(domain);
            if (parameters != null) {
                return parameters;
            }
            int dot = domain.indexOf('.');
            domain = dot >= 0 && domain.indexOf('.', dot + 1) >= 0 ? domain.substring(dot + 1) : null;
        }
        return null;
    }

    private static boolean isGoogleSearch(String host, Uri uri) {
        return (host.equals("google.com") || host.matches("google\\.[a-z.]{2,6}"))
                && "/search".equals(uri.getPath());
    }

    private static String redirectTarget(Uri uri, String host) {
        String path = uri.getPath();
        String target = null;
        if ((host.equals("google.com") || host.matches("google\\.[a-z.]{2,6}")) && "/url".equals(path)) {
            target = uri.getQueryParameter("q");
            if (target == null) {
                target = uri.getQueryParameter("url");
            }
        } else if ((host.equals("l.facebook.com") || host.equals("lm.facebook.com")) && "/l.php".equals(path)) {
            target = uri.getQueryParameter("u");
        } else if (host.equals("l.instagram.com")) {
            target = uri.getQueryParameter("u");
        } else if (host.equals("youtube.com") && "/redirect".equals(path)) {
            target = uri.getQueryParameter("q");
        }
        return target != null && (target.startsWith("https://") || target.startsWith("http://")) ? target : null;
    }

    private static boolean isTracking(String name, Set<String> siteParameters) {
        if (GLOBAL_PARAMETERS.contains(name)) {
            return true;
        }
        for (String prefix : GLOBAL_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        if (siteParameters != null) {
            if (siteParameters.contains(name)) {
                return true;
            }
            for (String parameter : siteParameters) {
                if (parameter.endsWith("*") && name.startsWith(parameter.substring(0, parameter.length() - 1))) {
                    return true;
                }
            }
        }
        return false;
    }
}
