plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.TusharGoyal.imgtopdf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.TusharGoyal.imgtopdf"
        minSdk = 26
        targetSdk = 35
        versionCode = 20
        versionName = "2.9"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation ("com.karumi:dexter:6.2.3")



    implementation ("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")


   // implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation ("com.unity3d.ads:unity-ads:4.9.2") // check for latest version












}