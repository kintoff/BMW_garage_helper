plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun Project.stringConfig(name: String, defaultValue: String): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(defaultValue)
        .get()

fun Project.intConfig(name: String, defaultValue: Int): Int =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .map(String::toInt)
        .orElse(defaultValue)
        .get()

fun Project.optionalConfig(name: String): String? =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull

val releaseVersionCode = intConfig("APP_VERSION_CODE", 1)
val releaseVersionName = stringConfig("APP_VERSION_NAME", "0.1.0")
val signingStoreFilePath = optionalConfig("ANDROID_SIGNING_STORE_FILE")
val signingStorePassword = optionalConfig("ANDROID_SIGNING_STORE_PASSWORD")
val signingKeyAlias = optionalConfig("ANDROID_SIGNING_KEY_ALIAS")
val signingKeyPassword = optionalConfig("ANDROID_SIGNING_KEY_PASSWORD")
val hasReleaseSigning =
    !signingStoreFilePath.isNullOrBlank() &&
        !signingStorePassword.isNullOrBlank() &&
        !signingKeyAlias.isNullOrBlank() &&
        !signingKeyPassword.isNullOrBlank()

android {
    namespace = "pl.garage.bmwassistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "pl.garage.bmwassistant"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        buildConfigField("String", "UPDATE_REPO_OWNER", "\"kintoff\"")
        buildConfigField("String", "UPDATE_REPO_NAME", "\"BMW_garage_helper\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingStoreFilePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
