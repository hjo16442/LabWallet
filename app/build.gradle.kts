plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.lab.wallet"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.lab.wallet"
        minSdk = 26          // PBKDF2WithHmacSHA256 از API 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-lab"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { getByName("debug") { isDebuggable = true } }
    lint { abortOnError = false }   // لب عمداً پر از آنتی‌پترن است
}
