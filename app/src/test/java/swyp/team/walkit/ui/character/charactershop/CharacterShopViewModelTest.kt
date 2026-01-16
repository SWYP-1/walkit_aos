package swyp.team.walkit.ui.character.charactershop

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertFalse

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CosmeticItem
import swyp.team.walkit.domain.model.EquipSlot
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.model.LottieAsset
import swyp.team.walkit.domain.model.LottieCharacterState
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.repository.CosmeticItemRepository
import swyp.team.walkit.domain.repository.PointRepository
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.domain.service.CharacterImageLoader
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.ui.dressroom.DressingRoomUiState

@ExperimentalCoroutinesApi
class CharacterShopViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockCosmeticItemRepository: CosmeticItemRepository
    private lateinit var mockCharacterRepository: CharacterRepository
    private lateinit var mockPointRepository: PointRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockLottieImageProcessor: LottieImageProcessor
    private lateinit var mockCharacterImageLoader: CharacterImageLoader

    private lateinit var viewModel: CharacterShopViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        mockCosmeticItemRepository = mockk()
        mockCharacterRepository = mockk()
        mockPointRepository = mockk()
        mockUserRepository = mockk()
        mockLottieImageProcessor = mockk()
        mockCharacterImageLoader = mockk()

        viewModel = CharacterShopViewModel(
            application = mockk(),
            cosmeticItemRepository = mockCosmeticItemRepository,
            characterRepository = mockCharacterRepository,
            pointRepository = mockPointRepository,
            userRepository = mockUserRepository,
            lottieImageProcessor = mockLottieImageProcessor,
            characterImageLoader = mockCharacterImageLoader,
            characterEventBus = mockk()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `카테고리 필터링이 올바르게 작동한다 - HEAD 카테고리 선택 시 HEAD 아이템만 반환`() = runTest {
        // Given
        val mockItems = listOf(
            createMockItem(1, EquipSlot.HEAD, owned = true),
            createMockItem(2, EquipSlot.BODY, owned = true),
            createMockItem(3, EquipSlot.FEET, owned = true),
            createMockItem(4, EquipSlot.HEAD, owned = false)
        )

        // Mock 초기 상태 설정
        setupMockSuccessState(mockItems, showOwnedOnly = false)

        // When - HEAD 카테고리 선택
        viewModel.changeCategoryFilter(EquipSlot.HEAD)

        // Then
        val currentState = viewModel.uiState.value as DressingRoomUiState.Success
        val filteredItems = currentState.items

        // HEAD 아이템만 필터링되어야 함
        Assert.assertEquals(2, filteredItems.size)
        Assert.assertTrue("필터링된 아이템이 모두 HEAD 위치여야 함", filteredItems.all { it.position == EquipSlot.HEAD })
    }

    @Test
    fun `보유 아이템 필터와 카테고리 필터가 AND 조건으로 적용된다`() = runTest {
        // Given
        val mockItems = listOf(
            createMockItem(1, EquipSlot.HEAD, owned = true),   // 보유 + HEAD
            createMockItem(2, EquipSlot.HEAD, owned = false),  // 미보유 + HEAD
            createMockItem(3, EquipSlot.BODY, owned = true),   // 보유 + BODY
            createMockItem(4, EquipSlot.BODY, owned = false)   // 미보유 + BODY
        )

        // Mock 초기 상태 설정 (보유 아이템만 보기 활성화)
        setupMockSuccessState(mockItems, showOwnedOnly = true)

        // When - HEAD 카테고리 선택
        viewModel.changeCategoryFilter(EquipSlot.HEAD)

        // Then
        val currentState = viewModel.uiState.value as DressingRoomUiState.Success
        val filteredItems = currentState.items

        // 보유한 HEAD 아이템만 표시되어야 함
        Assert.assertEquals(1, filteredItems.size)
        Assert.assertEquals(1, filteredItems[0].itemId)
        Assert.assertEquals(EquipSlot.HEAD, filteredItems[0].position)
        Assert.assertTrue("필터링된 첫 번째 아이템이 보유 상태여야 함", filteredItems[0].owned)
    }

    @Test
    fun `전체 카테고리 선택 시 모든 아이템이 표시된다`() = runTest {
        // Given
        val mockItems = listOf(
            createMockItem(1, EquipSlot.HEAD, owned = true),
            createMockItem(2, EquipSlot.BODY, owned = true),
            createMockItem(3, EquipSlot.FEET, owned = false)
        )

        setupMockSuccessState(mockItems, showOwnedOnly = false)

        // When - 전체 카테고리 선택 (null)
        viewModel.changeCategoryFilter(null)

        // Then
        val currentState = viewModel.uiState.value as DressingRoomUiState.Success
        val filteredItems = currentState.items

        Assert.assertEquals(3, filteredItems.size)
    }

    @Test
    fun `보유 아이템 토글이 올바르게 작동한다`() = runTest {
        // Given
        val mockItems = listOf(
            createMockItem(1, EquipSlot.HEAD, owned = true),
            createMockItem(2, EquipSlot.HEAD, owned = false),
            createMockItem(3, EquipSlot.BODY, owned = true)
        )

        setupMockSuccessState(mockItems, showOwnedOnly = false)

        // When - 보유 아이템만 보기 토글 ON
        viewModel.toggleShowOwnedOnly()

        // Then
        val currentState = viewModel.uiState.value as DressingRoomUiState.Success
        val filteredItems = currentState.items

        Assert.assertTrue("보유 아이템만 표시 모드여야 함", currentState.showOwnedOnly)
        Assert.assertEquals(2, filteredItems.size) // 보유 아이템만
        Assert.assertTrue("필터링된 아이템들이 모두 보유 상태여야 함", filteredItems.all { it.owned })
    }

    @Test
    fun `선택된 카테고리가 올바르게 업데이트된다`() = runTest {
        // When - BODY 카테고리 선택
        viewModel.changeCategoryFilter(EquipSlot.BODY)

        // Then
        val selectedCategory = viewModel.selectedCategory.first()
        Assert.assertEquals(EquipSlot.BODY, selectedCategory)
    }

    // 헬퍼 함수들
    private fun createMockItem(itemId: Int, position: EquipSlot, owned: Boolean): CosmeticItem {
        return CosmeticItem(
            itemId = itemId,
            name = "Test Item $itemId",
            imageName = "test_image_$itemId.png",
            position = position,
            owned = owned,
            point = 100
        )
    }

    private suspend fun setupMockSuccessState(items: List<CosmeticItem>, showOwnedOnly: Boolean) {
        // Mock repositories
        coEvery { mockUserRepository.getUser() } returns Result.Success(mockk())
        coEvery { mockCosmeticItemRepository.getCosmeticItems(null) } returns Result.Success(items)
        coEvery { mockPointRepository.getUserPoint() } returns Result.Success(1000)

        // 초기 데이터 로드
        viewModel.loadDressingRoom()

        // showOwnedOnly 상태 설정을 위해 직접 토글
        if (showOwnedOnly) {
            // 현재 상태를 확인하고 토글
            val currentState = viewModel.uiState.value as? DressingRoomUiState.Success
            if (currentState != null && !currentState.showOwnedOnly) {
                viewModel.toggleShowOwnedOnly()
            }
        }
    }
}
