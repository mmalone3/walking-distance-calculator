plugins {
    id("com.android.application")
}

android {
    namespace = "com.amica.walkingdistance"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.amica.walkingdistance"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
