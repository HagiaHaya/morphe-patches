package app.template.extension.pepper.api;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * Requests to the REST API of Pepper, made with the HTTP client of the app, so its interceptors
 * sign them in as the user and add the headers the API expects.
 */
@SuppressWarnings("unused")
public final class PepperApi {

    private static final String TAG = "MorphePepper";

    private static final String REST_API_PATH = "/rest_api/v2/";

    /**
     * HTTP client of the app that made the last request to the REST API.
     */
    private static volatile Object client;

    /**
     * Address of the REST API, such as https://www.pepper.pl/rest_api/v2/.
     */
    private static volatile String restApiUrl;

    /**
     * When the last request to the REST API was made, in {@link SystemClock#elapsedRealtime()}.
     */
    private static volatile long lastCallTime;

    /**
     * Injection point. Called when the app creates an HTTP call.
     */
    public static void onCallCreated(Object client, Object request) {
        try {
            // Request{method=GET, url=https://www.pepper.pl/rest_api/v2/thread?..., ...}
            String description = String.valueOf(request);
            int urlStart = description.indexOf("url=");
            int pathStart = urlStart < 0 ? -1 : description.indexOf(REST_API_PATH, urlStart);
            if (pathStart < 0) {
                return;
            }
            restApiUrl = description.substring(urlStart + "url=".length(), pathStart + REST_API_PATH.length());
            PepperApi.client = client;
            lastCallTime = SystemClock.elapsedRealtime();
        } catch (Exception ex) {
            Log.e(TAG, "onCallCreated failure", ex);
        }
    }

    /**
     * @return If the app made a request to the REST API, so the client to make more is known.
     */
    public static boolean isReady() {
        return client != null;
    }

    /**
     * @return When the last request to the REST API was made, in {@link SystemClock#elapsedRealtime()}.
     */
    public static long getLastCallTime() {
        return lastCallTime;
    }

    /**
     * @return The address of the website, such as https://www.pepper.pl, or null if not known yet.
     */
    public static String getWebsiteUrl() {
        String url = restApiUrl;
        return url == null ? null : url.substring(0, url.length() - REST_API_PATH.length());
    }

    /**
     * Sends a GET request to the REST API. Blocks, so must not be called on the main thread.
     *
     * @param pathAndQuery Path below the REST API, such as {@code thread?limit=20}.
     * @return The body of the response.
     */
    public static String get(String pathAndQuery) throws IOException {
        return get(pathAndQuery, null);
    }

    /**
     * Sends a GET request to the REST API. Blocks, so must not be called on the main thread.
     *
     * @param pathAndQuery Path below the REST API, such as {@code thread?limit=20}.
     * @param jsonFormat   Value of the Pepper-JSON-Format header, which selects the representations
     *                     in the response, such as {@code formatted_text=html}, or null.
     * @return The body of the response.
     */
    public static String get(String pathAndQuery, String jsonFormat) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Network request on the main thread");
        }
        Object httpClient = client;
        if (httpClient == null) {
            throw new IOException("The app has not used the API yet");
        }

        Object request = jsonFormat == null
                ? newRequest(restApiUrl + pathAndQuery, null, null)
                : newRequest(restApiUrl + pathAndQuery, "Pepper-JSON-Format", jsonFormat);
        Object response = execute(httpClient, request);
        try {
            int code = responseCode(response);
            String body = responseBody(response);
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + pathAndQuery);
            }
            return body == null ? "" : body;
        } finally {
            ((Closeable) response).close();
        }
    }

    // The methods below are generated during patching, as they use the obfuscated classes of OkHttp.

    /**
     * @param headerName The name of a header to add, or null.
     */
    private static Object newRequest(String url, String headerName, String headerValue) {
        throw new UnsupportedOperationException();
    }

    private static Object execute(Object client, Object request) throws IOException {
        throw new UnsupportedOperationException();
    }

    private static int responseCode(Object response) {
        throw new UnsupportedOperationException();
    }

    private static String responseBody(Object response) throws IOException {
        throw new UnsupportedOperationException();
    }
}
