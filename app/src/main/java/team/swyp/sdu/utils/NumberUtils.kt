package team.swyp.sdu.utils

/**
 * 숫자 유틸리티 함수
 */
object NumberUtils {
    /**
     * 숫자를 3자리마다 쉼표로 구분하여 포맷팅
     *
     * @param number 포맷팅할 숫자
     * @return 3자리마다 쉼표가 들어간 문자열 (예: 1,234,567)
     */
    fun formatNumber(number: Int): String =
        number.toString().reversed().chunked(3).joinToString(",").reversed()

    /**
     * Long 타입 숫자를 3자리마다 쉼표로 구분하여 포맷팅
     *
     * @param number 포맷팅할 숫자
     * @return 3자리마다 쉼표가 들어간 문자열 (예: 1,234,567)
     */
    fun formatNumber(number: Long): String =
        number.toString().reversed().chunked(3).joinToString(",").reversed()

    /**
     * Double 타입 숫자를 3자리마다 쉼표로 구분하여 포맷팅 (소수점 이하 포함)
     *
     * @param number 포맷팅할 숫자
     * @param decimalPlaces 소수점 이하 자릿수 (기본값: 2)
     * @return 3자리마다 쉼표가 들어간 문자열 (예: 1,234,567.89)
     */
    fun formatNumber(number: Double, decimalPlaces: Int = 2): String {
        val formatted = String.format("%.${decimalPlaces}f", number)
        val parts = formatted.split(".")
        val integerPart = formatNumber(parts[0].toLong())
        return if (parts.size > 1) "$integerPart.${parts[1]}" else integerPart
    }
}
