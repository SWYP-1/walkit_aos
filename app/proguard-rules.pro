# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================
# 기본 설정
# ============================================
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ============================================
# Kotlin
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ============================================
# Kotlinx Serialization
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class team.swyp.sdu.**$$serializer { *; }
-keepclassmembers class team.swyp.sdu.** {
    *** Companion;
}
-keepclasseswithmembers class team.swyp.sdu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializable classes
-keep @kotlinx.serialization.Serializable class team.swyp.sdu.** { *; }

# ============================================
# Kotlinx Serialization Enum 보호
# ============================================
# @Serializable 어노테이션이 있는 enum 클래스 보호
-keep @kotlinx.serialization.Serializable enum team.swyp.sdu.** {
    *;
}

# @SerialName 어노테이션이 있는 enum 값 보호
-keepclassmembers @kotlinx.serialization.Serializable enum team.swyp.sdu.** {
    @kotlinx.serialization.SerialName <fields>;
}

# enum의 values()와 valueOf() 메서드 보호 (직렬화/역직렬화에 필요)
-keepclassmembers @kotlinx.serialization.Serializable enum team.swyp.sdu.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# enum의 모든 필드와 메서드 보호
-keepclassmembers @kotlinx.serialization.Serializable enum team.swyp.sdu.** {
    <fields>;
    <methods>;
}

# ============================================
# Kotlin Parcelize
# ============================================
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class team.swyp.sdu.** implements android.os.Parcelable {
    static ** CREATOR;
}

# ============================================
# Retrofit & OkHttp
# ============================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ============================================
# Hilt (Dagger)
# ============================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @dagger.hilt.** <methods>;
}
-keepclassmembers class * {
    @javax.inject.** <methods>;
}
-keepclassmembers class * {
    @dagger.** <methods>;
}

# Hilt EntryPoint 보호
-keep @dagger.hilt.EntryPoint interface team.swyp.sdu.** { *; }

