package team.swyp.sdu.data.remote.user

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import team.swyp.sdu.data.remote.user.dto.RemoteUserDto
import team.swyp.sdu.domain.model.UserProfile

/**
 * 사용자 정보를 서버에서 가져오는 스텁 데이터 소스
 *
 * TODO: 실제 Retrofit API 호출로 교체
 */
@Singleton
class UserRemoteDataSource @Inject constructor(
    // 추후 Retrofit 서비스를 주입받도록 확장 예정
) {
    suspend fun fetchUserProfile(): UserProfile {
        // TODO: API 명세가 나오면 실제 호출로 대체
        delay(150) // 최소한의 비동기 흐름 유지
        val stub =
            RemoteUserDto(
                uid = "demo-uid",
                nickname = "게스트",
                clearedCount = 0,
                point = 0,
                goalKmPerWeek = 20.0,
            )
        return stub.toDomain()
    }
}
