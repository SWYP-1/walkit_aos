package swyp.team.walkit.ui.onboarding

import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * OnboardingViewModel 단위 테스트
 *
 * 테스트 대상:
 * - NicknameState.validateNickname(): 닉네임 유효성 검증
 * - updateNicknameRaw(): 입력 중 필터링 로직
 * - validateNicknameOnComplete(): 완료 시 검증 로직
 */
class OnboardingViewModelTest {

    // ===== NicknameState.validateNickname() 테스트 =====

    @Test
    fun `validateNickname - 빈 문자열은 유효함`() {
        // Given: 빈 문자열
        val nickname = ""

        // When: 유효성 검증
        val result = NicknameState.validateNickname(nickname)

        // Then: null 반환 (에러 없음)
        assertNull("빈 문자열은 유효해야 함", result)
    }

    @Test
    fun `validateNickname - 길이 초과 시 에러`() {
        // Given: 21자 닉네임 (최대 20자)
        val nickname = "가나다라마바사아자차카타파하이" // 21자

        // When: 유효성 검증
        val result = NicknameState.validateNickname(nickname)

        // Then: 길이 초과 에러
        Assert.assertEquals("닉네임은 최대 20자까지 입력 가능합니다", result)
    }

    @Test
    fun `validateNickname - 최대 길이는 유효함`() {
        // Given: 정확히 20자 닉네임
        val nickname = "가나다라마바사아자차카타파하" // 20자

        // When: 유효성 검증
        val result = NicknameState.validateNickname(nickname)

        // Then: null 반환 (에러 없음)
        assertNull("20자 닉네임은 유효해야 함", result)
    }

    @Test
    fun `validateNickname - 띄어쓰기 포함 시 에러`() {
        // Given: 띄어쓰기가 포함된 닉네임
        val nicknames = listOf(
            "안 녕",
            "Hello World",
            "안녕 ",
            " Hello"
        )

        // When & Then: 모두 띄어쓰기 에러
        nicknames.forEach { nickname ->
            val result = NicknameState.validateNickname(nickname)
            Assert.assertEquals("띄어쓰기가 포함된 닉네임은 에러여야 함: $nickname",
                        "닉네임에 띄어쓰기를 사용할 수 없습니다", result)
        }
    }

    @Test
    fun `validateNickname - 특수문자 포함 시 에러`() {
        // Given: 특수문자가 포함된 닉네임
        val nicknames = listOf(
            "안녕!",
            "Hello@",
            "안녕#",
            "test\$",
            "가나다123@"
        )

        // When & Then: 모두 특수문자 에러
        nicknames.forEach { nickname ->
            val result = NicknameState.validateNickname(nickname)
            Assert.assertEquals("특수문자가 포함된 닉네임은 에러여야 함: $nickname",
                        "닉네임은 한글과 영문만 사용할 수 있습니다", result)
        }
    }

    @Test
    fun `validateNickname - 숫자 포함 시 에러`() {
        // Given: 숫자가 포함된 닉네임
        val nicknames = listOf(
            "안녕123",
            "Hello1",
            "가나다2",
            "test123"
        )

        // When & Then: 모두 특수문자 에러 (숫자는 허용되지 않음)
        nicknames.forEach { nickname ->
            val result = NicknameState.validateNickname(nickname)
            Assert.assertEquals("숫자가 포함된 닉네임은 에러여야 함: $nickname",
                        "닉네임은 한글과 영문만 사용할 수 있습니다", result)
        }
    }

    @Test
    fun `validateNickname - 유효한 한글 닉네임`() {
        // Given: 유효한 한글 닉네임들
        val nicknames = listOf(
            "안녕",
            "가나다",
            "홍길동",
            "김철수",
            "이영희",
            "박민수",
            "최수진",
            "정다혜"
        )

        // When & Then: 모두 유효함
        nicknames.forEach { nickname ->
            val result = NicknameState.validateNickname(nickname)
            assertNull("유효한 한글 닉네임은 에러가 없어야 함: $nickname", result)
        }
    }

    @Test
    fun `validateNickname - 유효한 영문 닉네임`() {
        // Given: 유효한 영문 닉네임들
        val nicknames = listOf(
            "Hello",
            "John",
            "Alice",
            "Bob",
            "Charlie",
            "David",
            "Emma",
            "Frank"
        )

        // When & Then: 모두 유효함
        nicknames.forEach { nickname ->
            val result = NicknameState.validateNickname(nickname)
            assertNull("유효한 영문 닉네임은 에러가 없어야 함: $nickname", result)
        }
    }

    @Test
    fun `validateNickname - 대소문자 구분`() {
        // Given: 대소문자가 혼합된 닉네임들
        val nicknames = listOf(
            "Hello",
            "HELLO",
            "HelloWorld",
            "helloWorld",
            "Hello123"  // 숫자 포함으로 에러
        )

        // When & Then: 대소문자 모두 유효함
        val validNicknames = nicknames.filter { !it.contains("123") }
        validNicknames.forEach { nickname ->
            val result = NicknameState.validateNickname(nickname)
            assertNull("대소문자 닉네임은 유효해야 함: $nickname", result)
        }

        // 숫자 포함은 에러
        val invalidNickname = nicknames.find { it.contains("123") }
        invalidNickname?.let { nickname ->
            val result = NicknameState.validateNickname(nickname)
            Assert.assertEquals("숫자가 포함된 닉네임은 에러여야 함: $nickname",
                        "닉네임은 한글과 영문만 사용할 수 있습니다", result)
        }
    }

    @Test
    fun `validateNickname - 경계값 테스트`() {
        // Given: 경계값 테스트 케이스들
        val testCases = mapOf(
            "가" to null,                          // 1자 한글
            "a" to null,                           // 1자 영문
            "가나다라마바사아자차카타파하" to null,    // 정확히 20자
            "가나다라마바사아자차카타파하이" to "닉네임은 최대 20자까지 입력 가능합니다",  // 21자
            "안 녕" to "닉네임에 띄어쓰기를 사용할 수 없습니다",     // 띄어쓰기
            "안녕!" to "닉네임은 한글과 영문만 사용할 수 있습니다",   // 특수문자
            "Hello123" to "닉네임은 한글과 영문만 사용할 수 있습니다"  // 숫자
        )

        // When & Then: 각 케이스 검증
        testCases.forEach { (nickname, expectedError) ->
            val result = NicknameState.validateNickname(nickname)
            Assert.assertEquals("닉네임 검증 실패: $nickname", expectedError, result)
        }
    }

    // ===== NicknameState 프로퍼티 테스트 =====

    @Test
    fun `NicknameState - value 프로퍼티는 filteredValue를 반환함`() {
        // Given: filteredValue가 설정된 NicknameState
        val state = NicknameState(
            rawValue = "안ㄴ",
            filteredValue = "안"
        )

        // When: value 프로퍼티 접근
        val value = state.value

        // Then: filteredValue와 동일함
        Assert.assertEquals("안", value)
        Assert.assertEquals(state.filteredValue, value)
    }

    @Test
    fun `NicknameState - isValid 프로퍼티는 validationError가 null일 때 true`() {
        // Given: validationError가 null인 상태
        val validState = NicknameState(
            filteredValue = "안녕",
            validationError = null
        )

        // Given: validationError가 있는 상태
        val invalidState = NicknameState(
            filteredValue = "안녕!",
            validationError = "특수문자 에러"
        )

        // When & Then
        Assert.assertEquals(true, validState.isValid)
        Assert.assertEquals(false, invalidState.isValid)
    }
}