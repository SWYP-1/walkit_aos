package swyp.team.walkit.ui.home.utils

import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.R

/**
 * 감정 타입에 따른 이미지 리소스 매핑
 */
fun getEmotionDrawableRes(emotionType: EmotionType): Int {
    return when (emotionType) {
        EmotionType.JOYFUL -> R.drawable.ic_rec_happy
        EmotionType.DELIGHTED -> R.drawable.ic_rec_joyful
        EmotionType.HAPPY -> R.drawable.ic_rec_content
        EmotionType.DEPRESSED -> R.drawable.ic_rec_depressed
        EmotionType.TIRED -> R.drawable.ic_rec_tired
        EmotionType.IRRITATED -> R.drawable.ic_rec_anxious
    }
}

fun getEmotionFaceDrawableRes(emotionType: EmotionType): Int {
    return when (emotionType) {
        EmotionType.JOYFUL -> R.drawable.ic_face_happy
        EmotionType.DELIGHTED -> R.drawable.ic_face_joyfull
        EmotionType.HAPPY -> R.drawable.ic_face_content
        EmotionType.DEPRESSED -> R.drawable.ic_face_depressed
        EmotionType.TIRED -> R.drawable.ic_face_tired
        EmotionType.IRRITATED -> R.drawable.ic_face_anxious
    }
}

/**
 * 감정 타입을 한글 이름으로 변환
 */
fun getEmotionName(emotionType: EmotionType): String {
    return when (emotionType) {
        EmotionType.JOYFUL -> "기쁨"
        EmotionType.DELIGHTED -> "즐거움"
        EmotionType.HAPPY -> "행복함"
        EmotionType.DEPRESSED -> "우울함"
        EmotionType.TIRED -> "지침"
        EmotionType.IRRITATED -> "짜증남"
    }
}









