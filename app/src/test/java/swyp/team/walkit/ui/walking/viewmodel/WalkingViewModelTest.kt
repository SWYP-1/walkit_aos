package swyp.team.walkit.ui.walking.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.local.datastore.WalkingDataStore
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.domain.contract.WalkingTrackingContract
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.repository.GoalRepository
import swyp.team.walkit.domain.service.LocationManager
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.domain.service.filter.PathSmoother
import swyp.team.walkit.utils.LocationConstants

@ExperimentalCoroutinesApi
class WalkingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // Mock 의존성들
    private lateinit var mockWalkingTrackingContract: WalkingTrackingContract
    private lateinit var mockWalkingSessionRepository: WalkingSessionRepository
    private lateinit var mockLocationManager: LocationManager
    private lateinit var mockCharacterRepository: CharacterRepository
    private lateinit var mockGoalRepository: GoalRepository
    private lateinit var mockLottieImageProcessor: LottieImageProcessor
    private lateinit var mockPathSmoother: PathSmoother
    private lateinit var mockWalkingDataStore: WalkingDataStore
    private lateinit var mockContext: Context
    private lateinit var mockSavedStateHandle: SavedStateHandle

    private lateinit var viewModel: WalkingViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // 모든 의존성 모킹
        mockWalkingTrackingContract = mockk()
        mockWalkingSessionRepository = mockk()
        mockLocationManager = mockk()
        mockCharacterRepository = mockk()
        mockGoalRepository = mockk()
        mockLottieImageProcessor = mockk()
        mockPathSmoother = mockk()
        mockWalkingDataStore = mockk()
        mockContext = mockk()
        mockSavedStateHandle = mockk()

        // 기본 모킹 설정
        setupDefaultMocks()

        viewModel = WalkingViewModel(
            tracking = mockWalkingTrackingContract,
            walkingSessionRepository = mockWalkingSessionRepository,
            locationManager = mockLocationManager,
            characterRepository = mockCharacterRepository,
            goalRepository = mockGoalRepository,
            lottieImageProcessor = mockLottieImageProcessor,
            pathSmoother = mockPathSmoother,
            walkingDataStore = mockWalkingDataStore,
            context = mockContext,
            savedStateHandle = mockSavedStateHandle
        )
    }

    private fun setupDefaultMocks() {
        // 기본적인 Flow 모킹
        every { mockWalkingDataStore.isWalkingActive } returns kotlinx.coroutines.flow.flowOf(false)
        every { mockWalkingDataStore.walkingStartTime } returns kotlinx.coroutines.flow.flowOf(null)
        every { mockWalkingDataStore.walkingStepCount } returns kotlinx.coroutines.flow.flowOf(null)
        every { mockWalkingDataStore.walkingDuration } returns kotlinx.coroutines.flow.flowOf(null)
        every { mockWalkingDataStore.walkingIsPaused } returns kotlinx.coroutines.flow.flowOf(null)
        every { mockWalkingDataStore.preWalkingEmotion } returns kotlinx.coroutines.flow.flowOf(null)
        every { mockWalkingDataStore.postWalkingEmotion } returns kotlinx.coroutines.flow.flowOf(
            null
        )

        // 기본적인 suspend 함수 모킹
        coEvery { mockWalkingDataStore.getIsWalkingActive() } returns null
        coEvery { mockWalkingDataStore.getWalkingStartTime() } returns null
        coEvery { mockWalkingDataStore.getWalkingStepCount() } returns null
        coEvery { mockWalkingDataStore.getWalkingDuration() } returns null
        coEvery { mockWalkingDataStore.getWalkingIsPaused() } returns null
        coEvery { mockWalkingDataStore.getPreWalkingEmotion() } returns null
        coEvery { mockWalkingDataStore.getPostWalkingEmotion() } returns null

        // Repository 모킹
        coEvery {
            mockCharacterRepository.getCharacterByLocation(
                any(),
                any()
            )
        } returns Result.Success(createMockCharacter())
        coEvery { mockGoalRepository.getGoal() } returns Result.Success(mockk())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GPS 위치를 가져올 수 없을 때 서울 시청 좌표를 기본값으로 사용한다`() = runTest {
        // Given
        val mockCharacter = createMockCharacter()
        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns null
        coEvery {
            mockCharacterRepository.getCharacterByLocation(
                LocationConstants.DEFAULT_LATITUDE,
                LocationConstants.DEFAULT_LONGITUDE
            )
        } returns Result.Success(mockCharacter)

        // When
        viewModel.loadWalkingCharacter()

        // Then
        coVerify {
            mockCharacterRepository.getCharacterByLocation(
                LocationConstants.DEFAULT_LATITUDE,  // 37.5665
                LocationConstants.DEFAULT_LONGITUDE  // 126.9780
            )
        }
        Assert.assertEquals(mockCharacter, viewModel.walkingCharacter.value)
    }

    @Test
    fun `GPS 권한 있을 때 실제 위치를 우선 사용한다`() = runTest {
        // Given
        val mockLocation = LocationPoint(
            latitude = 35.123456,
            longitude = 129.987654,
            timestamp = System.currentTimeMillis()
        )
        val mockCharacter = createMockCharacter()

        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns mockLocation
        coEvery {
            mockCharacterRepository.getCharacterByLocation(
                mockLocation.latitude,
                mockLocation.longitude
            )
        } returns Result.Success(mockCharacter)

        // When
        viewModel.loadWalkingCharacter()

        // Then
        coVerify {
            mockCharacterRepository.getCharacterByLocation(
                mockLocation.latitude,
                mockLocation.longitude
            )
        }
        coVerify(exactly = 0) {
            mockCharacterRepository.getCharacterByLocation(
                LocationConstants.DEFAULT_LATITUDE,
                LocationConstants.DEFAULT_LONGITUDE
            )
        }
        Assert.assertEquals(mockCharacter, viewModel.walkingCharacter.value)
    }

    @Test
    fun `캐릭터 정보 조회 실패 시 walkingCharacter가 null로 유지된다`() = runTest {
        // Given
        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns null
        coEvery {
            mockCharacterRepository.getCharacterByLocation(
                LocationConstants.DEFAULT_LATITUDE,
                LocationConstants.DEFAULT_LONGITUDE
            )
        } returns Result.Error(Exception("API Error"), "캐릭터 조회 실패")

        // When
        viewModel.loadWalkingCharacter()

        // Then
        assertNull(viewModel.walkingCharacter.value)
    }

    @Test
    fun `GPS 위치와 기본 위치 모두 시도된다`() = runTest {
        // Given - 위치 정보가 있지만 캐릭터 조회 실패
        val mockLocation = LocationPoint(
            latitude = 35.123456,
            longitude = 129.987654,
            timestamp = System.currentTimeMillis()
        )

        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns mockLocation
        coEvery {
            mockCharacterRepository.getCharacterByLocation(
                mockLocation.latitude,
                mockLocation.longitude
            )
        } returns Result.Error(Exception("GPS 위치 실패"), "GPS 위치로 조회 실패")

        coEvery {
            mockCharacterRepository.getCharacterByLocation(
                LocationConstants.DEFAULT_LATITUDE,
                LocationConstants.DEFAULT_LONGITUDE
            )
        } returns Result.Success(createMockCharacter())

        // When
        viewModel.loadWalkingCharacter()

        // Then - GPS 위치로 먼저 시도한 후 기본 위치로 성공
        coVerify {
            mockCharacterRepository.getCharacterByLocation(
                mockLocation.latitude,
                mockLocation.longitude
            )
        }
        coVerify {
            mockCharacterRepository.getCharacterByLocation(
                LocationConstants.DEFAULT_LATITUDE,
                LocationConstants.DEFAULT_LONGITUDE
            )
        }
        Assert.assertEquals("테스트캐릭터", viewModel.walkingCharacter.value?.nickName)
    }

    @Test
    fun `기본 위치 좌표가 서울 시청 좌표와 일치한다`() {
        // Given & When & Then
        Assert.assertEquals(37.5665, LocationConstants.DEFAULT_LATITUDE, 0.0001)
        Assert.assertEquals(126.9780, LocationConstants.DEFAULT_LONGITUDE, 0.0001)
    }
    // 헬퍼 함수
    private fun createMockCharacter(): Character {
        return Character(
            nickName = "테스트캐릭터",
            level = 5,
            grade = Grade.SEED,
            backgroundImageName = "background_test.png",
            characterImageName = "character_test.png",
            headImage = null,
            bodyImage = null,
            feetImage = null
        )
    }
}
