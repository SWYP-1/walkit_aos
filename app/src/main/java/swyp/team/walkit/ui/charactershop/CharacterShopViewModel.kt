package swyp.team.walkit.ui.charactershop

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 캐릭터 상점 UI 상태
 */
data class CharacterShopUiState(
    val selectedTabIndex: Int = 0,
)

/**
 * 캐릭터 상점 ViewModel
 */
@HiltViewModel
class CharacterShopViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterShopUiState())
    val uiState: StateFlow<CharacterShopUiState> = _uiState.asStateFlow()

    /**
     * 탭 선택
     */
    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }
}
