plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
}


group = "dev.lynox"
version = providers.gradleProperty("VERSION_NAME").orElse("0.1.0-SNAPSHOT").get()
