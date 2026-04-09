package swyp.team.walkit.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import swyp.team.walkit.domain.model.OnboardingProgress
import timber.log.Timber

@Singleton
class OnboardingDataStore @Inject constructor(
    @Named("onboarding") private val dataStore: DataStore<Preferences>,
) {
    // 기존 키들 (하위 호환성 유지)
    private val completedKey = booleanPreferencesKey("onboarding_completed")

    // 약관 동의 완료 상태
    private val termsAgreedKey = booleanPreferencesKey("onboarding_terms_agreed")

    // 소셜 구별자별 온보딩 완료 상태
    private val kakaoOnboardingCompletedKey = booleanPreferencesKey("kakao_onboarding_completed")
    private val naverOnboardingCompletedKey = booleanPreferencesKey("naver_onboarding_completed")

    // 진행 상태를 위한 개별 키들
    private val currentStepKey = intPreferencesKey("onboarding_current_step")
    private val nicknameKey = stringPreferencesKey("onboarding_nickname")
    private val selectedImageUriKey = stringPreferencesKey("onboarding_selected_image_uri")
    private val sexKey = stringPreferencesKey("onboarding_sex")
    private val goalCountKey = intPreferencesKey("onboarding_goal_count")
    private val stepTargetKey = intPreferencesKey("onboarding_step_target")
    private val unitKey = stringPreferencesKey("onboarding_unit")
    private val birthYearKey = intPreferencesKey("onboarding_birth_year")
    private val birthMonthKey = intPreferencesKey("onboarding_birth_month")
    private val birthDayKey = intPreferencesKey("onboarding_birth_day")
    private val marketingConsentKey = booleanPreferencesKey("onboarding_marketing_consent")
    private val nicknameRegisteredKey = booleanPreferencesKey("onboarding_nickname_registered")

    // '어떻게 사용하나요' 온보딩 완료 여부
    private val howToUseCompletedKey = booleanPreferencesKey("how_to_use_completed")

    val isCompleted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[completedKey] ?: false }

    val isTermsAgreed: Flow<Boolean> = dataStore.data.map { prefs -> prefs[termsAgreedKey] ?: false }

    // '어떻게 사용하나요' 온보딩 완료 상태 Flow
    val isHowToUseCompleted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[howToUseCompletedKey] ?: false }

    // 소셜별 온보딩 완료 상태 Flow
    val kakaoOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[kakaoOnboardingCompletedKey] ?: false }
    val naverOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs -> prefs[naverOnboardingCompletedKey] ?: false }

    /**
     * 소셜 구별자별 온보딩 완료 상태 확인
     */
    fun isOnboardingCompletedForProvider(provider: String): Flow<Boolean> = dataStore.data.map { prefs ->
        when (provider.lowercase()) {
            "카카오" -> prefs[kakaoOnboardingCompletedKey] ?: false
            "네이버" -> prefs[naverOnboardingCompletedKey] ?: false
            else -> false
        }
    }

    /**
     * 소셜 구별자별 온보딩 완료 상태 설정
     */
    suspend fun setOnboardingCompletedForProvider(provider: String, completed: Boolean) {
        val key = when (provider.lowercase()) {
            "카카오" -> kakaoOnboardingCompletedKey
            "네이버" -> naverOnboardingCompletedKey
            else -> return // 지원하지 않는 제공자
        }
        dataStore.edit { prefs -> prefs[key] = completed }
    }

    suspend fun getProgress(): OnboardingProgress {
        return dataStore.data.first().let { prefs ->
            val progress = OnboardingProgress(
                currentStep = prefs[currentStepKey] ?: 0,
                nickname = prefs[nicknameKey] ?: "",
                selectedImageUri = prefs[selectedImageUriKey],
                goalCount = prefs[goalCountKey] ?: 10,
                stepTarget = prefs[stepTargetKey] ?: 0,
                unit = prefs[unitKey] ?: "달",
                birthYear = prefs[birthYearKey] ?: 1990,
                birthMonth = prefs[birthMonthKey] ?: 1,
                birthDay = prefs[birthDayKey] ?: 1,
                marketingConsent = prefs[marketingConsentKey] ?: false,
                nicknameRegistered = prefs[nicknameRegisteredKey] ?: false
            )
            Timber.d("OnboardingDataStore.getProgress() - currentStep: ${progress.currentStep}, nickname: ${progress.nickname}, goalCount: ${progress.goalCount}")
            progress
        }
    }

    suspend fun setCompleted(completed: Boolean) {
        dataStore.edit { prefs -> prefs[completedKey] = completed }
    }

    suspend fun setHowToUseCompleted(completed: Boolean) {
        dataStore.edit { prefs -> prefs[howToUseCompletedKey] = completed }
    }

    suspend fun setTermsAgreed(agreed: Boolean) {
        dataStore.edit { prefs -> prefs[termsAgreedKey] = agreed }
    }

    suspend fun saveProgress(progress: OnboardingProgress) {
        dataStore.edit { prefs ->
            prefs[currentStepKey] = progress.currentStep
            prefs[nicknameKey] = progress.nickname
            progress.selectedImageUri?.let { prefs[selectedImageUriKey] = it }
            prefs[goalCountKey] = progress.goalCount
            prefs[stepTargetKey] = progress.stepTarget
            prefs[unitKey] = progress.unit
            prefs[birthYearKey] = progress.birthYear
            prefs[birthMonthKey] = progress.birthMonth
            prefs[birthDayKey] = progress.birthDay
            prefs[marketingConsentKey] = progress.marketingConsent
            prefs[nicknameRegisteredKey] = progress.nicknameRegistered
        }
        Timber.d("OnboardingDataStore.saveProgress() 완료 - currentStep: ${progress.currentStep}, nickname: ${progress.nickname}, goalCount: ${progress.goalCount}")
    }

    suspend fun clearProgress() {
        dataStore.edit { prefs ->
            prefs.remove(currentStepKey)
            prefs.remove(nicknameKey)
            prefs.remove(selectedImageUriKey)
            prefs.remove(sexKey)
            prefs.remove(goalCountKey)
            prefs.remove(stepTargetKey)
            prefs.remove(unitKey)
            prefs.remove(birthYearKey)
            prefs.remove(birthMonthKey)
            prefs.remove(birthDayKey)
            prefs.remove(marketingConsentKey)
            prefs.remove(nicknameRegisteredKey)
        }
    }

    /**
     * 온보딩 진행 데이터 초기화
     *
     * 로그인 전환 시 모든 온보딩 데이터를 초기화합니다.
     * 다른 소셜 계정으로 로그인할 때 재온보딩이 필요합니다.
     */
    suspend fun clearAllOnboardingData() {
        dataStore.edit { prefs ->
            // 약관 동의 상태 초기화
            prefs.remove(termsAgreedKey)
            // 진행 상태 데이터 모두 제거
            prefs.remove(currentStepKey)
            prefs.remove(nicknameKey)
            prefs.remove(selectedImageUriKey)
            prefs.remove(sexKey)
            prefs.remove(goalCountKey)
            prefs.remove(stepTargetKey)
            prefs.remove(unitKey)
            prefs.remove(birthYearKey)
            prefs.remove(birthMonthKey)
            prefs.remove(birthDayKey)
            prefs.remove(marketingConsentKey)
            prefs.remove(nicknameRegisteredKey)
            // 🔥 로그인 전환 시 온보딩 완료 상태도 초기화 (다른 계정으로 재온보딩 필요)
            prefs.remove(completedKey)
        }
    }

    /**
     * 온보딩 진행 상태만 초기화 (약관 동의 및 완료 상태는 유지)
     * 로그아웃 시 사용자가 완료한 온보딩 상태는 보존하되 진행 데이터는 초기화
     */
    suspend fun clearOnboardingProgressOnly() {
        dataStore.edit { prefs ->
            // ✅ 약관 동의 상태 및 완료 상태는 유지
            // 진행 상태 데이터만 제거
            prefs.remove(currentStepKey)
            prefs.remove(nicknameKey)
            prefs.remove(selectedImageUriKey)
            prefs.remove(sexKey)
            prefs.remove(goalCountKey)
            prefs.remove(stepTargetKey)
            prefs.remove(unitKey)
            prefs.remove(birthYearKey)
            prefs.remove(birthMonthKey)
            prefs.remove(birthDayKey)
            prefs.remove(marketingConsentKey)
            prefs.remove(nicknameRegisteredKey)
            // ✅ completedKey는 유지 (온보딩 완료 상태 보존)
        }
    }

    /**
     * 특정 소셜 제공자의 온보딩 데이터 초기화
     * 로그아웃 시 해당 제공자의 온보딩 상태만 초기화
     */
    suspend fun clearOnboardingDataForProvider(provider: String) {
        val key = when (provider.lowercase()) {
            "카카오" -> kakaoOnboardingCompletedKey
            "네이버" -> naverOnboardingCompletedKey
            else -> return // 지원하지 않는 제공자
        }

        dataStore.edit { prefs ->
            prefs.remove(key)
            Timber.d("$provider 온보딩 상태 초기화 완료")
        }
    }

    /**
     * 온보딩 완료 처리 (완료 상태 저장 + 모든 데이터 초기화)
     * 중첩된 updateData 호출을 방지하기 위해 하나의 edit 블록에서 처리
     */
    suspend fun completeOnboarding() {
        Timber.d("OnboardingDataStore.completeOnboarding() 시작")
        try {
            dataStore.edit { prefs ->
                // 완료 상태 저장
                prefs[completedKey] = true
                Timber.d("온보딩 완료 상태 저장: completedKey = true")
                // 약관 동의 상태 초기화
                prefs.remove(termsAgreedKey)
                Timber.d("약관 동의 상태 초기화: termsAgreedKey 삭제")
                // 진행 상태 데이터 모두 제거
                prefs.remove(currentStepKey)
                prefs.remove(nicknameKey)
                prefs.remove(selectedImageUriKey)
                prefs.remove(sexKey)
                prefs.remove(goalCountKey)
                prefs.remove(stepTargetKey)
                prefs.remove(unitKey)
                prefs.remove(birthYearKey)
                prefs.remove(birthMonthKey)
                prefs.remove(birthDayKey)
                prefs.remove(marketingConsentKey)
                prefs.remove(nicknameRegisteredKey)
                Timber.d("진행 상태 데이터 모두 제거 완료")
            }
            Timber.i("OnboardingDataStore.completeOnboarding() 성공")
        } catch (e: Exception) {
            Timber.e(e, "OnboardingDataStore.completeOnboarding() 실패")
            throw e
        }
    }
}


