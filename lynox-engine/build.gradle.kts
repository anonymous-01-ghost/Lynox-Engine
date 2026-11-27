plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

// Keep published module coordinates aligned with the root release configuration.
group = rootProject.group
version = rootProject.version

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "LynoxEngine"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "dev.lynox.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }
}


publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Lynox Engine")
            description.set("Kotlin Multiplatform library for provider-neutral media search and catalog metadata.")
            url.set("https://github.com/anonymous-01-ghost/Lynox-Engine")
            licenses {
                license {
                    name.set("License not selected")
                    url.set("https://github.com/anonymous-01-ghost/Lynox-Engine/blob/main/LICENSE_SELECTION.md")
                    distribution.set("repo")
                }
            }
            scm {
                url.set("https://github.com/anonymous-01-ghost/Lynox-Engine")
                connection.set("scm:git:https://github.com/anonymous-01-ghost/Lynox-Engine.git")
                developerConnection.set("scm:git:ssh://git@github.com/anonymous-01-ghost/Lynox-Engine.git")
            }
        }
    }

    repositories {
        maven {
            name = "buildDirectory"
            url = uri(layout.buildDirectory.dir("repository"))
        }
    }
}
