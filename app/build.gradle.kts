plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.phani.voicelock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phani.voicelock"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // A fixed debug key (committed on purpose) so every CI build is signed the same way
    // and a new APK installs over the previous one. Debug builds only; never use it for release.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        jniLibs { useLegacyPackaging = true }
        resources.excludes += listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/DEPENDENCIES",
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.biometric:biometric:1.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Unpacks the downloaded Whisper model archive (.tar.bz2)
    implementation("org.apache.commons:commons-compress:1.26.2")
    // On-device Whisper speech-to-text and speaker verification (sherpa-onnx).
    // The AAR is fetched by scripts/fetch_sherpa.sh into app/libs.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    testImplementation("junit:junit:4.13.2")
}
