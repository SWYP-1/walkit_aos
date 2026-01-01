package team.swyp.sdu.domain.model

import team.swyp.sdu.data.model.LocationPoint

/**
 * 산책 기록 도메인 모델
 */
data class WalkRecord(
    val id: Long = 0,
    val preWalkEmotion: String = "JOYFUL",
    val postWalkEmotion: String = "JOYFUL",
    val note: String? = null,
    val imageUrl: String? = null,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val totalTime: Long = 0,
    val stepCount: Int = 0,
    val totalDistance: Double = 0.0,
    val createdDate: String? = null,
    val points: List<LocationPoint> = emptyList(),
)







