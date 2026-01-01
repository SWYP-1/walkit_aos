package team.swyp.sdu.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 감정 데이터 모델
 *
 * @param type 감정 타입 (예: HAPPY, SAD, EXCITED, CALM, TIRED 등)
 * @param timestamp 감정 기록 시간 (밀리초)
 * @param note 감정에 대한 메모 (선택사항)
 */
@Parcelize
@Serializable
data class Emotion(
    val type: EmotionType,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
) : Parcelable

/**
 * 감정 타입 열거형
 */
@Serializable
enum class EmotionType(
    val ko: String,
    val en: String,
    val priority: Int, // 낮을수록 높은 우선순위 (1이 가장 높음)
) {
    // 긍정 감정
    HAPPY("기쁨", "Happy", 1),        // 1위: 긍정 - 기쁘다
    JOYFUL("즐거움", "Joyful", 2),    // 2위: 긍정 - 즐겁다
    CONTENT("행복함", "Content", 3),  // 3위: 긍정 - 행복하다

    // 부정 감정
    DEPRESSED("우울함", "Depressed", 4), // 4위: 부정 - 우울하다
    TIRED("지침", "Tired", 5),       // 5위: 부정 - 지친다
    IRRITATED("짜증남", "Irritated", 6); // 6위: 부정 - 짜증난다
}

