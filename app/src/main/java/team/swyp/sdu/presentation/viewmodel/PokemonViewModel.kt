package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.api.Pokemon
import team.swyp.sdu.data.api.PokemonApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PokemonViewModel
    @Inject
    constructor(
        private val apiService: PokemonApiService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PokemonUiState>(PokemonUiState.Initial)
        val uiState: StateFlow<PokemonUiState> = _uiState.asStateFlow()

        fun searchPokemon(name: String) {
            if (name.isBlank()) {
                _uiState.value = PokemonUiState.Initial
                return
            }

            viewModelScope.launch {
                Timber.d("포켓몬 검색 시작: $name")
                _uiState.value = PokemonUiState.Loading
                try {
                    val pokemon = apiService.getPokemon(name.lowercase())
                    Timber.d("포켓몬 검색 성공: ${pokemon.name}")
                    _uiState.value = PokemonUiState.Success(pokemon)
                } catch (e: Exception) {
                    Timber.e(e, "포켓몬 검색 실패: $name")
                    _uiState.value = PokemonUiState.Error(e.message ?: "포켓몬을 찾을 수 없습니다")
                }
            }
        }
    }

sealed interface PokemonUiState {
    data object Initial : PokemonUiState

    data object Loading : PokemonUiState

    data class Success(
        val pokemon: Pokemon,
    ) : PokemonUiState

    data class Error(
        val message: String,
    ) : PokemonUiState
}
