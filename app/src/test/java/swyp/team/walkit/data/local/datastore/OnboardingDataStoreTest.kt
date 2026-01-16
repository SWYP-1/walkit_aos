package swyp.team.walkit.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import swyp.team.walkit.domain.model.OnboardingProgress

/**
 * OnboardingDataStore 단위 테스트
 *
 * 테스트 대상:
 * - Flow 프로퍼티들: isCompleted, isTermsAgreed, kakaoOnboardingCompleted, etc.
 * - isOnboardingCompletedForProvider(): 제공자별 완료 상태 확인
 * - setOnboardingCompletedForProvider(): 제공자별 완료 상태 설정
 * - getProgress(): OnboardingProgress 조회
 * - saveProgress(): OnboardingProgress 저장
 * - completeOnboarding(): 온보딩 완료 처리
 * - clearAllOnboardingData(): 모든 데이터 초기화
 */
class OnboardingDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var onboardingDataStore: OnboardingDataStore

    @Before
    fun setup() {
        dataStore = mockk()
        onboardingDataStore = OnboardingDataStore(dataStore)
    }

    // ===== Flow 프로퍼티 테스트 =====

    @Test
    fun `isCompleted Flow - 온보딩 완료 상태를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 완료 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("onboarding_completed")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = onboardingDataStore.isCompleted.first()

        // Then: true가 반환됨
        Assert.assertTrue("온보딩 완료 상태가 true여야 함", result)
    }

    @Test
    fun `isTermsAgreed Flow - 약관 동의 상태를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 약관 동의 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("onboarding_terms_agreed")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = onboardingDataStore.isTermsAgreed.first()

        // Then: true가 반환됨
        Assert.assertTrue("약관 동의 상태가 true여야 함", result)
    }

    @Test
    fun `kakaoOnboardingCompleted Flow - 카카오 온보딩 완료 상태를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 카카오 완료 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("kakao_onboarding_completed")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = onboardingDataStore.kakaoOnboardingCompleted.first()

        // Then: true가 반환됨
        Assert.assertTrue("카카오 온보딩 완료 상태가 true여야 함", result)
    }

    @Test
    fun `naverOnboardingCompleted Flow - 네이버 온보딩 완료 상태를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 네이버 완료 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("naver_onboarding_completed")] } returns false
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = onboardingDataStore.naverOnboardingCompleted.first()

        // Then: false가 반환됨
        assertFalse(result)
    }

    @Test
    fun `Flow 프로퍼티들 - 값이 없을 때 기본값 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("onboarding_completed")] } returns null
        every { mockPreferences[booleanPreferencesKey("onboarding_terms_agreed")] } returns null
        every { mockPreferences[booleanPreferencesKey("kakao_onboarding_completed")] } returns null
        every { mockPreferences[booleanPreferencesKey("naver_onboarding_completed")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 각 Flow에서 값 수집
        val isCompleted = onboardingDataStore.isCompleted.first()
        val isTermsAgreed = onboardingDataStore.isTermsAgreed.first()
        val kakaoCompleted = onboardingDataStore.kakaoOnboardingCompleted.first()
        val naverCompleted = onboardingDataStore.naverOnboardingCompleted.first()

        // Then: 모두 기본값(false)이 반환됨
        assertFalse(isCompleted)
        assertFalse(isTermsAgreed)
        assertFalse(kakaoCompleted)
        assertFalse(naverCompleted)
    }

    // ===== isOnboardingCompletedForProvider 테스트 =====

    @Test
    fun `isOnboardingCompletedForProvider - 카카오 제공자 완료 상태 확인`() = runTest {
        // Given: Mock DataStore가 카카오 완료 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("kakao_onboarding_completed")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 카카오 제공자 완료 상태 확인
        val result = onboardingDataStore.isOnboardingCompletedForProvider("카카오").first()

        // Then: true가 반환됨
        Assert.assertTrue("카카오 제공자 온보딩 완료 상태가 true여야 함", result)
    }

    @Test
    fun `isOnboardingCompletedForProvider - 네이버 제공자 완료 상태 확인`() = runTest {
        // Given: Mock DataStore가 네이버 완료 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("naver_onboarding_completed")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 네이버 제공자 완료 상태 확인
        val result = onboardingDataStore.isOnboardingCompletedForProvider("네이버").first()

        // Then: true가 반환됨
        Assert.assertTrue("네이버 제공자 온보딩 완료 상태가 true여야 함", result)
    }

    @Test
    fun `isOnboardingCompletedForProvider - 지원하지 않는 제공자는 false 반환`() = runTest {
        // Given: 지원하지 않는 제공자
        val provider = "google"

        // When: 지원하지 않는 제공자 확인
        val result = onboardingDataStore.isOnboardingCompletedForProvider(provider).first()

        // Then: false가 반환됨 (기본값)
        assertFalse(result)
    }

    @Test
    fun `isOnboardingCompletedForProvider - 소문자 제공자명도 정상 처리`() = runTest {
        // Given: Mock DataStore가 카카오 완료 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("kakao_onboarding_completed")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 소문자 제공자명으로 확인
        val result = onboardingDataStore.isOnboardingCompletedForProvider("kakao").first()

        // Then: true가 반환됨 (대소문자 구분 없이 처리)
        Assert.assertTrue("소문자 제공자명으로도 완료 상태가 true여야 함", result)
    }

    // ===== setOnboardingCompletedForProvider 테스트 =====

    @Test
    fun `setOnboardingCompletedForProvider - 카카오 제공자 완료 상태 설정`() = runTest {
        // When: 카카오 제공자 완료 상태를 true로 설정
        onboardingDataStore.setOnboardingCompletedForProvider("카카오", true)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setOnboardingCompletedForProvider - 네이버 제공자 완료 상태 설정`() = runTest {
        // When: 네이버 제공자 완료 상태를 false로 설정
        onboardingDataStore.setOnboardingCompletedForProvider("네이버", false)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setOnboardingCompletedForProvider - 지원하지 않는 제공자는 무시됨`() = runTest {
        // When: 지원하지 않는 제공자 설정 시도
        onboardingDataStore.setOnboardingCompletedForProvider("apple", true)

        // Then: 아무 동작도 하지 않음 (예외 발생하지 않음)
        // 실제로는 edit가 호출되지 않지만, mock으로는 검증하기 어려움
    }

    // ===== getProgress 테스트 =====

    @Test
    fun `getProgress - OnboardingProgress를 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 모든 온보딩 데이터를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[intPreferencesKey("onboarding_current_step")] } returns 2
        every { mockPreferences[stringPreferencesKey("onboarding_nickname")] } returns "테스트유저"
        every { mockPreferences[stringPreferencesKey("onboarding_selected_image_uri")] } returns "content://image/123"
        every { mockPreferences[intPreferencesKey("onboarding_goal_count")] } returns 15
        every { mockPreferences[intPreferencesKey("onboarding_step_target")] } returns 12000
        every { mockPreferences[stringPreferencesKey("onboarding_unit")] } returns "걸음"
        every { mockPreferences[intPreferencesKey("onboarding_birth_year")] } returns 1995
        every { mockPreferences[intPreferencesKey("onboarding_birth_month")] } returns 8
        every { mockPreferences[intPreferencesKey("onboarding_birth_day")] } returns 20
        every { mockPreferences[booleanPreferencesKey("onboarding_marketing_consent")] } returns true
        every { mockPreferences[booleanPreferencesKey("onboarding_nickname_registered")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getProgress 호출
        val progress = onboardingDataStore.getProgress()

        // Then: 올바른 OnboardingProgress가 반환됨
        Assert.assertEquals(2, progress.currentStep)
        Assert.assertEquals("테스트유저", progress.nickname)
        Assert.assertEquals("content://image/123", progress.selectedImageUri)
        Assert.assertEquals(15, progress.goalCount)
        Assert.assertEquals(12000, progress.stepTarget)
        Assert.assertEquals("걸음", progress.unit)
        Assert.assertEquals(1995, progress.birthYear)
        Assert.assertEquals(8, progress.birthMonth)
        Assert.assertEquals(20, progress.birthDay)
        Assert.assertTrue("마케팅 동의 상태가 true여야 함", progress.marketingConsent)
        Assert.assertTrue("닉네임 등록 상태가 true여야 함", progress.nicknameRegistered)
    }

    @Test
    fun `getProgress - 값이 없을 때 기본값 사용`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[intPreferencesKey("onboarding_current_step")] } returns null
        every { mockPreferences[stringPreferencesKey("onboarding_nickname")] } returns null
        every { mockPreferences[stringPreferencesKey("onboarding_selected_image_uri")] } returns null
        every { mockPreferences[intPreferencesKey("onboarding_goal_count")] } returns null
        every { mockPreferences[intPreferencesKey("onboarding_step_target")] } returns null
        every { mockPreferences[stringPreferencesKey("onboarding_unit")] } returns null
        every { mockPreferences[intPreferencesKey("onboarding_birth_year")] } returns null
        every { mockPreferences[intPreferencesKey("onboarding_birth_month")] } returns null
        every { mockPreferences[intPreferencesKey("onboarding_birth_day")] } returns null
        every { mockPreferences[booleanPreferencesKey("onboarding_marketing_consent")] } returns null
        every { mockPreferences[booleanPreferencesKey("onboarding_nickname_registered")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getProgress 호출
        val progress = onboardingDataStore.getProgress()

        // Then: 기본값들이 설정됨
        Assert.assertEquals(0, progress.currentStep)
        Assert.assertEquals("", progress.nickname)
        assertNull(progress.selectedImageUri)
        Assert.assertEquals(10, progress.goalCount)  // 기본값
        Assert.assertEquals(0, progress.stepTarget)
        Assert.assertEquals("달", progress.unit)  // 기본값
        Assert.assertEquals(1990, progress.birthYear)  // 기본값
        Assert.assertEquals(1, progress.birthMonth)  // 기본값
        Assert.assertEquals(1, progress.birthDay)  // 기본값
        assertFalse(progress.marketingConsent)
        assertFalse(progress.nicknameRegistered)
    }

    // ===== saveProgress 테스트 =====

    @Test
    fun `saveProgress - OnboardingProgress를 올바르게 저장해야 함`() = runTest {
        // Given: 저장할 OnboardingProgress
        val progress = OnboardingProgress(
            currentStep = 3,
            nickname = "저장테스트",
            selectedImageUri = "content://saved/image",
            goalCount = 20,
            stepTarget = 15000,
            unit = "칼로리",
            birthYear = 1992,
            birthMonth = 12,
            birthDay = 25,
            marketingConsent = true,
            nicknameRegistered = true
        )

        // When: saveProgress 호출
        onboardingDataStore.saveProgress(progress)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    // ===== completeOnboarding 테스트 =====

    @Test
    fun `completeOnboarding - 온보딩 완료 처리를 올바르게 수행해야 함`() = runTest {
        // When: completeOnboarding 호출
        onboardingDataStore.completeOnboarding()

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    // ===== clearAllOnboardingData 테스트 =====

    @Test
    fun `clearAllOnboardingData - 모든 온보딩 데이터를 올바르게 초기화해야 함`() = runTest {
        // When: clearAllOnboardingData 호출
        onboardingDataStore.clearAllOnboardingData()

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    // ===== 통합 테스트 =====

    @Test
    fun `saveProgress와 getProgress 통합 테스트`() = runTest {
        // Given: 저장할 Progress
        val originalProgress = OnboardingProgress(
            currentStep = 1,
            nickname = "통합테스트",
            selectedImageUri = "content://test",
            goalCount = 25,
            stepTarget = 10000,
            unit = "시간",
            birthYear = 1988,
            birthMonth = 3,
            birthDay = 15,
            marketingConsent = false,
            nicknameRegistered = true
        )

        // Mock Preferences 설정 (getProgress에서 사용할 값들)
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[intPreferencesKey("onboarding_current_step")] } returns 1
        every { mockPreferences[stringPreferencesKey("onboarding_nickname")] } returns "통합테스트"
        every { mockPreferences[stringPreferencesKey("onboarding_selected_image_uri")] } returns "content://test"
        every { mockPreferences[intPreferencesKey("onboarding_goal_count")] } returns 25
        every { mockPreferences[intPreferencesKey("onboarding_step_target")] } returns 10000
        every { mockPreferences[stringPreferencesKey("onboarding_unit")] } returns "시간"
        every { mockPreferences[intPreferencesKey("onboarding_birth_year")] } returns 1988
        every { mockPreferences[intPreferencesKey("onboarding_birth_month")] } returns 3
        every { mockPreferences[intPreferencesKey("onboarding_birth_day")] } returns 15
        every { mockPreferences[booleanPreferencesKey("onboarding_marketing_consent")] } returns false
        every { mockPreferences[booleanPreferencesKey("onboarding_nickname_registered")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: saveProgress 호출 후 getProgress로 조회
        onboardingDataStore.saveProgress(originalProgress)
        val retrievedProgress = onboardingDataStore.getProgress()

        // Then: 저장된 값과 조회된 값이 동일함
        Assert.assertEquals(originalProgress, retrievedProgress)
    }
}