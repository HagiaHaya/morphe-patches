// Compile-only stubs of the classes of Slack the extension uses. They mirror the names and
// descriptors in the app, which is not obfuscated. Fields are not final, so javac reads
// them instead of inlining constants.
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Framework classes the stubs name in their signatures, such as Context.
    compileOnly(libs.android) {
        isTransitive = false
    }
}
