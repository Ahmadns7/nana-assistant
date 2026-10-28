import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nana.assistant"
    compileSdk = rootProject.ext.get("android.compileSdk").toString().toInt()
    defaultConfig {
        minSdk = rootProject.ext.get("android.minSdk").toString().toInt()
        targetSdk = rootProject.ext.get("android.targetSdk").toString().toInt()
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
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
}

dependencies {
    // Core Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${rootProject.ext.get("kotlin.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${rootProject.ext.get("kotlinx.coroutines.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.ext.get("kotlinx.coroutines.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.ext.get("kotlinx.serialization.json")}")

    // AndroidX Core
    implementation("androidx.core:core-ktx:${rootProject.ext.get("androidx.core.version")}")
    implementation("androidx.appcompat:appcompat:${rootProject.ext.get("androidx.appcompat.version")}")
    implementation("androidx.activity:activity-ktx:${rootProject.ext.get("androidx.activity.version")}")
    implementation("androidx.fragment:fragment-ktx:${rootProject.ext.get("androidx.fragment.version")}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${rootProject.ext.get("androidx.lifecycle.version")}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${rootProject.ext.get("androidx.lifecycle.version")}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${rootProject.ext.get("androidx.lifecycle.version")}")

    // Room
    implementation("androidx.room:room-runtime:${rootProject.ext.get("androidx.room.version")}")
    implementation("androidx.room:room-ktx:${rootProject.ext.get("androidx.room.version")}")
    implementation("androidx.room:room-compiler:${rootProject.ext.get("androidx.room.version")}")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:${rootProject.ext.get("androidx.datastore.version")}")
    implementation("androidx.datastore:datastore-core:${rootProject.ext.get("androidx.datastore.version")}")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:${rootProject.ext.get("androidx.navigation.version")}")
    implementation("androidx.navigation:navigation-ui-ktx:${rootProject.ext.get("androidx.navigation.version")}")
    implementation("androidx.navigation:navigation-compose:${rootProject.ext.get("androidx.navigation.version")}")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:${rootProject.ext.get("androidx.work.version")}")

    // Security
    implementation("androidx.security:security-crypto:${rootProject.ext.get("androidx.security.version")}")

    // Biometric
    implementation("androidx.biometric:biometric-ktx:${rootProject.ext.get("androidx.biometric.version")}")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout-compose:${rootProject.ext.get("androidx.constraintlayout.version")}")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:${rootProject.ext.get("compose.bom")}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava3")

    // Media3
    implementation("androidx.media3:media3-session:${rootProject.ext.get("media3.version")}")
    implementation("androidx.media3:media3-common:${rootProject.ext.get("media3.version")}")
    implementation("androidx.media3:media3-exoplayer:${rootProject.ext.get("media3.version")}")

    // ML Kit
    implementation("com.google.mlkit:text-recognition:${rootProject.ext.get("mlkit.text.recognition.version")}")
    implementation("com.google.mlkit:language-id:${rootProject.ext.get("mlkit.language.id.version")}")

    // Vosk STT
    implementation("com.alphacephei:vosk-android:${rootProject.ext.get("vosk.version")}")

    // Logging
    implementation("com.jakewharton.timber:timber:${rootProject.ext.get("timber.version")}")

    // Okio
    implementation("com.squareup.okio:okio:${rootProject.ext.get("okio.version")}")

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

tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=androidx.lifecycle.ExperimentalLifecycleApi"
        )
    }
}