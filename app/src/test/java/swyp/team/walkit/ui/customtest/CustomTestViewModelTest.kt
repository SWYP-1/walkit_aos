package swyp.team.walkit.ui.customtest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.domain.model.User
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.utils.DateUtils
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class CustomTestViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockWalkingSessionRepository: WalkingSessionRepository
    private lateinit var mockUserRepository: UserRepository

    private lateinit var viewModel: CustomTestViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        mockWalkingSessionRepository = mockk()
        mockUserRepository = mockk()

        viewModel = CustomTestViewModel(
            walkingSessionRepository = mockWalkingSessionRepository,
            userRepository = mockUserRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `더미 데이터가 실제 사용자 ID로 생성된다`() = runTest {
        // Given
        val mockUser = User(
            userId = 123L,
            nickname = "테스트유저",
            imageName = "test.jpg"
        )
        coEvery { mockUserRepository.getUser() } returns(Result.Success(mockUser))
        coEvery { mockWalkingSessionRepository.saveSessionLocalOnly(any(), any()) } returns "mock-id"

        // When
        viewModel.addDummySessions()

        // Then
        // 생성된 세션들이 userId = 123을 사용했는지 확인
        io.mockk.verify(exactly = 40) { mockWalkingSessionRepository.saveSessionLocalOnly(
            argThat { session -> session.userId == 123L },
            any()
        )
    }

    @Test
    fun `오늘 날짜 이전으로만 더미 데이터가 생성된다`() = runTest {
        // Given
        val mockUser = User(userId = 999L, nickname = "테스트", imageName = null)
        coEvery { mockUserRepository.getUser() } returns(Result.Success(mockUser))
        coEvery { mockWalkingSessionRepository.saveSessionLocalOnly(any(), any()) } returns "mock-id"

        val todayStart = DateUtils.getStartOfDay(System.currentTimeMillis())

        // When
        viewModel.addDummySessions()

        // Then
        // 모든 세션의 startTime이 오늘 이전인지 확인
        io.mockk.verify(exactly = 40) { mockWalkingSessionRepository.saveSessionLocalOnly(
            argThat { session ->
                val sessionDate = DateUtils.getStartOfDay(session.startTime)
                sessionDate < todayStart
            },
            any()
        )
    }

    @Test
    fun `사용자 정보 조회 실패 시 기본 userId 0L로 데이터 생성`() = runTest {
        // Given
        coEvery { mockUserRepository.getUser() } returns(
            Result.Error(Exception("User not found"), "사용자를 찾을 수 없습니다")
        )
        coEvery { mockWalkingSessionRepository.saveSessionLocalOnly(any(), any()) } returns "mock-id"

        // When
        viewModel.addDummySessions()

        // Then
        io.mockk.verify(exactly = 40) { mockWalkingSessionRepository.saveSessionLocalOnly(
            argThat { session -> session.userId == 0L },
            any()
        )
    }

    @Test
    fun `더미 데이터의 걸음 수 범위가 올바르다`() = runTest {
        // Given
        val mockUser = User(userId = 456L, nickname = "테스트", imageName = null)
        coEvery { mockUserRepository.getUser() } returns(Result.Success(mockUser))

        val capturedSessions = mutableListOf<WalkingSession>()
        `when`(mockWalkingSessionRepository.saveSessionLocalOnly(any(), any())).thenAnswer { invocation ->
            capturedSessions.add(invocation.arguments[0] as WalkingSession)
            "mock-id-${capturedSessions.size}"
        }

        // When
        viewModel.addDummySessions()

        // Then
        assertEquals(40, capturedSessions.size)
        capturedSessions.forEach { session ->
            // 걸음 수가 3000-8000 범위 내에 있는지 확인
            assertTrue("걸음 수가 범위를 벗어남: ${session.stepCount}",
                session.stepCount in 3000..8000)
        }
    }

    @Test
    fun `더미 데이터의 감정 타입이 유효하다`() = runTest {
        // Given
        val mockUser = User(userId = 789L, nickname = "테스트", imageName = null)
        coEvery { mockUserRepository.getUser() } returns(Result.Success(mockUser))

        val capturedSessions = mutableListOf<WalkingSession>()
        `when`(mockWalkingSessionRepository.saveSessionLocalOnly(any(), any())).thenAnswer { invocation ->
            capturedSessions.add(invocation.arguments[0] as WalkingSession)
            "mock-id-${capturedSessions.size}"
        }

        // When
        viewModel.addDummySessions()

        // Then
        assertEquals(40, capturedSessions.size)
        capturedSessions.forEach { session ->
            // 감정 타입이 유효한 EmotionType인지 확인
            val validEmotions = EmotionType.entries.map { it.name }
            assertTrue("유효하지 않은 감정 타입: ${session.preWalkEmotion}",
                validEmotions.contains(session.preWalkEmotion))
            assertTrue("유효하지 않은 감정 타입: ${session.postWalkEmotion}",
                validEmotions.contains(session.postWalkEmotion))
        }
    }

    @Test
    fun `더미 데이터의 날짜가 연속적이다`() = runTest {
        // Given
        val mockUser = User(userId = 111L, nickname = "테스트", imageName = null)
        coEvery { mockUserRepository.getUser() } returns(Result.Success(mockUser))

        val capturedSessions = mutableListOf<WalkingSession>()
        `when`(mockWalkingSessionRepository.saveSessionLocalOnly(any(), any())).thenAnswer { invocation ->
            capturedSessions.add(invocation.arguments[0] as WalkingSession)
            "mock-id-${capturedSessions.size}"
        }

        // When
        viewModel.addDummySessions()

        // Then
        assertEquals(40, capturedSessions.size)

        // 날짜가 내림차순으로 정렬되어 있는지 확인 (최근 날짜부터 과거로)
        val sortedSessions = capturedSessions.sortedByDescending { it.startTime }
        assertEquals(capturedSessions, sortedSessions)

        // 인접한 세션들 간의 날짜 차이가 1일인지 확인
        for (i in 0 until capturedSessions.size - 1) {
            val currentDate = DateUtils.getStartOfDay(capturedSessions[i].startTime)
            val nextDate = DateUtils.getStartOfDay(capturedSessions[i + 1].startTime)

            val dayDiff = TimeUnit.MILLISECONDS.toDays(currentDate - nextDate)
            assertEquals("날짜 차이가 1일이 아님: $dayDiff", 1L, dayDiff)
        }
    }

    @Test
    fun `더미 데이터의 노트에 올바른 형식이 포함된다`() = runTest {
        // Given
        val mockUser = User(userId = 222L, nickname = "테스트", imageName = null)
        coEvery { mockUserRepository.getUser() } returns(Result.Success(mockUser))

        val capturedSessions = mutableListOf<WalkingSession>()
        `when`(mockWalkingSessionRepository.saveSessionLocalOnly(any(), any())).thenAnswer { invocation ->
            capturedSessions.add(invocation.arguments[0] as WalkingSession)
            "mock-id-${capturedSessions.size}"
        }

        // When
        viewModel.addDummySessions()

        // Then
        assertEquals(40, capturedSessions.size)
        capturedSessions.forEachIndexed { index, session ->
            assertNotNull("노트가 null임", session.note)
            assertTrue("노트 형식이 올바르지 않음: ${session.note}",
                session.note?.contains("커스텀 테스트 더미 데이터 ${index + 1}일차") == true)
        }
    }

    // Mockito 헬퍼 함수들
    private fun <T> any(): T = org.mockito.kotlin.any()
    private fun <T> argThat(predicate: (T) -> Boolean) = org.mockito.kotlin.argThat(predicate)
    private fun <T> times(count: Int) = org.mockito.kotlin.times(count)
}
