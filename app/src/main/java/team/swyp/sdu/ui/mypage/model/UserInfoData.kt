package team.swyp.sdu.ui.mypage.model

import team.swyp.sdu.domain.model.Grade

/**
 * 사용자 정보 데이터
 */
data class UserInfoData(
    val nickname: String,
    val profileImageUrl: String? = null,
    val grade: Grade?
)

