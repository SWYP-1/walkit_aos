package swyp.team.walkit.ui.walking.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import swyp.team.walkit.R
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.ui.components.EmotionOption
import swyp.team.walkit.ui.theme.SemanticColor

/**
 * 감정 관련 유틸리티 함수들
 * PreWalkingEmotionSelect와 PostWalkingEmotionSelect에서 공통으로 사용
 */

/**
 * 감정 타입 리스트 (위에서 아래 순서: 기쁘다 → 즐겁다 → 행복하다 → 우울하다 → 지친다 → 짜증난다)
 * 
 * UI 데이터가 아닌 순수 enum 리스트만 포함합니다.
 */
val EMOTION_TYPE_ORDER = listOf(
    EmotionType.HAPPY,
    EmotionType.JOYFUL,
    EmotionType.CONTENT,
    EmotionType.DEPRESSED,
    EmotionType.TIRED,
    EmotionType.IRRITATED
)

/**
 * EmotionType을 EmotionOption으로 변환 (Composable 함수)
 * 
 * Theme(SemanticColor), R.drawable 접근은 Composable 내부에서만 허용됩니다.
 *
 * @param emotionType 감정 타입
 * @return EmotionOption 객체
 */
@Composable
fun emotionTypeToOption(emotionType: EmotionType): EmotionOption {
    return when (emotionType) {
        EmotionType.HAPPY -> EmotionOption(
            imageResId = R.drawable.ic_circle_happy,
            label = "기쁘다",
            boxColor = SemanticColor.stateYellowTertiary,
            textColor = SemanticColor.stateYellowPrimary,
            value = emotionType.value
        )

        EmotionType.JOYFUL -> EmotionOption(
            imageResId = R.drawable.ic_circle_joyful,
            label = "즐겁다",
            boxColor = SemanticColor.stateGreenTertiary,
            textColor = SemanticColor.stateGreenPrimary,
            value = emotionType.value
        )

        EmotionType.CONTENT -> EmotionOption(
            imageResId = R.drawable.ic_circle_content,
            label = "행복하다",
            boxColor = SemanticColor.statePinkTertiary,
            textColor = SemanticColor.statePinkPrimary,
            value = emotionType.value
        )

        EmotionType.DEPRESSED -> EmotionOption(
            imageResId = R.drawable.ic_circle_depressed,
            label = "우울하다",
            boxColor = SemanticColor.stateBlueTertiary,
            textColor = SemanticColor.stateBluePrimary,
            value = emotionType.value
        )

        EmotionType.TIRED -> EmotionOption(
            imageResId = R.drawable.ic_circle_tired,
            label = "지친다",
            boxColor = SemanticColor.statePurpleTertiary,
            textColor = SemanticColor.statePurplePrimary,
            value = emotionType.value
        )

        EmotionType.IRRITATED -> EmotionOption(
            imageResId = R.drawable.ic_circle_anxious,
            label = "짜증난다",
            boxColor = SemanticColor.stateRedTertiary,
            textColor = SemanticColor.stateRedPrimary,
            value = emotionType.value
        )
    }
}

/**
 * 감정 타입 리스트를 EmotionOption 리스트로 변환 (Composable 함수)
 *
 * @param emotionTypes 감정 타입 리스트
 * @return EmotionOption 리스트
 */
@Composable
fun emotionTypesToOptions(emotionTypes: List<EmotionType>): List<EmotionOption> {
    return emotionTypes.map { emotionTypeToOption(it) }
}

/**
 * 기본 감정 옵션 리스트 생성 (EMOTION_TYPE_ORDER 사용, Composable 함수)
 *
 * @return EmotionOption 리스트
 */
@Composable
fun createDefaultEmotionOptions(): List<EmotionOption> {
    return emotionTypesToOptions(EMOTION_TYPE_ORDER)
}

/**
 * EmotionType을 value로 변환
 *
 * @param emotionType 감정 타입
 * @return 감정 값 (0-5)
 */
fun emotionToValue(emotionType: EmotionType): Int {
    return emotionType.value
}

/**
 * value를 EmotionType으로 변환
 *
 * @param value 감정 값 (0-5)
 * @return EmotionType
 */
fun valueToEmotionType(value: Int): EmotionType {
    return EmotionType.entries.find { it.value == value } ?: EmotionType.CONTENT
}

/**
 * 선택된 감정의 인덱스 찾기 (Composable 함수)
 *
 * @param selectedEmotion 선택된 감정 타입
 * @param emotionOptions 감정 옵션 리스트
 * @return 선택된 감정의 인덱스 (없으면 기본값 2)
 */
fun findSelectedEmotionIndex(
    selectedEmotion: EmotionType?,
    emotionOptions: List<EmotionOption>
): Int {
    return selectedEmotion?.let { emotion ->
        emotionOptions.indexOfFirst { it.value == emotionToValue(emotion) }
            .takeIf { it >= 0 }
    } ?: 2 // 기본값: CONTENT (인덱스 2)
}

/**
 * String을 EmotionType으로 안전하게 변환
 * 
 * @param emotionString 감정 문자열 (예: "HAPPY", "JOYFUL" 등)
 * @return 변환된 EmotionType (실패 시 기본값 CONTENT)
 */
fun stringToEmotionType(emotionString: String?): EmotionType {
    return try {
        if (emotionString.isNullOrBlank()) {
            EmotionType.CONTENT
        } else {
            EmotionType.valueOf(emotionString.uppercase())
        }
    } catch (e: Throwable) {
        EmotionType.CONTENT // 기본값
    }
}

/**
 * String을 EmotionType?으로 안전하게 변환 (null 허용)
 * 
 * @param emotionString 감정 문자열 (예: "HAPPY", "JOYFUL" 등)
 * @return 변환된 EmotionType? (실패 시 null)
 */
fun stringToEmotionTypeOrNull(emotionString: String?): EmotionType? {
    return try {
        if (emotionString.isNullOrBlank()) {
            null
        } else {
            EmotionType.valueOf(emotionString.uppercase())
        }
    } catch (e: Throwable) {
        null
    }
}

/**
 * EmotionType을 String으로 안전하게 변환
 * 
 * R8 난독화 환경에서 안전하게 enum을 String으로 변환합니다.
 * .name 속성 대신 when 표현식을 사용하여 안전하게 변환합니다.
 * 
 * @param emotionType 감정 타입
 * @return 감정 문자열 (예: "HAPPY", "JOYFUL" 등)
 */
fun emotionTypeToString(emotionType: EmotionType): String {
    return when (emotionType) {
        EmotionType.HAPPY -> "HAPPY"
        EmotionType.JOYFUL -> "JOYFUL"
        EmotionType.CONTENT -> "CONTENT"
        EmotionType.DEPRESSED -> "DEPRESSED"
        EmotionType.TIRED -> "TIRED"
        EmotionType.IRRITATED -> "IRRITATED"
    }
}




