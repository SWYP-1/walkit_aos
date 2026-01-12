import com.android.build.gradle.internal.tasks.AarMetadataReader.Companion.load
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktlint.gradle)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "swyp.team.walkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "swyp.team.walkit"
        minSdk = 27
        targetSdk = 36
        versionCode = 3
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties에서 민감한 정보 읽기
        val localProperties = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                load(localPropsFile.inputStream())
            }
        }

        // BuildConfig에 API 키 추가
        val kakaoAppKey = localProperties.getProperty("KAKAO_APP_KEY", "")
        val naverClientId = localProperties.getProperty("NAVER_CLIENT_ID", "")
        val naverClientSecret = localProperties.getProperty("NAVER_CLIENT_SECRET", "")
        
        // 릴리즈 빌드 시 API 키 검증 (경고만 출력, 빌드는 계속 진행)
        if (kakaoAppKey.isBlank()) {
            println("⚠️ WARNING: KAKAO_APP_KEY가 설정되지 않았습니다. local.properties를 확인하세요.")
        }
        if (naverClientId.isBlank() || naverClientSecret.isBlank()) {
            println("⚠️ WARNING: NAVER_CLIENT_ID 또는 NAVER_CLIENT_SECRET이 설정되지 않았습니다. local.properties를 확인하세요.")
        }
        
        buildConfigField("String", "KAKAO_APP_KEY", "\"$kakaoAppKey\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"$naverClientId\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"$naverClientSecret\"")

        // AndroidManifest.xml 플레이스홀더 치환
        manifestPlaceholders["KAKAO_APP_KEY"] = localProperties.getProperty("KAKAO_APP_KEY", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            versionNameSuffix = "-debug"
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
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

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

    // Material Icons Extended - BOM 버전 사용 (버전 명시 불필요)
    implementation("androidx.compose.material:material-icons-extended")

    // KakaoMap SDK
    implementation(libs.kakao.map)

    // Google Play Services - Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Google Maps Utils - GPS 필터링 및 경로 스무딩용
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

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

    // ExifInterface (EXIF orientation 처리)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.hilt)
    ksp(libs.androidx.work.hilt.compiler)

    // KMP Date Time Picker
    implementation("network.chaintech:kmp-date-time-picker:1.1.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")

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

    // Architecture Components Test (InstantTaskExecutorRule용)
    testImplementation("androidx.arch.core:core-testing:2.2.0")

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

// google-services.json에 디버그 패키지(swyp.team.walkit.debug)가 추가되어
// 이제 디버그 빌드에서도 정상적으로 Google Services 플러그인이 작동합니다.
