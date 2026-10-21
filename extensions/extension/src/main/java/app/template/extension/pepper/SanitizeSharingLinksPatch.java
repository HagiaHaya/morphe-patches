package app.template.extension.pepper;

import android.net.Uri;
import android.util.Log;

import java.util.Locale;

@SuppressWarnings("unused")
public final class SanitizeSharingLinksPatch {

    private static final String TAG = "MorphePepper";

    /**
     * Prefix of the query parameters used to track where a link was shared from.
     */
    private static final String TRACKING_PARAMETER_PREFIX = "utm_";

    /**
     * Injection point. Called when the link to share a thread is read from the API response.
     *
     * @param threadUrl     Address of the thread on the website.
     * @param shareableLink Link that redirects to the thread through the Adjust tracking service.
     * @return The thread address without tracking parameters,
     *         or the shareable link if the thread address is missing.
     */
    public static String getThreadShareUrl(String threadUrl, String shareableLink) {
        try {
            if (threadUrl != null) {
                Uri uri = Uri.parse(threadUrl);
                String scheme = uri.getScheme();
                if (("https".equals(scheme) || "http".equals(scheme)) && uri.getHost() != null) {
                    return removeTrackingParameters(uri).toString();
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "getThreadShareUrl failure", ex);
        }

        return shareableLink;
    }

    private static Uri removeTrackingParameters(Uri uri) {
        if (uri.getQuery() == null) {
            return uri;
        }

        Uri.Builder builder = uri.buildUpon().clearQuery();
        for (String name : uri.getQueryParameterNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(TRACKING_PARAMETER_PREFIX)) {
                continue;
            }
            for (String value : uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value);
            }
        }

        return builder.build();
    }
}
