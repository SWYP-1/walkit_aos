package team.swyp.sdu.domain.model

/**
 * 사용자 도메인 모델
 *
 * 사용자의 기본 정보만 포함합니다.
 * Goal 정보는 별도의 Goal 모델로 분리되었습니다.
 * Character 정보는 별도의 Character 모델로 분리되었습니다.
 */
data class User(
    val userId: Long,
    val imageName: String? = null,
    val nickname: String,
    val birthDate: String?, // ISO 8601 형식: "2025-12-07"
    val sex: Sex? = null,
) {
    /**
     * 온보딩 완료 여부 (닉네임이 빈 문자열이면 온보딩 미완료로 판단)
     */
    val isOnboarded: Boolean
        get() = nickname.isNotEmpty()
    companion object {
        val EMPTY = User(
            userId = 0L,
            imageName = null,
            nickname = "",
            birthDate = "",
            sex = null,
        )
    }
}

/**
 * 성별 enum
 */
enum class Sex {
    MALE,
    FEMALE,
}


