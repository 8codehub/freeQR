import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp)
}
kotlin {
    android {
        namespace = "qrstudio.shared"
        compileSdk = 37
        minSdk = 26
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest { isIncludeAndroidResources = true }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "QRStudio"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.qrcode)
            implementation(libs.datastore.core)
        }
        commonTest.dependencies { implementation(kotlin("test"));implementation(libs.coroutines.test) }
        androidMain.dependencies {
            implementation(libs.activity.compose)
            implementation(libs.androidx.core)
            implementation(libs.datastore)
            implementation(libs.profileinstaller)
        }

    }
}
compose.resources { packageOfResClass = "qrstudio.generated.resources"; publicResClass = true }
