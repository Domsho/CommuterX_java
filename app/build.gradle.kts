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
    implementation(libs.espresso.core)
    implementation(libs.play.services.fido)
    implementation(libs.play.services.fido)
    implementation(libs.foundation.android)
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
    implementation(libs.android.v1161)
    implementation(libs.facebook.android.sdk)
    implementation (libs.mapbox.search.android)
    implementation (libs.mapbox.search.android.ui)
    implementation (libs.autofill)
    implementation (libs.discover)
    implementation (libs.place.autocomplete)
    implementation (libs.offline)
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.mapbox.maps:android:11.7.1")
    implementation ("com.mapbox.navigationcore:android:3.4.0")
    implementation ("com.mapbox.navigationcore:navigation:3.4.0")
    implementation ("com.mapbox.navigationcore:ui-components:3.4.0")
    implementation ("com.mapbox.navigationcore:voice:3.4.0")
    implementation ("com.mapbox.navigationcore:tripdata:3.4.0")
    implementation ("com.mapbox.navigationcore:copilot:3.4.0")
    implementation ("com.mapbox.navigationcore:ui-maps:3.4.0")
    implementation ("com.mapbox.navigationcore:android:3.4.0")
}