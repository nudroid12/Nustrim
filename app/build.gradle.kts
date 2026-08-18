plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val nustrimKeystorePath = System.getenv("NUSTRIM_KEYSTORE_PATH")
val nustrimKeystorePassword = System.getenv("NUSTRIM_KEYSTORE_PASSWORD")
val nustrimKeyAlias = System.getenv("NUSTRIM_KEY_ALIAS")
val nustrimKeyPassword = System.getenv("NUSTRIM_KEY_PASSWORD")

android {
    namespace = "app.nudroidlabs.nustrim"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.nudroidlabs.nustrim"
        minSdk = 23
        targetSdk = 37
        versionCode = 51
        versionName = "0.17.0-tv2-rc6"
    }

    val persistentUpdateSigning = if (
        !nustrimKeystorePath.isNullOrBlank() &&
        !nustrimKeystorePassword.isNullOrBlank() &&
        !nustrimKeyAlias.isNullOrBlank() &&
        !nustrimKeyPassword.isNullOrBlank()
    ) {
        signingConfigs.create("nustrimUpdate") {
            storeFile = file(nustrimKeystorePath)
            storePassword = nustrimKeystorePassword
            keyAlias = nustrimKeyAlias
            keyPassword = nustrimKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        getByName("debug") {
            persistentUpdateSigning?.let { signingConfig = it }
        }
        getByName("release") {
            persistentUpdateSigning?.let { signingConfig = it }
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")

    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")

    implementation("com.github.recloudstream.cloudstream:library:-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