# Hilt AndroidEntryPoint 보호
-keep @dagger.hilt.android.AndroidEntryPoint class team.swyp.sdu.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.app.Activity { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.app.Service { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.content.BroadcastReceiver { *; }

# Hilt ViewModel 보호
-keep @dagger.hilt.android.lifecycle.HiltViewModel class team.swyp.sdu.** { *; }

# ============================================
# Room Database
# ============================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class team.swyp.sdu.** { *; }
-keep class team.swyp.sdu.**_Impl { *; }
-keep class team.swyp.sdu.**_Impl$* { *; }

# ============================================
# Jetpack Compose
# ============================================
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**

# Navigation Compose 보호
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Navigation Screen (sealed class) 보호
-keep class team.swyp.sdu.navigation.Screen { *; }
-keep class team.swyp.sdu.navigation.Screen$* { *; }

# ============================================
# Lottie
# ============================================
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# LottieImageProcessor 보호 (JSONObject 사용)
-keep class team.swyp.sdu.domain.service.LottieImageProcessor { *; }
-keep class org.json.JSONObject { *; }
-keep class org.json.JSONArray { *; }

# ============================================
# Coil
# ============================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================
# Kakao SDK
# ============================================
-keep class com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**
-keep class com.kakao.maps.** { *; }
-dontwarn com.kakao.maps.**

# ============================================
# Naver OAuth SDK
# ============================================
-keep class com.nhn.android.naverlogin.** { *; }
-dontwarn com.nhn.android.naverlogin.**

# ============================================
# Google Play Services - Location
# ============================================
# Location Services 클래스 유지
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.internal.location.** { *; }

# 내부 클래스 및 Companion 객체 보호
-keepclassmembers class com.google.android.gms.internal.location.** {
    *;
}
-keepclassmembers class com.google.android.gms.internal.location.**$* {
    *;
}

# Companion 객체 명시적 보호
-keepclassmembers class com.google.android.gms.internal.location.** {
    public static ** Companion;
}
-keepclassmembers class com.google.android.gms.internal.location.**$Companion {
    *;
}

-dontwarn com.google.android.gms.location.**
-dontwarn com.google.android.gms.internal.location.**

# Google Play Services 공통 규칙
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Location Tracking Service 보호
-keep class team.swyp.sdu.domain.service.LocationTrackingService { *; }
-keep class team.swyp.sdu.domain.service.LocationTrackingService$* { *; }
-keep class team.swyp.sdu.domain.service.ActivityRecognitionManager { *; }
-keep class team.swyp.sdu.domain.service.ActivityRecognitionManager$* { *; }

# BroadcastReceiver 보호 (PendingIntent에서 사용)
-keep class * extends android.content.BroadcastReceiver { *; }
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public <init>(...);
    public void onReceive(android.content.Context, android.content.Intent);
}

# ============================================
# Google Maps Utils
# ============================================
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.maps.android.**

# ============================================
# Accompanist
# ============================================
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ============================================
# KMP Date Time Picker
# ============================================
-keep class network.chaintech.** { *; }
-dontwarn network.chaintech.**

# ============================================
# Google Play Billing
# ============================================
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ============================================
# WorkManager
# ============================================
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# WorkManager Worker 보호
-keep class team.swyp.sdu.worker.** { *; }
-keepclassmembers class team.swyp.sdu.worker.** {
    *;
}

# Hilt Worker 보호
-keep @androidx.hilt.work.HiltWorker class team.swyp.sdu.** { *; }

# ============================================
# DataStore
# ============================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ============================================
# Firebase Crashlytics
# ============================================
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Firebase Messaging Service 보호
-keep class team.swyp.sdu.domain.service.WalkItFirebaseMessagingService { *; }

# ============================================
# 프로젝트 특정 클래스 유지
# ============================================
# DTO 클래스들 유지
-keep class team.swyp.sdu.data.remote.** { *; }
-keep class team.swyp.sdu.data.model.** { *; }
-keep class team.swyp.sdu.domain.model.** { *; }

# ViewModel 유지
-keep class team.swyp.sdu.**ViewModel { *; }
-keep class team.swyp.sdu.**ViewModel$* { *; }

# Application 클래스 유지
-keep class team.swyp.sdu.WalkingBuddyApplication { *; }

# ============================================
# Enum 클래스 보호 (Enum.valueOf() 사용)
# ============================================
-keepclassmembers enum team.swyp.sdu.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Enum 클래스 전체 보호
-keep enum team.swyp.sdu.data.model.EmotionType { *; }
-keepclassmembers enum team.swyp.sdu.data.model.EmotionType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep enum team.swyp.sdu.data.local.entity.SyncState { *; }
-keep enum team.swyp.sdu.domain.model.Grade { *; }
-keep enum team.swyp.sdu.domain.service.ActivityType { *; }
-keep enum team.swyp.sdu.data.remote.walking.dto.Grade { *; }

# ============================================
# Sealed Class 보호
# ============================================
-keep class team.swyp.sdu.domain.model.WearState { *; }
-keep class team.swyp.sdu.domain.model.WearState$* { *; }
-keepclassmembers class team.swyp.sdu.domain.model.WearState {
    *;
}

# ============================================
# Room TypeConverter 보호
# ============================================
-keep class team.swyp.sdu.data.local.database.Converters { *; }
-keepclassmembers class team.swyp.sdu.data.local.database.Converters {
    *;
}

# Room Database 보호
-keep class team.swyp.sdu.data.local.database.AppDatabase { *; }
-keep class team.swyp.sdu.data.local.database.AppDatabase$* { *; }

# Room DAO 인터페이스 보호
-keep interface team.swyp.sdu.data.local.dao.** { *; }
-keep class team.swyp.sdu.data.local.dao.** { *; }

# Room DAO 내부 데이터 클래스 보호 (RecentSessionEmotion, EmotionCount 등)
-keep class team.swyp.sdu.data.local.dao.**$* { *; }

# Room Entity 보호
-keep @androidx.room.Entity class team.swyp.sdu.data.local.entity.** { *; }
-keep class team.swyp.sdu.data.local.entity.** { *; }

# ============================================
# EnumConverter 유틸리티 보호 (Enum.valueOf 사용)
# ============================================
-keep class team.swyp.sdu.data.utils.EnumConverter { *; }
-keepclassmembers class team.swyp.sdu.data.utils.EnumConverter {
    *;
}

# ============================================
# 리플렉션 사용 클래스
# ============================================
-keepclassmembers class team.swyp.sdu.** {
    @kotlinx.serialization.SerialName <fields>;
}

# @Keep 어노테이션 사용 클래스
-keep @androidx.annotation.Keep class team.swyp.sdu.** { *; }
-keepclassmembers class team.swyp.sdu.** {
    @androidx.annotation.Keep *;
}

# ============================================
# Companion Object 보호 (리플렉션 접근 가능성)
# ============================================
-keepclassmembers class team.swyp.sdu.** {
    public static ** Companion;
}
-keepclassmembers class team.swyp.sdu.**$Companion {
    *;
}

# ============================================
# Object 싱글톤 보호
# ============================================
-keep class team.swyp.sdu.** {
    public static ** INSTANCE;
}

# ============================================
# 데이터 클래스 보호 (Parcelize, Serialization)
# ============================================
-keep @kotlinx.parcelize.Parcelize class team.swyp.sdu.** { *; }
-keep @kotlinx.serialization.Serializable class team.swyp.sdu.** { *; }

# ============================================
# Mapper 클래스 보호
# ============================================
-keep class team.swyp.sdu.**Mapper { *; }
-keep class team.swyp.sdu.**Mapper$* { *; }
-keep class team.swyp.sdu.data.**.mapper.** { *; }