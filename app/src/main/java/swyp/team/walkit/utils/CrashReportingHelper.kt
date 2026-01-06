package swyp.team.walkit.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * 크래시 리포팅 헬퍼 클래스
 * 사용자 동의에 따라 크래시 리포팅 활성화/비활성화
 */
object CrashReportingHelper {

    fun setCrashReportingEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

    fun logException(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    fun logMessage(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
}
