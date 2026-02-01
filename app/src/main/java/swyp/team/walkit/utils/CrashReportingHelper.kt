package swyp.team.walkit.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.IOException
import retrofit2.HttpException

/**
 * 크래시 리포팅 헬퍼 클래스
 * 사용자 동의에 따라 크래시 리포팅 활성화/비활성화
 * 
 * ## 예외 처리 가이드
 * 
 * ### Non-Fatal로 기록해야 하는 경우 (복구 가능한 예외)
 * - 네트워크 오류 (IOException)
 * - 서버 오류 (HttpException 5xx)
 * - 사용자 입력 검증 실패
 * - 권한 거부
 * 
 * ### Fatal로 기록해야 하는 경우 (치명적 오류)
 * - NullPointerException
 * - IllegalStateException
 * - ClassCastException
 * - 초기화 실패
 * 
 * **중요**: 치명적 오류는 catch하지 않고 크래시를 허용해야 합니다.
 * 이 메서드는 복구 가능한 예외만 Non-Fatal로 기록하는 데 사용하세요.
 * 
 * @see docs/07-exception-handling-strategy.md
 */
object CrashReportingHelper {

    fun setCrashReportingEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

    /**
     * 복구 가능한 예외를 Non-Fatal로 기록합니다.
     * 
     * **사용 시 주의사항**:
     * - 네트워크 오류, 서버 오류 등 복구 가능한 예외만 기록
     * - 치명적 오류(NullPointerException, IllegalStateException 등)는 
     *   catch하지 않고 크래시를 허용해야 함
     * 
     * @param throwable 기록할 예외 (복구 가능한 예외만)
     */
    fun logException(throwable: Throwable) {
        // 치명적 오류인 경우 경고 로그만 남기고 기록하지 않음
        // (실제로는 이런 경우가 발생하면 안 되지만, 방어적 코딩)
        when (throwable) {
            is NullPointerException,
            is IllegalStateException,
            is ClassCastException -> {
                FirebaseCrashlytics.getInstance().log(
                    "WARNING: 치명적 오류가 logException으로 전달됨. " +
                    "이 예외는 catch하지 않고 크래시를 허용해야 합니다: ${throwable.javaClass.simpleName}"
                )
            }
            else -> {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        }
    }

    /**
     * 네트워크 오류를 Non-Fatal로 기록합니다.
     * 
     * @param exception 네트워크 관련 예외
     * @param context 예외가 발생한 컨텍스트 (예: "getUserData", "saveWalk")
     */
    fun logNetworkError(exception: IOException, context: String) {
        FirebaseCrashlytics.getInstance().log("Network error in $context: ${exception.message}")
        FirebaseCrashlytics.getInstance().recordException(exception)
    }

    /**
     * HTTP 오류를 Non-Fatal로 기록합니다.
     * 
     * @param exception HTTP 예외
     * @param context 예외가 발생한 컨텍스트
     */
    fun logHttpError(exception: HttpException, context: String) {
        FirebaseCrashlytics.getInstance().log(
            "HTTP error in $context: ${exception.code()}, ${exception.message()}"
        )
        FirebaseCrashlytics.getInstance().recordException(exception)
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
