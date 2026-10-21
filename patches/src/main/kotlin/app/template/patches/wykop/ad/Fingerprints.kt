package app.template.patches.wykop.ad

import app.morphe.patcher.Fingerprint

/**
 * Wykop is a Flutter app that shows the wykop.pl website in a WebView from the
 * flutter_inappwebview plugin. The plugin keeps its class names unobfuscated,
 * so its Android classes can be matched by name.
 */
private const val IN_APP_WEBVIEW_PACKAGE =
    "Lcom/pichillilorenzo/flutter_inappwebview_android/webview/in_app_webview/"

internal const val WEB_RESOURCE_REQUEST_EXT_CLASS =
    "Lcom/pichillilorenzo/flutter_inappwebview_android/types/WebResourceRequestExt;"

/**
 * The plugin has two WebViewClient implementations and picks one at runtime,
 * depending on the installed Android System WebView version, so both are patched.
 */
internal val webViewClientClasses = listOf(
    "${IN_APP_WEBVIEW_PACKAGE}InAppWebViewClient;",
    "${IN_APP_WEBVIEW_PACKAGE}InAppWebViewClientCompat;",
)

/**
 * Every resource request of the WebView goes through this method,
 * including the ones the overloads from [android.webkit.WebViewClient] receive.
 */
internal class ShouldInterceptRequestFingerprint(webViewClientClass: String) : Fingerprint(
    definingClass = webViewClientClass,
    name = "shouldInterceptRequest",
    returnType = "Landroid/webkit/WebResourceResponse;",
    parameters = listOf("Landroid/webkit/WebView;", WEB_RESOURCE_REQUEST_EXT_CLASS),
)

/**
 * Called from onPageStarted() and onPageFinished() of the WebViewClient.
 */
internal class LoadCustomJavaScriptFingerprint(
    webViewClientClass: String,
    methodName: String,
) : Fingerprint(
    definingClass = webViewClientClass,
    name = methodName,
    returnType = "V",
    parameters = listOf("Landroid/webkit/WebView;"),
)
