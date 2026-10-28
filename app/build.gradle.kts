plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Removed androidx.navigation.safeargs.kotlin for build compatibility

// kapt/parcelize/serialization plugins not available in plugin portal for this Gradle version;
// enable via compiler args / dependency substitutions where needed.

android {
    namespace = "com.nana.assistant"
    compileSdk = rootProject.ext.get("android.compileSdk").toString().toInt()
    defaultConfig {
        applicationId = "com.nana.assistant"
        minSdk = rootProject.ext.get("android.minSdk").toString().toInt()
        targetSdk = rootProject.ext.get("android.targetSdk").toString().toInt()
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // No INTERNET permission - fully offline
    }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password"
            keyAlias = System.getenv("KEY_ALIAS") ?: "release"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "password"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=androidx.lifecycle.ExperimentalLifecycleApi"
        )
    }
    buildFeatures {
        compose = false
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.ext.get("compose.compiler.version") as String
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    // Ensure assets are not compressed for model files
    aaptOptions {
        noCompress("gguf", "onnx", "ppn", "bin", "tflite")
    }
}

dependencies {
    // Core modules
    implementation(project(":core:wakeword"))
    implementation(project(":core:stt"))
    implementation(project(":core:tts"))
    implementation(project(":core:llm"))
    implementation(project(":core:accessibility"))
    implementation(project(":core:media"))
    implementation(project(":core:agent"))
    implementation(project(":core:storage"))
    implementation(project(":core:phone"))
    implementation(project(":core:macros"))
    implementation(project(":core:ocr"))
    implementation(project(":core:notifications"))
    implementation(project(":core:settings"))

    // Shared
    implementation(project(":shared:common"))
    implementation(project(":shared:logging"))

    // UI modules
    implementation(project(":ui:main"))
    implementation(project(":ui:settings"))

    // AndroidX
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.window:window:1.2.0")

    // Hilt removed for build compatibility; use manual DI or re-enable via KSP.
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Test
    testImplementation("junit:junit:${rootProject.ext.get("junit.version")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.ext.get("junit.jupiter.version")}")
    testImplementation("io.mockk:mockk:${rootProject.ext.get("mockk.version")}")
    testImplementation("app.cash.turbine:turbine:${rootProject.ext.get("turbine.version")}")
    androidTestImplementation("androidx.test:core:${rootProject.ext.get("androidx.test.version")}")
    androidTestImplementation("androidx.test:rules:${rootProject.ext.get("androidx.test.version")}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${rootProject.ext.get("androidx.test.espresso.version")}")
    androidTestImplementation("androidx.test.espresso:espresso-intents:${rootProject.ext.get("androidx.test.espresso.version")}")
}

// kapt block removed for build compatibility
