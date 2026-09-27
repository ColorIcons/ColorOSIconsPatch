plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.parcelize")
}

android {
    namespace = "com.immortal521.colorosiconspatch"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.immortal521.colorosiconspatch"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = providers.gradleProperty("release.minify").map(String::toBoolean).getOrElse(true)
            isShrinkResources = providers.gradleProperty("release.shrinkResources").map(String::toBoolean).getOrElse(true)
            signingConfig = signingConfigs.getByName("debug")
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