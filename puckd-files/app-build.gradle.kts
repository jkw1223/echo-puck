plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.steve.puckd"
    buildFeatures {
        buildConfig = true
    }
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.steve.puckd"
        minSdk = 30
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        val relayUrl = providers.gradleProperty("relayUrl")
            .orElse("ws://192.168.1.30:8787/puck")
            .get()
        buildConfigField("String", "RELAY_URL", "\"$relayUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
