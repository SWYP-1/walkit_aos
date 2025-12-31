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
) {
    // 긍정 감정
    HAPPY("기쁨", "Happy"),
    JOYFUL("즐거움", "Joyful"),
    CONTENT("행복함", "Content"),

    // 부정 감정
    DEPRESSED("우울함", "Depressed"),
    TIRED("지침", "Tired"),
    IRRITATED("짜증남", "Irritated");
}

