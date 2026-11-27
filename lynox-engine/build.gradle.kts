import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = rootProject.group
version = rootProject.version

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    jvm()

    val lynoxXCFramework = XCFramework("LynoxEngine")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "LynoxEngine"
            isStatic = true
            lynoxXCFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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
            description.set("Independent Kotlin Multiplatform media extraction library.")
            url.set("https://github.com/anonymous-01-ghost/Lynox-Engine")
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
