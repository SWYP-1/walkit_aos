package team.swyp.sdu.data.remote.user.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.Sex

/**
 * 사용자 API 응답 DTO
 *
 * 새로운 서버 API 구조에 맞춘 DTO입니다.
 * Kotlinx Serialization을 사용합니다.
 */
@Serializable
data class RemoteUserDto(
    @SerialName("imageName")
    val imageName: String? = null,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("birthDate")
    val birthDate: String?,
    @SerialName("sex")
    val sex: String? = null,
) {
    fun toDomain(): User = User(
        imageName = imageName,
        nickname = nickname,
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
