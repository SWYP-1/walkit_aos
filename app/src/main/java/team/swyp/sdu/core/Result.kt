package team.swyp.sdu.core

/**
 * 공통 Result 타입
 *
 * 네트워크/로컬 작업에서 일관된 상태 표현을 위해 사용한다.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>
    data object Loading : Result<Nothing>
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(exception, message)
        Result.Loading -> Result.Loading
    }

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T> {
    if (this is Result.Error) action(exception, message)
    return this
}
