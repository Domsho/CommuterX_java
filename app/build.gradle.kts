plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.facebook.android:facebook-android-sdk:latest.release")
    implementation ("com.facebook.android:facebook-login:latest.release")
    implementation ("com.google.firebase:firebase-auth:latest.release")
    implementation ("com.google.firebase:firebase-database:20.0.3")
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation ("com.google.android.gms:play-services-auth:19.2.0")
    implementation("com.google.firebase:firebase-auth")
}