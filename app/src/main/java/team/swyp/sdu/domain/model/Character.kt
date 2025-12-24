package team.swyp.sdu.domain.model

import team.swyp.sdu.data.remote.walking.dto.Grade

/**
 * 캐릭터 도메인 모델
 */
data class Character(
    val headImageName: String? = null,
    val bodyImageName: String? = null,
    val feetImageName: String? = null,
    val characterImageName: String? = null,
    val backgroundImageName: String? = null,
    val level: Int = 1,
    val grade: Grade = Grade.SEED,
    val nickName: String? = null,
)



