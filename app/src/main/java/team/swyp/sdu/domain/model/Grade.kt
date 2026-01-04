package team.swyp.sdu.domain.model

/**
 * 캐릭터 등급 (도메인 모델)
 *
 * 비즈니스 로직에서 사용하는 캐릭터 등급을 나타냅니다.
 * 각 등급은 고정된 레벨을 가지며, 게임 내 성장 시스템의 일부입니다.
 *
 * @property level 등급에 해당하는 레벨 값
 * @property displayName UI 표시용 이름
 */
enum class Grade(val level: Int, val displayName: String) {
    SEED(level = 1, displayName = "씨앗"),
    SPROUT(level = 2, displayName = "새싹"),
    TREE(level = 3, displayName = "나무");

    companion object {
        /**
         * 레벨로 등급을 찾습니다.
         */
        fun fromLevel(level: Int): Grade? {
            return values().find { it.level == level }
        }

        /**
         * API DTO Grade를 도메인 Grade로 변환합니다.
         */
        fun fromApiGrade(apiGrade: team.swyp.sdu.data.remote.walking.dto.Grade): Grade {
            return when (apiGrade) {
                team.swyp.sdu.data.remote.walking.dto.Grade.SEED -> SEED
                team.swyp.sdu.data.remote.walking.dto.Grade.SPROUT -> SPROUT
                team.swyp.sdu.data.remote.walking.dto.Grade.TREE -> TREE
            }
        }

        /**
         * API String을 도메인 Grade로 변환합니다.
         */
        fun fromApiString(apiGrade: String): Grade {
            return when (apiGrade.uppercase()) {
                "SEED" -> SEED
                "SPROUT" -> SPROUT
                "TREE" -> TREE
                else -> SEED // 기본값
            }
        }

        /**
         * 도메인 Grade를 API DTO Grade로 변환합니다.
         */
        fun toApiGrade(domainGrade: Grade): team.swyp.sdu.data.remote.walking.dto.Grade {
            return when (domainGrade) {
                SEED -> team.swyp.sdu.data.remote.walking.dto.Grade.SEED
                SPROUT -> team.swyp.sdu.data.remote.walking.dto.Grade.SPROUT
                TREE -> team.swyp.sdu.data.remote.walking.dto.Grade.TREE
            }
        }
    }
}
