package team.swyp.sdu.data.remote.user.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.UserProfile
import team.swyp.sdu.domain.model.Sex

/**
 * 사용자 API 응답 DTO
 *
 * 새로운 서버 API 구조에 맞춘 DTO입니다.
 * Kotlinx Serialization을 사용합니다.
 */
@Serializable
data class RemoteUserDto(
    @SerialName("userId")
    val userId: Long,
    @SerialName("imageName")
    val imageName: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("birthDate")
    val birthDate: String?,
    @SerialName("sex")
    val sex: String? = null,
) {
    fun toDomain(): User = User(
        userId = userId,
        imageName = imageName,
        nickname = nickname.orEmpty(), // null이면 빈 문자열로 변환
        birthDate = birthDate,
        sex = sex?.let {
            try {
                Sex.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        },
    )
}
