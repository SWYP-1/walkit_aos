package team.swyp.sdu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.presentation.viewmodel.PokemonUiState
import team.swyp.sdu.presentation.viewmodel.PokemonViewModel

@Composable
fun PokemonSearchScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: PokemonViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "포켓몬 검색",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("포켓몬 이름 입력") },
                placeholder = { Text("예: pikachu, charizard") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = {
                    if (searchText.isNotBlank()) {
                        viewModel.searchPokemon(searchText.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchText.isNotBlank() && uiState !is PokemonUiState.Loading,
            ) {
                Text("검색")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is PokemonUiState.Initial -> {
                    Text(
                        text = "포켓몬 이름을 입력하고 검색 버튼을 눌러주세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is PokemonUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PokemonUiState.Success -> {
                    // 성공 시 상세 화면으로 이동
                    LaunchedEffect(state.pokemon.name) {
                        onNavigateToDetail(state.pokemon.name)
                    }
                    Text(
                        text = "로딩 중...",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                is PokemonUiState.Error -> {
                    Text(
                        text = "오류: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
