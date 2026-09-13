plugins { id("com.android.application") }
android { namespace="com.steve.apmdiag"; compileSdk=35
 defaultConfig { applicationId="com.steve.apmdiag"; minSdk=24; targetSdk=30; versionCode=1; versionName="1"; ndk { abiFilters += "armeabi-v7a" } }
 externalNativeBuild { cmake { path=file("src/main/cpp/CMakeLists.txt") } }
}
