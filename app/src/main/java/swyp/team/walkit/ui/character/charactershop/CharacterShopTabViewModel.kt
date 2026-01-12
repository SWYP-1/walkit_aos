package swyp.team.walkit.ui.character.charactershop

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 캐릭터 상점 탭 ViewModel (탭 선택만 담당)
 */
@HiltViewModel
class CharacterShopTabViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterShopTabUiState())
    val uiState: StateFlow<CharacterShopTabUiState> = _uiState.asStateFlow()

    /**
     * 탭 선택
     */
    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }
}


