plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktlint.gradle)
    id("kotlin-parcelize")
}

android {
    namespace = "team.swyp.sdu"
    compileSdk = 36

    defaultConfig {
        applicationId = "team.swyp.sdu"
        minSdk = 27
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(false)

    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Coil - 이미지 로딩
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    // Lottie
    implementation(libs.lottie.compose)

    // Timber - 로깅
    implementation(libs.timber)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Accompanist - Compose 유틸리티
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    // Material Icons Extended
    implementation(
        "androidx.compose.material:material-icons-extended:${libs.versions.composeBom.get().substringAfterLast(".")}",
    )

    // KakaoMap SDK
    implementation(libs.kakao.map)

    // Google Play Services - Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Calendar Library
    implementation(libs.calendar.compose)

    // Kakao SDK
    implementation(libs.kakao.sdk)

    // Naver OAuth SDK
    implementation(libs.naver.oauth)

    // Google Play Billing
    implementation(libs.billing.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // JUnit 4 (기존 호환성)
    testImplementation(libs.junit)

    // JUnit 5 (Jupiter) - Unit Tests
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)

    // MockK - Mocking framework
    testImplementation(libs.mockk)

    // Coroutines Test
    testImplementation(libs.kotlinx.coroutines.test)

    // Android Test
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk) // MockK for Android tests
    androidTestImplementation("io.mockk:mockk-android:1.13.10") // MockK Android support
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Pre-PR 체크 태스크 (Cursor YOLO 모드용)
tasks.register("prePR") {
    group = "verification"
    description = "PR 전 체크: 빌드, 테스트, Lint"
    dependsOn("build", "test", "lint")
}
