plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.lab.wallet.offline"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.lab.wallet.offline"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-offline"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { getByName("debug") { isDebuggable = true } }
}
dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    testImplementation("junit:junit:4.13.2")
}
