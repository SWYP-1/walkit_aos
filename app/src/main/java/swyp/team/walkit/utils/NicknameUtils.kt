package swyp.team.walkit.utils

/**
 * 닉네임 입력 관련 유틸리티 함수들
 */
object NicknameUtils {

    /**
     * 입력 완료 시점에 적용할 필터링 (포커스 아웃 시 사용)
     * 한글 자음/모음만 있는 입력을 필터링하여 완성된 글자만 허용
     *
     * 예시:
     * "안ㄴ" → "안" (ㄴ 제거)
     * "안녕ㅎ" → "안녕" (ㅎ 제거)
     * "Hello" → "Hello" (영문 유지)
     * "안녕Hi" → "안녕Hi" (완성된 글자 유지)
     */
    fun filterIncompleteKorean(text: String): String {
        return text.filter { char ->
            // 완성된 한글(가-힣) 또는 영문, 숫자만 허용
            // 한글 자음(ㄱ,ㄷ,ㅂ 등)과 모음(ㅏ,ㅑ,ㅓ 등)은 제거
            char in '가'..'힣' || char in 'a'..'z' || char in 'A'..'Z' || char.isDigit()
        }
    }

    /**
     * 입력 중 필터링 (현재는 사용하지 않음 - 자연스러운 입력을 위해)
     * 입력 중에는 필터링하지 않고, 포커스 아웃 시점에만 필터링 적용
     */
    fun filterInput(text: String): String {
        // 입력 중에는 그대로 반환 (자연스러운 입력 경험을 위해)
        return text
    }

    /**
     * 닉네임 입력값이 완성된 상태인지 확인
     * (자음이나 모음으로 끝나지 않는지)
     */
    fun isNicknameComplete(text: String): Boolean {
        if (text.isEmpty()) return true

        val lastChar = text.last()
        // 한글 자음(0x1100-0x1112)이나 모음(0x1161-0x1175)으로 끝나면 미완성
        return !(lastChar in '\u1100'..'\u1112' || lastChar in '\u1161'..'\u1175')
    }
}