plugins {
    id("com.android.application")
}

android {
    namespace = "com.novastore.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.novastore.sdktestkotlin"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":novalicense"))
}
