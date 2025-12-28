package team.swyp.sdu.ui.home.utils

import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.R

/**
 * 감정 타입에 따른 이미지 리소스 매핑
 */
fun getEmotionDrawableRes(emotionType: EmotionType): Int {
    return when (emotionType) {
        EmotionType.HAPPY -> R.drawable.ic_rec_happy
        EmotionType.JOYFUL -> R.drawable.ic_rec_joyful
        EmotionType.CONTENT -> R.drawable.ic_rec_content
        EmotionType.DEPRESSED -> R.drawable.ic_rec_depressed
        EmotionType.TIRED -> R.drawable.ic_rec_tired
        EmotionType.ANXIOUS -> R.drawable.ic_rec_anxious
    }
}

fun getEmotionFaceDrawableRes(emotionType: EmotionType): Int {
    return when (emotionType) {
        EmotionType.HAPPY -> R.drawable.ic_face_happy
        EmotionType.JOYFUL -> R.drawable.ic_face_joyfull
        EmotionType.CONTENT -> R.drawable.ic_face_content
        EmotionType.DEPRESSED -> R.drawable.ic_face_depressed
        EmotionType.TIRED -> R.drawable.ic_face_tired
        EmotionType.ANXIOUS -> R.drawable.ic_face_anxious
    }
}

/**
 * 감정 타입을 한글 이름으로 변환
 */
fun getEmotionName(emotionType: EmotionType): String {
    return when (emotionType) {
        EmotionType.HAPPY -> "기쁨"
        EmotionType.JOYFUL -> "즐거움"
        EmotionType.CONTENT -> "행복함"
        EmotionType.DEPRESSED -> "우울함"
        EmotionType.TIRED -> "지침"
        EmotionType.ANXIOUS -> "짜증남"
    }
}








