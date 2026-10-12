plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// مفتاح Klipy لميزة GIF: من ملف klipy.key (بجانب مجلد app) أو متغير البيئة KLIPY_API_KEY (للبناء الآلي)
val klipyApiKey: String = (file("../klipy.key").takeIf { it.exists() }?.readText()?.trim()
    ?: System.getenv("KLIPY_API_KEY")?.trim()
    ?: "")

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.owlino.messageslab"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "lab-1"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "KLIPY_API_KEY", "\"$klipyApiKey\"")
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.foundation:foundation:1.7.0")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-graphics:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("com.composables:icons-lucide:1.1.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("com.airbnb.android:lottie-compose:6.5.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.0")
    implementation("io.ktor:ktor-client-core:3.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
}
