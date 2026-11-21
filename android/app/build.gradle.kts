plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zirakusa.calculadora"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zirakusa.calculadora"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0"
        // Modo MCP: the same public relay as Stone (one device id per app). Empty = no MCP over the internet.
        val relayUrl = (project.findProperty("calcRelayUrl") as String?).orEmpty().trimEnd('/')
        buildConfigField("String", "RELAY_URL", "\"$relayUrl\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed like Stone and Gateway Hub, so the hub recognises the calculator as the same developer.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Screenshots of the screens land in app/build/screenshots .
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    // The floating window is drawn by a service (Compose outside an Activity).
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    // Modo MCP: WebSocket to the relay (same library and version as Stone and Luma).
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
