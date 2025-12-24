package team.swyp.sdu.domain.model

/**
 * 사용자 도메인 모델
 *
 * 사용자의 기본 정보만 포함합니다.
 * Goal 정보는 별도의 Goal 모델로 분리되었습니다.
 * Character 정보는 별도의 Character 모델로 분리되었습니다.
 */
data class User(
    val imageName: String? = null,
    val nickname: String,
    val birthDate: String?, // ISO 8601 형식: "2025-12-07"
    val sex: Sex? = null,
) {
    companion object {
        val EMPTY = User(
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


