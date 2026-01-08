package swyp.team.walkit.ui.walking.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.domain.contract.WalkingRawEvent
import swyp.team.walkit.domain.contract.WalkingTrackingContract
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.service.LocationManager
import swyp.team.walkit.utils.LocationConstants

@ExperimentalCoroutinesApi
class WalkingViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockCharacterRepository: CharacterRepository
    private lateinit var mockLocationManager: LocationManager
    private lateinit var mockWalkingTrackingContract: WalkingTrackingContract

    private lateinit var viewModel: WalkingViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        mockCharacterRepository = mockk()
        mockLocationManager = mockk()
        mockWalkingTrackingContract = mockk()

        viewModel = WalkingViewModel(
            characterRepository = mockCharacterRepository,
            locationManager = mockLocationManager,
            walkingTrackingContract = mockWalkingTrackingContract
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GPS 위치를 가져올 수 없을 때 서울 시청 좌표를 기본값으로 사용한다`() = runTest {
        // Given
        val mockCharacter = createMockCharacter()
        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns(null)
        `when`(mockCharacterRepository.getCharacterByLocation(
            LocationConstants.DEFAULT_LATITUDE,
            LocationConstants.DEFAULT_LONGITUDE
        )).thenReturn(Result.Success(mockCharacter))

        // When
        viewModel.loadWalkingCharacter()

        // Then
        io.mockk.verify { mockCharacterRepository.getCharacterByLocation(
            LocationConstants.DEFAULT_LATITUDE,  // 37.5665
            LocationConstants.DEFAULT_LONGITUDE  // 126.9780
        )
        assertEquals(mockCharacter, viewModel.walkingCharacter.value)
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

        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns(mockLocation)
        `when`(mockCharacterRepository.getCharacterByLocation(
            mockLocation.latitude,
            mockLocation.longitude
        )).thenReturn(Result.Success(mockCharacter))

        // When
        viewModel.loadWalkingCharacter()

        // Then
        io.mockk.verify { mockCharacterRepository.getCharacterByLocation(
            mockLocation.latitude,
            mockLocation.longitude
        )
        io.mockk.verify { mockCharacterRepository.getCharacterByLocation(
            LocationConstants.DEFAULT_LATITUDE,
            LocationConstants.DEFAULT_LONGITUDE
        ) // 서울 시청 좌표는 호출되지 않아야 함
    }

    @Test
    fun `캐릭터 정보 조회 실패 시 walkingCharacter가 null로 유지된다`() = runTest {
        // Given
        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns(null)
        `when`(mockCharacterRepository.getCharacterByLocation(
            LocationConstants.DEFAULT_LATITUDE,
            LocationConstants.DEFAULT_LONGITUDE
        )).thenReturn(Result.Error(Exception("API Error"), "캐릭터 조회 실패"))

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

        coEvery { mockLocationManager.getCurrentLocationOrLast() } returns(mockLocation)
        `when`(mockCharacterRepository.getCharacterByLocation(
            mockLocation.latitude,
            mockLocation.longitude
        )).thenReturn(Result.Error(Exception("GPS 위치 실패"), "GPS 위치로 조회 실패"))

        `when`(mockCharacterRepository.getCharacterByLocation(
            LocationConstants.DEFAULT_LATITUDE,
            LocationConstants.DEFAULT_LONGITUDE
        )).thenReturn(Result.Success(createMockCharacter()))

        // When
        viewModel.loadWalkingCharacter()

        // Then - GPS 위치로 먼저 시도한 후 기본 위치로 성공
        io.mockk.verify { mockCharacterRepository.getCharacterByLocation(
            mockLocation.latitude,
            mockLocation.longitude
        )
        io.mockk.verify { mockCharacterRepository.getCharacterByLocation(
            LocationConstants.DEFAULT_LATITUDE,
            LocationConstants.DEFAULT_LONGITUDE
        )
        assertEquals("테스트캐릭터", viewModel.walkingCharacter.value?.nickName)
    }

    @Test
    fun `기본 위치 좌표가 서울 시청 좌표와 일치한다`() {
        // Given & When & Then
        assertEquals(37.5665, LocationConstants.DEFAULT_LATITUDE, 0.0001)
        assertEquals(126.9780, LocationConstants.DEFAULT_LONGITUDE, 0.0001)
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

    // Mockito 헬퍼 함수들
    private fun <T> any(): T = org.mockito.kotlin.any()
}
