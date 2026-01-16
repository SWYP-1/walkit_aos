package swyp.team.walkit.utils

import org.junit.Assert
import org.junit.Assert.assertFalse

import org.junit.Test

/**
 * NicknameUtils 단위 테스트
 *
 * 테스트 대상:
 * - filterIncompleteKorean(): 한글 자음/모음 필터링
 * - filterInput(): 입력 중 필터링 (현재 미사용)
 * - isNicknameComplete(): 닉네임 완성 상태 확인
 */
class NicknameUtilsTest {

    // ===== filterIncompleteKorean() 테스트 =====

    @Test
    fun `filterIncompleteKorean - 한글 자음 제거`() {
        // Given: 한글 자음이 포함된 입력
        val input = "안ㄴ"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 자음이 제거되고 완성된 글자만 남음
        Assert.assertEquals("안", result)
    }

    @Test
    fun `filterIncompleteKorean - 한글 모음 제거`() {
        // Given: 한글 모음이 포함된 입력
        val input = "안ㅏ"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 모음이 제거되고 완성된 글자만 남음
        Assert.assertEquals("안", result)
    }

    @Test
    fun `filterIncompleteKorean - 여러 자음모음 제거`() {
        // Given: 여러 자음/모음이 포함된 입력
        val input = "안ㄴㅕㅇㅎ"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 모든 자음/모음이 제거되고 완성된 글자만 남음
        Assert.assertEquals("안녕", result)
    }

    @Test
    fun `filterIncompleteKorean - 영문 유지`() {
        // Given: 영문 입력
        val input = "Hello"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 영문은 그대로 유지
        Assert.assertEquals("Hello", result)
    }

    @Test
    fun `filterIncompleteKorean - 한글과 영문 혼합`() {
        // Given: 한글과 영문이 혼합된 입력
        val input = "안녕Hi"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 완성된 글자만 유지
        Assert.assertEquals("안녕Hi", result)
    }

    @Test
    fun `filterIncompleteKorean - 숫자 유지`() {
        // Given: 숫자 입력
        val input = "123"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 숫자는 그대로 유지
        Assert.assertEquals("123", result)
    }

    @Test
    fun `filterIncompleteKorean - 특수문자 제거`() {
        // Given: 특수문자가 포함된 입력
        val input = "안@녕!"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 특수문자는 제거되고 완성된 글자만 남음
        Assert.assertEquals("안녕", result)
    }

    @Test
    fun `filterIncompleteKorean - 빈 문자열`() {
        // Given: 빈 문자열
        val input = ""

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 빈 문자열 그대로 반환
        Assert.assertEquals("", result)
    }

    @Test
    fun `filterIncompleteKorean - 완성된 한글만 있는 경우`() {
        // Given: 완성된 한글만 있는 입력
        val input = "안녕하세요"

        // When: 필터링 적용
        val result = NicknameUtils.filterIncompleteKorean(input)

        // Then: 그대로 유지
        Assert.assertEquals("안녕하세요", result)
    }

    // ===== filterInput() 테스트 =====

    @Test
    fun `filterInput - 입력 중 필터링하지 않음`() {
        // Given: 모든 종류의 입력
        val inputs = listOf(
            "안ㄴ",      // 자음 포함
            "안ㅏ",      // 모음 포함
            "Hello",     // 영문
            "안녕Hi",    // 혼합
            "123",       // 숫자
            "안@녕!",    // 특수문자
            ""           // 빈 문자열
        )

        // When & Then: 모든 입력이 그대로 반환됨
        inputs.forEach { input ->
            val result = NicknameUtils.filterInput(input)
            Assert.assertEquals("입력 중에는 필터링하지 않아야 함", input, result)
        }
    }

    // ===== isNicknameComplete() 테스트 =====

    @Test
    fun `isNicknameComplete - 빈 문자열은 완성됨`() {
        // Given: 빈 문자열
        val input = ""

        // When: 완성 상태 확인
        val result = NicknameUtils.isNicknameComplete(input)

        // Then: true 반환
        Assert.assertTrue("빈 문자열은 완성된 상태여야 함", result)
    }

    @Test
    fun `isNicknameComplete - 완성된 한글로 끝나는 경우`() {
        // Given: 완성된 글자로 끝나는 문자열들
        val inputs = listOf(
            "안",
            "안녕",
            "Hello",
            "안녕Hi",
            "123",
            "안녕하세요"
        )

        // When & Then: 모두 true 반환
        inputs.forEach { input ->
            val result = NicknameUtils.isNicknameComplete(input)
            Assert.assertTrue("완성된 글자로 끝나야 함: $input", result)
        }
    }

    @Test
    fun `isNicknameComplete - 자음으로 끝나는 경우`() {
        // Given: 자음으로 끝나는 문자열들
        val inputs = listOf(
            "안ㄱ",
            "안ㄴ",
            "안ㄷ",
            "Helloㄱ",
            "안녕ㅎ"
        )

        // When & Then: 모두 false 반환
        inputs.forEach { input ->
            val result = NicknameUtils.isNicknameComplete(input)
            assertFalse("자음으로 끝나면 미완성: $input", result)
        }
    }

    @Test
    fun `isNicknameComplete - 모음으로 끝나는 경우`() {
        // Given: 모음으로 끝나는 문자열들
        val inputs = listOf(
            "안ㅏ",
            "안ㅑ",
            "안ㅓ",
            "Helloㅏ",
            "안녕ㅗ"
        )

        // When & Then: 모두 false 반환
        inputs.forEach { input ->
            val result = NicknameUtils.isNicknameComplete(input)
            assertFalse("모음으로 끝나면 미완성: $input", result)
        }
    }

    @Test
    fun `isNicknameComplete - 실제 한글 자음모음 유니코드`() {
        // Given: 실제 한글 자음/모음 유니코드
        val consonantInputs = listOf(
            "안\u1100",  // ᄀ (U+1100)
            "안\u1101",  // ᄁ (U+1101)
            "안\u1112"   // ᄒ (U+1112)
        )

        val vowelInputs = listOf(
            "안\u1161",  // ᅡ (U+1161)
            "안\u1162",  // ᅢ (U+1162)
            "안\u1175"   // ᅵ (U+1175)
        )

        // When & Then: 자음으로 끝나면 false
        consonantInputs.forEach { input ->
            val result = NicknameUtils.isNicknameComplete(input)
            assertFalse("한글 자음으로 끝나면 미완성: $input", result)
        }

        // When & Then: 모음으로 끝나면 false
        vowelInputs.forEach { input ->
            val result = NicknameUtils.isNicknameComplete(input)
            assertFalse("한글 모음으로 끝나면 미완성: $input", result)
        }
    }
}