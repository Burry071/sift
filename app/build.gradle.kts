import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

// Release signing lives in keystore/keystore.properties (git-ignored, local only).
// Without it the release build is simply unsigned; debug builds are unaffected.
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore/keystore.properties")
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { keystoreProps.load(it) }
}

android {
    namespace = "com.sift"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sift.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Phones are arm64; keeps the downloadable APK at half the size.
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.8.5")
    // kapt, not ksp: KSP has no Kotlin 2.4 release yet, and litertlm 0.17+ requires Kotlin 2.4.
    kapt("androidx.room:room-compiler:2.8.5")
    // Room 2.8.5's processor ships a metadata reader that tops out at Kotlin
    // metadata 2.3; Kotlin 2.4 classes need the matching reader on the kapt path.
    kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // 0.17.0 is the first release with Gemma 4 support (0.16.1 predates it).
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
