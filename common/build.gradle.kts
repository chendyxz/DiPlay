plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose) apply false
}

val kitkat = providers.gradleProperty("kitkat").map(String::toBoolean).getOrElse(false)

if (kitkat) layout.buildDirectory.set(layout.projectDirectory.dir("build/kitkat"))

if (!kitkat) apply(plugin = "org.jetbrains.kotlin.plugin.compose")

android {
    namespace = "com.shilapi.xcertplay.host"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = if (kitkat) 19 else 28
        multiDexEnabled = kitkat
        buildConfigField("boolean", "KITKAT_BUILD", kitkat.toString())
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = kitkat
    }

    testOptions { unitTests.isIncludeAndroidResources = kitkat }

    buildFeatures {
        compose = !kitkat
        buildConfig = true
    }
}

dependencies {
    if (kitkat) {
        implementation("androidx.activity:activity-ktx:1.8.0")
        implementation("androidx.core:core-ktx:1.13.1")
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    } else {
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.core.ktx)
        debugImplementation(libs.androidx.compose.ui.tooling)
    }
    api(project(":shared"))
    testImplementation(libs.junit)
    testImplementation(if (kitkat) "org.robolectric:robolectric:4.11.1" else "org.robolectric:robolectric:4.17")
    implementation(libs.androidx.lifecycle.runtime.ktx)
}

android {
    sourceSets {
        named("main") {
            if (kitkat) {
                manifest.srcFile("src/kitkat/AndroidManifest.xml")
                java.directories.add("src/kitkat/java")
                kotlin.directories.add("src/kitkat/java")
                res.directories.add("src/kitkat/res")
            }
            else {
                java.directories.add("src/modern/java")
                kotlin.directories.add("src/modern/java")
            }
        }
        if (kitkat) named("test") {
            java.directories.clear()
            java.directories.add("src/kitkatTest/java")
            kotlin.directories.clear()
            kotlin.directories.add("src/kitkatTest/java")
        }
    }
}

if (kitkat) {
    tasks.withType<Test>().configureEach {
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    }
}
