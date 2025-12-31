package team.swyp.sdu.ui.walking.utils

import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.EmotionOption
import team.swyp.sdu.ui.theme.SemanticColor

/**
 * 감정 관련 유틸리티 함수들
 * PreWalkingEmotionSelect와 PostWalkingEmotionSelect에서 공통으로 사용
 */

/**
 * 감정 타입 리스트 (위에서 아래 순서: 기쁘다 → 즐겁다 → 행복하다 → 우울하다 → 지친다 → 짜증난다)
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
 * EmotionType을 EmotionOption으로 변환
 *
 * @param emotionType 감정 타입
 * @return EmotionOption 객체
 */
fun emotionTypeToOption(emotionType: EmotionType): EmotionOption {
    return when (emotionType) {
        EmotionType.HAPPY -> EmotionOption(
            imageResId = R.drawable.ic_circle_happy,
            label = "기쁘다",
            boxColor = SemanticColor.stateYellowTertiary,
            textColor = SemanticColor.stateYellowPrimary,
            value = 5
        )

        EmotionType.JOYFUL -> EmotionOption(
            imageResId = R.drawable.ic_circle_joyful,
            label = "즐겁다",
            boxColor = SemanticColor.stateGreenPrimary,
            textColor = SemanticColor.stateGreenTertiary,
            value = 4
        )

        EmotionType.CONTENT -> EmotionOption(
            imageResId = R.drawable.ic_circle_content,
            label = "행복하다",
            boxColor = SemanticColor.statePinkTertiary,
            textColor = SemanticColor.statePinkPrimary,
            value = 3
        )

        EmotionType.DEPRESSED -> EmotionOption(
            imageResId = R.drawable.ic_circle_depressed,
            label = "우울하다",
            boxColor = SemanticColor.stateBlueTertiary,
            textColor = SemanticColor.stateBluePrimary,
            value = 2
        )

        EmotionType.TIRED -> EmotionOption(
            imageResId = R.drawable.ic_circle_tired,
            label = "지친다",
            boxColor = SemanticColor.statePurpleTertiary,
            textColor = SemanticColor.statePurplePrimary,
            value = 1
        )

        EmotionType.IRRITATED -> EmotionOption(
            imageResId = R.drawable.ic_circle_anxious,
            label = "짜증난다",
            boxColor = SemanticColor.stateRedTertiary,
            textColor = SemanticColor.stateRedPrimary,
            value = 0
        )
    }
}

/**
 * 감정 타입 리스트를 EmotionOption 리스트로 변환
 *
 * @param emotionTypes 감정 타입 리스트
 * @return EmotionOption 리스트
 */
fun emotionTypesToOptions(emotionTypes: List<EmotionType>): List<EmotionOption> {
    return emotionTypes.map { emotionTypeToOption(it) }
}

/**
 * 기본 감정 옵션 리스트 생성 (EMOTION_TYPE_ORDER 사용)
 *
 * @return EmotionOption 리스트
 */
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
    return when (emotionType) {
        EmotionType.HAPPY -> 5
        EmotionType.JOYFUL -> 4
        EmotionType.CONTENT -> 3
        EmotionType.DEPRESSED -> 2
        EmotionType.TIRED -> 1
        EmotionType.IRRITATED -> 0
    }
}

/**
 * value를 EmotionType으로 변환
 *
 * @param value 감정 값 (0-5)
 * @return EmotionType
 */
fun valueToEmotionType(value: Int): EmotionType {
    return when (value) {
        5 -> EmotionType.HAPPY
        4 -> EmotionType.JOYFUL
        3 -> EmotionType.CONTENT
        2 -> EmotionType.DEPRESSED
        1 -> EmotionType.TIRED
        0 -> EmotionType.IRRITATED
        else -> EmotionType.CONTENT // 기본값
    }
}

/**
 * 선택된 감정의 인덱스 찾기
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




