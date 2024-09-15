plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.example.commuterx_java"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.commuterx_java"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.material.v140)
    implementation (libs.facebook.android.sdk)
    implementation (libs.facebook.login)
    implementation (libs.firebase.auth.vlatestrelease)
    implementation (libs.firebase.database)
    implementation(platform(libs.firebase.bom))
    implementation (libs.play.services.auth)
    implementation(libs.google.firebase.auth)
    implementation (libs.android)
    implementation (libs.ui.components)
    implementation("com.mapbox.maps:android:11.6.0")
    implementation("com.facebook.android:facebook-android-sdk:latest.release")
    implementation ("com.mapbox.search:mapbox-search-android:2.3.0")
    implementation ("com.mapbox.search:mapbox-search-android-ui:2.3.0")
    implementation ("com.mapbox.search:autofill:2.3.0")
    implementation ("com.mapbox.search:discover:2.3.0")
    implementation ("com.mapbox.search:place-autocomplete:2.3.0")
    implementation ("com.mapbox.search:offline:2.3.0")
}