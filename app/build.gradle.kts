plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.micromasters.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.micromasters.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 14
        versionName = "2.4"
        resourceConfigurations += listOf("hu", "en")
    }

    signingConfigs {
        create("release") {
            // Populated by CI (an ephemeral keystore generated at build time) via env vars.
            // Local debug builds don't need these and are left untouched.
            System.getenv("KEYSTORE_FILE")?.let { ks ->
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASS")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASS")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ""
            isMinifyEnabled = false
        }
        release {
            // A real, non-debuggable, signed release build. Fixes the audit's only High
            // finding ("Application is debuggable"). Signing is only wired up when CI
            // provides a keystore, so local `assembleDebug` keeps working unchanged.
            // R8 minify: real code obfuscation + dead-code removal (resolves the
            // "DEX obfuscated" conflict). Resource shrinking is left off deliberately —
            // it can strip dynamically-referenced resources and isn't device-verifiable here.
            // Keep rules (JS bridge, custom Views, workers, enums) live in proguard-rules.pro.
            isMinifyEnabled = true
            if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.2")
}
