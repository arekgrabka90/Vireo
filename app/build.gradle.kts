plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val tmdbApiKey = providers.gradleProperty("TMDB_API_KEY")
    .orElse(providers.environmentVariable("TMDB_API_KEY"))
    .getOrElse("")
val escapedTmdbApiKey = tmdbApiKey
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val vireoSigningStorePath =
    providers.environmentVariable("VIREO_KEYSTORE_PATH").orNull
val vireoSigningStorePassword =
    providers.environmentVariable("VIREO_KEYSTORE_PASSWORD").orNull
val vireoSigningKeyAlias =
    providers.environmentVariable("VIREO_KEY_ALIAS").orNull
val vireoSigningKeyPassword =
    providers.environmentVariable("VIREO_KEY_PASSWORD").orNull

val vireoHasStableSigning = listOf(
    vireoSigningStorePath,
    vireoSigningStorePassword,
    vireoSigningKeyAlias,
    vireoSigningKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "app.vireo"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.vireo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1000 + (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
        versionName = "0.1.0"
        buildConfigField("String", "TMDB_API_KEY", "\"$escapedTmdbApiKey\"")

    }


    signingConfigs {
        if (vireoHasStableSigning) {
            create("stable") {
                storeFile = file(vireoSigningStorePath!!)
                storePassword = vireoSigningStorePassword!!
                keyAlias = vireoSigningKeyAlias!!
                keyPassword = vireoSigningKeyPassword!!
            }
        }
    }

    buildTypes {
        debug {
            if (vireoHasStableSigning) {
                signingConfig = signingConfigs.getByName("stable")
            }
        }
        release {
            isMinifyEnabled = false
            if (vireoHasStableSigning) {
                signingConfig = signingConfigs.getByName("stable")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.tv:tv-material:1.0.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
}
