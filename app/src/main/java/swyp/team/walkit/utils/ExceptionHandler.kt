package swyp.team.walkit.utils

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import swyp.team.walkit.core.Result
import timber.log.Timber
import java.io.IOException

/**
 * 예외 처리 유틸리티 함수
 * 
 * 문서: docs/07-exception-handling-strategy.md
 * 
 * 이 유틸리티는 복구 가능한 예외만 처리하고, 치명적 오류는 크래시를 허용합니다.
 */
object ExceptionHandler {

    /**
     * 네트워크/서버 작업에서 발생한 예외를 Result.Error로 변환합니다.
     * 
     * 복구 가능한 예외만 처리하고, 치명적 오류는 다시 던져서 크래시를 허용합니다.
     * 
     * @param context 예외가 발생한 컨텍스트 (예: "getUserData", "saveWalk")
     * @param block 실행할 suspend 함수
     * @return Result.Success 또는 Result.Error
     */
    suspend fun <T> handleNetworkOperation(
        context: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: CancellationException) {
            // 코루틴 취소: 정상적인 취소이므로 다시 던짐
            throw e
        } catch (e: IOException) {
            // 네트워크 오류: 복구 가능
            CrashReportingHelper.logNetworkError(e, context)
            Timber.e(e, "$context 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            // HTTP 오류: 복구 가능
            CrashReportingHelper.logHttpError(e, context)
            val errorMessage = when (e.code()) {
                in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
                in 400..499 -> "요청을 처리할 수 없습니다"
                else -> "오류가 발생했습니다"
            }
            Timber.e(e, "$context 실패: HTTP ${e.code()}")
            Result.Error(e, errorMessage)
        }
        // NullPointerException, IllegalStateException 등 치명적 오류는 catch하지 않음
        // → 크래시로 이어져서 개발자가 즉시 수정 가능
    }

    /**
     * 로컬 작업(데이터베이스, 파일 등)에서 발생한 예외를 Result.Error로 변환합니다.
     * 
     * 복구 가능한 예외만 처리하고, 치명적 오류는 다시 던져서 크래시를 허용합니다.
     * 
     * @param context 예외가 발생한 컨텍스트
     * @param block 실행할 suspend 함수
     * @return Result.Success 또는 Result.Error
     */
    suspend fun <T> handleLocalOperation(
        context: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: CancellationException) {
            // 코루틴 취소: 정상적인 취소이므로 다시 던짐
            throw e
        } catch (e: IOException) {
            // 파일/IO 오류: 복구 가능
            CrashReportingHelper.logException(e)
            Timber.e(e, "$context 실패: IO 오류")
            Result.Error(e, "파일 처리 중 오류가 발생했습니다")
        } catch (e: java.io.FileNotFoundException) {
            // 파일 없음: 복구 가능
            Timber.w("$context: 파일을 찾을 수 없음")
            Result.Error(e, "파일을 찾을 수 없습니다")
        }
        // NullPointerException, IllegalStateException 등 치명적 오류는 catch하지 않음
        // → 크래시로 이어져서 개발자가 즉시 수정 가능
    }

    /**
     * 예외가 치명적인지 확인합니다.
     * 
     * @param throwable 확인할 예외
     * @return 치명적이면 true
     */
    fun isFatalException(throwable: Throwable): Boolean {
        return when (throwable) {
            is NullPointerException,
            is IllegalStateException,
            is ClassCastException,
            is IllegalArgumentException -> true
            is CancellationException -> false // 코루틴 취소는 정상
            is IOException,
            is HttpException -> false // 네트워크/서버 오류는 복구 가능
            else -> throwable is Error // Error 타입은 치명적
        }
    }
}
