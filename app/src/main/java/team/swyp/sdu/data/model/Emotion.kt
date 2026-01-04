package team.swyp.sdu.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 감정 타입 열거형
 * 
 * 순수 데이터만 포함합니다. UI 관련 데이터(Color, Drawable 등)는 포함하지 않습니다.
 */
enum class EmotionType(val value: Int) {
    // 긍정 감정
    HAPPY(5),        // 기쁘다
    JOYFUL(4),       // 즐겁다
    CONTENT(3),      // 행복하다

    // 부정 감정
    DEPRESSED(2),    // 우울하다
    TIRED(1),        // 지친다
    IRRITATED(0);    // 짜증난다
}

