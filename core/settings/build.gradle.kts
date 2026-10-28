plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nana.core.settings"
    compileSdk = rootProject.ext.get("android.compileSdk").toString().toInt()
    defaultConfig {
        minSdk = rootProject.ext.get("android.minSdk").toString().toInt()
        targetSdk = rootProject.ext.get("android.targetSdk").toString().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false        }
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${rootProject.ext.get("kotlinx.coroutines.version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.ext.get("kotlinx.serialization.json")}")
    implementation("androidx.core:core-ktx:${rootProject.ext.get("androidx.core.version")}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${rootProject.ext.get("androidx.lifecycle.version")}")
    implementation("androidx.appcompat:appcompat:${rootProject.ext.get("androidx.appcompat.version")}")
    implementation("com.jakewharton.timber:timber:${rootProject.ext.get("timber.version")}")
    implementation(project(":core:storage"))
    implementation("androidx.datastore:datastore-preferences:${rootProject.ext.get("androidx.datastore.version")}")

    implementation(project(":shared:common"))
    implementation(project(":shared:logging"))
    
    // Test
    testImplementation("junit:junit:${rootProject.ext.get("junit.version")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.ext.get("junit.jupiter.version")}")
    testImplementation("io.mockk:mockk:${rootProject.ext.get("mockk.version")}")
    testImplementation("app.cash.turbine:turbine:${rootProject.ext.get("turbine.version")}")
    androidTestImplementation("androidx.test:core:${rootProject.ext.get("androidx.test.version")}")
    androidTestImplementation("androidx.test:rules:${rootProject.ext.get("androidx.test.version")}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${rootProject.ext.get("androidx.test.espresso.version")}")
}