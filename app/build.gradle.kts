plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nana.assistant"
    compileSdk = rootProject.ext.get("android.compileSdk") as Int
    defaultConfig {
        applicationId = "com.nana.assistant"
        minSdk = rootProject.ext.get("android.minSdk") as Int
        targetSdk = rootProject.ext.get("android.targetSdk") as Int
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // No INTERNET permission - fully offline
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
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
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
        compose = true
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
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.window:window:1.2.0")

    // Hilt for DI (optional, can use manual)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
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
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48")
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}