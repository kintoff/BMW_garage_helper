plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
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
val aiAssistantBaseUrl = optionalConfig("AI_ASSISTANT_BASE_URL").orEmpty()
val useFirebaseAiLogic = stringConfig("USE_FIREBASE_AI_LOGIC", "false").toBoolean()
val hasFirebaseConfig = file("google-services.json").exists()
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
        buildConfigField("String", "AI_ASSISTANT_BASE_URL", "\"$aiAssistantBaseUrl\"")
        buildConfigField("boolean", "USE_FIREBASE_AI_LOGIC", useFirebaseAiLogic.toString())
        buildConfigField("boolean", "HAS_FIREBASE_CONFIG", hasFirebaseConfig.toString())

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
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }

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

    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation("org.json:json:20240303")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.firebase.appcheck.debug)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
}

jacoco {
    toolVersion = "0.8.12"
}

val jacocoDebugFileFilter = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*Preview*.*",
    "**/*\$Companion.*"
)

fun Project.debugCoverageClassDirectories() = files(
    fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoDebugFileFilter)
    },
    fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
        exclude(jacocoDebugFileFilter)
    }
)

fun JacocoReport.configureDebugCoverageReport() {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(project.debugCoverageClassDirectories())
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
}

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    dependsOn("testDebugUnitTest")

    configureDebugCoverageReport()

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
            )
        }
    )
}

tasks.register<JacocoReport>("jacocoDebugCombinedCoverageReport") {
    configureDebugCoverageReport()

    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                "outputs/managed_device_code_coverage/**/*.ec"
            )
        }
    )
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
