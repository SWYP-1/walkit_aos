package swyp.team.walkit.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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

/**
 * WalkingDataStore 단위 테스트
 *
 * 테스트 대상:
 * - Flow 프로퍼티들: isWalkingActive, walkingStartTime, etc.
 * - Setter 메소드들: setWalkingActive, setWalkingStartTime, etc.
 * - Getter 메소드들: getWalkingStartTime, getWalkingStepCount, etc.
 * - clearWalkingData(): 모든 데이터 삭제
 */
class WalkingDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var walkingDataStore: WalkingDataStore

    @Before
    fun setup() {
        dataStore = mockk()
        walkingDataStore = WalkingDataStore(dataStore)
    }

    // ===== Flow 프로퍼티 테스트 =====

    @Test
    fun `isWalkingActive Flow - 산책 활성화 상태를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 true를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("is_walking_active")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.isWalkingActive.first()

        // Then: true가 반환됨
        Assert.assertEquals(true, result)
    }

    @Test
    fun `walkingStartTime Flow - 시작 시간을 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 시작 시간을 반환하도록 설정
        val startTime = 1640995200000L  // 2022-01-01 00:00:00 UTC
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[longPreferencesKey("walking_start_time")] } returns startTime
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.walkingStartTime.first()

        // Then: 올바른 시작 시간이 반환됨
        Assert.assertEquals(startTime, result)
    }

    @Test
    fun `walkingStepCount Flow - 걸음 수를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 걸음 수를 반환하도록 설정
        val stepCount = 1250
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[intPreferencesKey("walking_step_count")] } returns stepCount
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.walkingStepCount.first()

        // Then: 올바른 걸음 수가 반환됨
        Assert.assertEquals(stepCount, result)
    }

    @Test
    fun `walkingDuration Flow - 산책 시간을 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 산책 시간을 반환하도록 설정
        val duration = 1800000L  // 30분
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[longPreferencesKey("walking_duration")] } returns duration
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.walkingDuration.first()

        // Then: 올바른 산책 시간이 반환됨
        Assert.assertEquals(duration, result)
    }

    @Test
    fun `walkingIsPaused Flow - 일시정지 상태를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 일시정지 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("walking_is_paused")] } returns false
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.walkingIsPaused.first()

        // Then: 올바른 일시정지 상태가 반환됨
        Assert.assertEquals(false, result)
    }

    @Test
    fun `preWalkingEmotion Flow - 산책 전 감정을 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 산책 전 감정을 반환하도록 설정
        val emotion = "HAPPY"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("pre_walking_emotion")] } returns emotion
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.preWalkingEmotion.first()

        // Then: 올바른 감정이 반환됨
        Assert.assertEquals(emotion, result)
    }

    @Test
    fun `postWalkingEmotion Flow - 산책 후 감정을 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 산책 후 감정을 반환하도록 설정
        val emotion = "TIRED"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("post_walking_emotion")] } returns emotion
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: Flow에서 값 수집
        val result = walkingDataStore.postWalkingEmotion.first()

        // Then: 올바른 감정이 반환됨
        Assert.assertEquals(emotion, result)
    }

    @Test
    fun `Flow 프로퍼티들 - 값이 없을 때 null 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("is_walking_active")] } returns null
        every { mockPreferences[longPreferencesKey("walking_start_time")] } returns null
        every { mockPreferences[intPreferencesKey("walking_step_count")] } returns null
        every { mockPreferences[longPreferencesKey("walking_duration")] } returns null
        every { mockPreferences[booleanPreferencesKey("walking_is_paused")] } returns null
        every { mockPreferences[stringPreferencesKey("pre_walking_emotion")] } returns null
        every { mockPreferences[stringPreferencesKey("post_walking_emotion")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 각 Flow에서 값 수집
        val isActive = walkingDataStore.isWalkingActive.first()
        val startTime = walkingDataStore.walkingStartTime.first()
        val stepCount = walkingDataStore.walkingStepCount.first()
        val duration = walkingDataStore.walkingDuration.first()
        val isPaused = walkingDataStore.walkingIsPaused.first()
        val preEmotion = walkingDataStore.preWalkingEmotion.first()
        val postEmotion = walkingDataStore.postWalkingEmotion.first()

        // Then: 모두 null이 반환됨
        assertNull(isActive)
        assertNull(startTime)
        assertNull(stepCount)
        assertNull(duration)
        assertNull(isPaused)
        assertNull(preEmotion)
        assertNull(postEmotion)
    }

    // ===== Setter 메소드 테스트 =====

    @Test
    fun `setWalkingActive - 산책 활성화 상태를 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 값
        val active = true

        // When: setWalkingActive 호출
        walkingDataStore.setWalkingActive(active)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setWalkingStartTime - 시작 시간을 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 시작 시간
        val startTime = System.currentTimeMillis()

        // When: setWalkingStartTime 호출
        walkingDataStore.setWalkingStartTime(startTime)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setWalkingStepCount - 걸음 수를 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 걸음 수
        val stepCount = 2500

        // When: setWalkingStepCount 호출
        walkingDataStore.setWalkingStepCount(stepCount)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setWalkingDuration - 산책 시간을 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 산책 시간
        val duration = 3600000L  // 1시간

        // When: setWalkingDuration 호출
        walkingDataStore.setWalkingDuration(duration)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setWalkingPaused - 일시정지 상태를 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 일시정지 상태
        val paused = true

        // When: setWalkingPaused 호출
        walkingDataStore.setWalkingPaused(paused)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setPreWalkingEmotion - 산책 전 감정을 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 산책 전 감정
        val emotion = "EXCITED"

        // When: setPreWalkingEmotion 호출
        walkingDataStore.setPreWalkingEmotion(emotion)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `setPostWalkingEmotion - 산책 후 감정을 올바르게 저장해야 함`() = runTest {
        // Given: 설정할 산책 후 감정
        val emotion = "SATISFIED"

        // When: setPostWalkingEmotion 호출
        walkingDataStore.setPostWalkingEmotion(emotion)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    // ===== Getter 메소드 테스트 =====

    @Test
    fun `getWalkingStartTime - 시작 시간을 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 시작 시간을 반환하도록 설정
        val expectedTime = 1700000000000L
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[longPreferencesKey("walking_start_time")] } returns expectedTime
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getWalkingStartTime 호출
        val result = walkingDataStore.getWalkingStartTime()

        // Then: 올바른 시작 시간이 반환됨
        Assert.assertEquals(expectedTime, result)
    }

    @Test
    fun `getWalkingStepCount - 걸음 수를 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 걸음 수를 반환하도록 설정
        val expectedSteps = 1500
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[intPreferencesKey("walking_step_count")] } returns expectedSteps
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getWalkingStepCount 호출
        val result = walkingDataStore.getWalkingStepCount()

        // Then: 올바른 걸음 수가 반환됨
        Assert.assertEquals(expectedSteps, result)
    }

    @Test
    fun `getWalkingDuration - 산책 시간을 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 산책 시간을 반환하도록 설정
        val expectedDuration = 2700000L  // 45분
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[longPreferencesKey("walking_duration")] } returns expectedDuration
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getWalkingDuration 호출
        val result = walkingDataStore.getWalkingDuration()

        // Then: 올바른 산책 시간이 반환됨
        Assert.assertEquals(expectedDuration, result)
    }

    @Test
    fun `getWalkingIsPaused - 일시정지 상태를 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 일시정지 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("walking_is_paused")] } returns true
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getWalkingIsPaused 호출
        val result = walkingDataStore.getWalkingIsPaused()

        // Then: 올바른 일시정지 상태가 반환됨
        Assert.assertTrue("일시정지 상태가 true여야 함", result!!)
    }

    @Test
    fun `getIsWalkingActive - 산책 활성화 상태를 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 산책 활성화 상태를 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[booleanPreferencesKey("is_walking_active")] } returns false
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getIsWalkingActive 호출
        val result = walkingDataStore.getIsWalkingActive()

        // Then: 올바른 산책 활성화 상태가 반환됨
        assertFalse(result!!)
    }

    @Test
    fun `getPreWalkingEmotion - 산책 전 감정을 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 산책 전 감정을 반환하도록 설정
        val expectedEmotion = "CALM"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("pre_walking_emotion")] } returns expectedEmotion
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getPreWalkingEmotion 호출
        val result = walkingDataStore.getPreWalkingEmotion()

        // Then: 올바른 감정이 반환됨
        Assert.assertEquals(expectedEmotion, result)
    }

    @Test
    fun `getPostWalkingEmotion - 산책 후 감정을 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 산책 후 감정을 반환하도록 설정
        val expectedEmotion = "REFRESHED"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("post_walking_emotion")] } returns expectedEmotion
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getPostWalkingEmotion 호출
        val result = walkingDataStore.getPostWalkingEmotion()

        // Then: 올바른 감정이 반환됨
        Assert.assertEquals(expectedEmotion, result)
    }

    @Test
    fun `Getter 메소드들 - 값이 없을 때 null 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[longPreferencesKey("walking_start_time")] } returns null
        every { mockPreferences[intPreferencesKey("walking_step_count")] } returns null
        every { mockPreferences[longPreferencesKey("walking_duration")] } returns null
        every { mockPreferences[booleanPreferencesKey("walking_is_paused")] } returns null
        every { mockPreferences[booleanPreferencesKey("is_walking_active")] } returns null
        every { mockPreferences[stringPreferencesKey("pre_walking_emotion")] } returns null
        every { mockPreferences[stringPreferencesKey("post_walking_emotion")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 각 getter 메소드 호출
        val startTime = walkingDataStore.getWalkingStartTime()
        val stepCount = walkingDataStore.getWalkingStepCount()
        val duration = walkingDataStore.getWalkingDuration()
        val isPaused = walkingDataStore.getWalkingIsPaused()
        val isActive = walkingDataStore.getIsWalkingActive()
        val preEmotion = walkingDataStore.getPreWalkingEmotion()
        val postEmotion = walkingDataStore.getPostWalkingEmotion()

        // Then: 모두 null이 반환됨
        assertNull(startTime)
        assertNull(stepCount)
        assertNull(duration)
        assertNull(isPaused)
        assertNull(isActive)
        assertNull(preEmotion)
        assertNull(postEmotion)
    }

    // ===== clearWalkingData 테스트 =====

    @Test
    fun `clearWalkingData - 모든 산책 데이터를 올바르게 삭제해야 함`() = runTest {
        // When: clearWalkingData 호출
        walkingDataStore.clearWalkingData()

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) }
    }

    // ===== 통합 테스트 =====

    @Test
    fun `setter와 getter 통합 테스트 - 저장 후 조회가 올바르게 동작해야 함`() = runTest {
        // Given: 설정할 값들
        val testData = mapOf(
            "is_walking_active" to true,
            "walking_start_time" to 1700000000000L,
            "walking_step_count" to 2000,
            "walking_duration" to 2400000L,
            "walking_is_paused" to false,
            "pre_walking_emotion" to "ENERGETIC",
            "post_walking_emotion" to "FULFILLED"
        )

        // Mock Preferences 설정
        val mockPreferences = mockk<Preferences>()
        testData.forEach { (key, value) ->
            when (key) {
                "is_walking_active" -> every { mockPreferences[booleanPreferencesKey(key)] } returns value as Boolean
                "walking_start_time", "walking_duration" -> every { mockPreferences[longPreferencesKey(key)] } returns value as Long
                "walking_step_count" -> every { mockPreferences[intPreferencesKey(key)] } returns value as Int
                "walking_is_paused" -> every { mockPreferences[booleanPreferencesKey(key)] } returns value as Boolean
                "pre_walking_emotion", "post_walking_emotion" -> every { mockPreferences[stringPreferencesKey(key)] } returns value as String
            }
        }
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 각 getter 메소드로 값 조회
        val isActive = walkingDataStore.getIsWalkingActive()
        val startTime = walkingDataStore.getWalkingStartTime()
        val stepCount = walkingDataStore.getWalkingStepCount()
        val duration = walkingDataStore.getWalkingDuration()
        val isPaused = walkingDataStore.getWalkingIsPaused()
        val preEmotion = walkingDataStore.getPreWalkingEmotion()
        val postEmotion = walkingDataStore.getPostWalkingEmotion()

        // Then: 설정한 값들이 올바르게 반환됨
        Assert.assertEquals(testData["is_walking_active"], isActive)
        Assert.assertEquals(testData["walking_start_time"], startTime)
        Assert.assertEquals(testData["walking_step_count"], stepCount)
        Assert.assertEquals(testData["walking_duration"], duration)
        Assert.assertEquals(testData["walking_is_paused"], isPaused)
        Assert.assertEquals(testData["pre_walking_emotion"], preEmotion)
        Assert.assertEquals(testData["post_walking_emotion"], postEmotion)
    }
}