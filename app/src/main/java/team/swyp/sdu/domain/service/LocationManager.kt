package team.swyp.sdu.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import team.swyp.sdu.data.model.LocationPoint
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 단순 위치 요청을 위한 LocationManager
 *
 * 연속 위치 추적이 아닌, 일회성 위치 요청을 처리합니다.
 * - 홈화면에서 현재 위치 표시
 * - 지도 썸네일에서 현재 위치 표시
 * - 기타 일회성 위치 요청
 *
 * 연속 위치 추적이 필요한 경우는 LocationTrackingService를 사용하세요.
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        /**
         * 위치 정보의 최대 유효 시간 (1시간)
         */
        private const val MAX_LOCATION_AGE = 60 * 60 * 1000L // 1시간

        /**
         * 위치 정보의 최대 허용 정확도 (2km)
         */
        private const val MAX_ACCURACY = 2000f // 2km

        /**
         * 성능 측정 로그 태그
         */
        private const val TAG_PERFORMANCE = "HomePerformance"
    }

    /**
     * 위치 권한이 있는지 확인
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 현재 위치를 가져옵니다 (고정밀도)
     *
     * @return LocationPoint 또는 null (권한 없음 또는 위치를 가져올 수 없음)
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): LocationPoint? {
        if (!hasLocationPermission()) {
            Timber.w("위치 권한이 없어서 현재 위치를 가져올 수 없습니다")
            return null
        }

        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val task: Task<Location> = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token,
            )

            // Task를 코루틴으로 변환
            val location = task.await()

            location?.let {
                LocationPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.time,
                    accuracy = it.accuracy,
                )
            }
        } catch (t: Throwable) {
            Timber.e(t, "현재 위치 가져오기 실패")
            null
        }
    }

    /**
     * 마지막으로 알려진 위치를 가져옵니다
     *
     * getCurrentLocation()이 실패하거나 null을 반환할 때 사용할 수 있습니다.
     *
     * @return LocationPoint 또는 null (권한 없음 또는 위치를 가져올 수 없음)
     */
    suspend fun getLastLocation(): LocationPoint? {
        if (!hasLocationPermission()) {
            Timber.w("위치 권한이 없어서 마지막 위치를 가져올 수 없습니다")
            return null
        }

        return try {
            val task: Task<Location> = fusedLocationClient.lastLocation
            
            // Task를 코루틴으로 변환
            val location = task.await()

            location?.let {
                LocationPoint(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.time,
                    accuracy = it.accuracy,
                )
            }
        } catch (t: Throwable) {
            Timber.e(t, "마지막 위치 가져오기 실패")
            null
        }
    }

    /**
     * 위치 정보가 사용 가능한지 확인합니다
     *
     * @param location 확인할 위치 정보
     * @return 위치가 1시간 이내이고 정확도가 2km 이내이면 true
     */
    private fun isUsable(location: LocationPoint): Boolean {
        val age = System.currentTimeMillis() - location.timestamp
        val isRecent = age <= MAX_LOCATION_AGE
        
        // accuracy가 null이면 정확도 정보가 없는 것으로 간주하여 사용 가능
        val isAccurate = location.accuracy == null || location.accuracy <= MAX_ACCURACY
        
        return isRecent && isAccurate
    }

    /**
     * 현재 위치를 가져오고, 실패하면 마지막 위치를 반환합니다
     * 위치 정보의 유효성을 검사하여 오래되었거나 정확도가 낮은 위치는 제외합니다.
     * 
     * 성능 최적화: lastLocation은 즉시 반환되므로 먼저 확인하고,
     * 유효하지 않거나 없을 때만 getCurrentLocation()을 호출합니다.
     *
     * @return LocationPoint 또는 null (모든 시도 실패 또는 유효하지 않은 위치)
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocationOrLast(): LocationPoint? {
        val startTime = System.currentTimeMillis()
        
        // 먼저 마지막 위치 확인 (즉시 반환)
        val lastLocationStartTime = System.currentTimeMillis()
        val lastLocation = getLastLocation()
        val lastLocationElapsedTime = System.currentTimeMillis() - lastLocationStartTime
        
        if (lastLocation != null && isUsable(lastLocation)) {
            val elapsedTime = System.currentTimeMillis() - startTime
            Timber.tag(TAG_PERFORMANCE).d("위치 획득 완료 (마지막 위치, 즉시): ${elapsedTime}ms (lastLocation: ${lastLocationElapsedTime}ms), lat=${lastLocation.latitude}, lon=${lastLocation.longitude}, accuracy=${lastLocation.accuracy}m")
            return lastLocation
        }
        
        // 마지막 위치가 유효하지 않거나 없으면 현재 위치 요청 (시간 소요)
        val currentLocationStartTime = System.currentTimeMillis()
        val currentLocation = getCurrentLocation()
        val currentLocationElapsedTime = System.currentTimeMillis() - currentLocationStartTime
        
        if (currentLocation != null && isUsable(currentLocation)) {
            val elapsedTime = System.currentTimeMillis() - startTime
            Timber.tag(TAG_PERFORMANCE).d("위치 획득 완료 (현재 위치): ${elapsedTime}ms (lastLocation: ${lastLocationElapsedTime}ms, getCurrentLocation: ${currentLocationElapsedTime}ms), lat=${currentLocation.latitude}, lon=${currentLocation.longitude}, accuracy=${currentLocation.accuracy}m")
            return currentLocation
        }
        
        // 유효한 위치를 찾지 못한 경우
        val elapsedTime = System.currentTimeMillis() - startTime
        Timber.tag(TAG_PERFORMANCE).w("위치 획득 실패: ${elapsedTime}ms (lastLocation: ${lastLocationElapsedTime}ms, getCurrentLocation: ${currentLocationElapsedTime}ms, 유효한 위치를 찾지 못함)")
        return null
    }

    /**
     * Task를 코루틴으로 변환하는 확장 함수
     * 
     * Google Play Services의 Task API를 코루틴으로 변환합니다.
     * Task의 addOnCompleteListener를 사용하여 코루틴으로 변환합니다.
     */
    private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                val exception = task.exception
                if (exception != null) {
                    continuation.resumeWithException(exception)
                } else {
                    // Task가 취소된 경우
                    continuation.resume(null)
                }
            }
        }
        
        // 코루틴이 취소되면 Task도 취소
        continuation.invokeOnCancellation {
            // Task 취소는 자동으로 처리됨
        }
    }
}

