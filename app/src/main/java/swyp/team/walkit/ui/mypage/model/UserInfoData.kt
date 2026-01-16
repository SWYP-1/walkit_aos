package swyp.team.walkit.ui.mypage.model

import swyp.team.walkit.domain.model.Grade

/**
 * 사용자 정보 데이터
 */
data class UserInfoData(
    val nickname: String,
    val profileImageUrl: String? = null,
    val grade: Grade?,
    val level: Int? = null
)

