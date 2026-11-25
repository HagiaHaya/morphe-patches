extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.template.extension"

    defaultConfig {
        // The extension only runs inside the patched apps, so it needs the lowest minSdk of their
        // supported versions: Wykop.pl 24, Pepper 26, Happy Color 29 and Slack 32.
        minSdk = 24
    }
}

dependencies {
    compileOnly(project(":extensions:extension:stub"))
}
