package swyp.team.walkit.ui.character

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 캐릭터 상점 탭 타입
 */
enum class CharacterTabType {
    Category, // 캐릭터 카테고리
    Shop,     // 아이템 상점
}
/**
 * 캐릭터 상점 탭 ViewModel (탭 선택만 담당)
 */

/**
 * 캐릭터 상점 탭 UI 상태
 */
data class CharacterTabUiState(
    val selectedTabIndex: Int = 0,
)

@HiltViewModel
class CharacterTabViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterTabUiState())
    val uiState: StateFlow<CharacterTabUiState> = _uiState.asStateFlow()

    /**
     * 탭 선택
     */
    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }
}