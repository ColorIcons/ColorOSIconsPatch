plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.parcelize")
}

android {
    namespace = "com.immortal521.colorosiconspatch"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    val appVersion = providers.gradleProperty("release.version")
        .map { it.removePrefix("v").substringBefore('-') }
        .getOrElse("1.0.5")
    val versionParts = appVersion.split('.').map { it.toIntOrNull() }
    require(versionParts.size == 3 && versionParts.all { it != null && it >= 0 }) {
        "release.version must use major.minor.patch, got: $appVersion"
    }
    val (major, minor, patch) = versionParts.map { it!! }
    val appVersionCode = major * 1_000_000 + minor * 1_000 + patch

    defaultConfig {
        applicationId = "com.immortal521.colorosiconspatch"
        minSdk = 33
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseSigning = listOf(
        "releaseKeystore",
        "releaseKeystorePassword",
        "releaseKeyAlias",
        "releaseKeyPassword"
    ).map { providers.gradleProperty(it).orNull }
    val hasReleaseSigning = releaseSigning.all { !it.isNullOrBlank() }
    if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = file(releaseSigning[0]!!)
            storePassword = releaseSigning[1]
            keyAlias = releaseSigning[2]
            keyPassword = releaseSigning[3]
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = providers.gradleProperty("release.minify").map(String::toBoolean).getOrElse(true)
            isShrinkResources = providers.gradleProperty("release.shrinkResources").map(String::toBoolean).getOrElse(true)
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val releaseAbis = providers.gradleProperty("release.abis").orNull
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()
    if (releaseAbis.isNotEmpty()) {
        defaultConfig {
            ndk {
                abiFilters += releaseAbis
            }
        }
    }

    val releaseLocales = providers.gradleProperty("release.locales").orNull
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()
    if (releaseLocales.isNotEmpty()) {
        androidResources {
            localeFilters += releaseLocales
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.libsu.core)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.nav)
    implementation(libs.material.kolor)
    implementation(libs.hidden.api.bypass)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}