plugins {
    id("com.android.library")
}

android {
    namespace = "com.novastore.novalicense"
    // 34 (y no 36) para que el AAR lo pueda consumir cualquier proyecto Unity 6
    // sin pedirle que suba su compileSdk.
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
