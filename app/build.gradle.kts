plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Enable annotation processing for Kotlin (Room requires this)
}

android {
    namespace = "com.pranavj.satellitetrackingmount"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pranavj.satellitetrackingmount"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures{
        compose = true //Enable compose
    }
    composeOptions{
        kotlinCompilerExtensionVersion = "1.3.2"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.8.0")

    //Jetpack Compose Dependencies
    implementation(platform("androidx.compose:compose-bom:2023.03.00")) // BOM for consistent versions
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.7.0")

    //Room dependencies for database setup
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")  // Add Room KTX for coroutine support
    kapt("androidx.room:room-compiler:2.6.1") // Room annotation processor (Kotlin)

    kapt ("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0")

    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Room runtime

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("org.orekit:orekit:12.2") //orekit library

    implementation("com.mapbox.maps:android:11.7.2")
    // If you're using compose also add the compose extension
    implementation("com.mapbox.extension:maps-compose:11.7.2")


}