import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.compose); alias(libs.plugins.google.services) }
// Release signing is read from keystore.properties (git-ignored): storeFile, storePassword, keyAlias, keyPassword
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
android {
    namespace = "com.free.qr"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.free.qr"; minSdk = 26; targetSdk = 36; versionCode = 35; versionName =
        "3.0.35"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Only ship the languages the app is translated into; drops ~80 unused AndroidX locales from resources.arsc
    androidResources {
        localeFilters += listOf(
            "en",
            "ar",
            "bn",
            "de",
            "es",
            "fr",
            "hi",
            "id",
            "it",
            "ja",
            "ko",
            "pl",
            "pt",
            "ru",
            "th",
            "tr",
            "uk",
            "ur",
            "vi",
            "zh"
        )
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) create("release") {
            storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true; isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources.excludes += listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/**/LICENSE*",
            "/META-INF/*.version",
            "/META-INF/*.kotlin_module",
            "/kotlin/**",
            "DebugProbesKt.bin",
            "/*.properties",
        )
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true; all {
            it.systemProperty(
                "storeScreenshotsDir",
                rootProject.file("play-store-screenshots").absolutePath
            )
        }
        }
    }
    // Dependency metadata is still added to the AAB for Play, but not to standalone APKs
    dependenciesInfo { includeInApk = false; includeInBundle = true }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
    implementation(project(":composeApp"))
    implementation(libs.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.profileinstaller)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.uitest)
    testImplementation(libs.coroutines.core)
    testImplementation("androidx.compose.foundation:foundation:1.12.1")
    testImplementation("androidx.compose.material3:material3:1.4.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.compose.uitest)
    androidTestImplementation(libs.zxing.test)
    debugImplementation(libs.compose.uitest.manifest)
}
