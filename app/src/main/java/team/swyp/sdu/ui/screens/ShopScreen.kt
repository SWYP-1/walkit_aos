package team.swyp.sdu.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.presentation.viewmodel.CosmeticItemViewModel
import team.swyp.sdu.presentation.viewmodel.CosmeticItemUiState
import team.swyp.sdu.presentation.viewmodel.PurchaseState
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize

/**
 * 상점 화면
 *
 * 아이템 구매 화면입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    viewModel: CosmeticItemViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 구매 성공 시 상태 초기화
    LaunchedEffect(purchaseState) {
        if (purchaseState is PurchaseState.Success) {
            viewModel.resetPurchaseState()
            viewModel.loadItems() // 목록 새로고침
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("상점") },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val state = uiState) {
                is CosmeticItemUiState.Loading -> {
                    CustomProgressIndicator(
                        size = ProgressIndicatorSize.Medium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is CosmeticItemUiState.Success -> {
                    ItemList(
                        items = state.availableItems,
                        purchasedProductIds = state.purchasedItems.map { it.productId }.toSet(),
                        onPurchaseClick = { productId ->
                            if (context is Activity) {
                                viewModel.startPurchaseFlow(context, productId)
                            }
                        },
                    )
                }

                is CosmeticItemUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "오류: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            // 구매 상태 표시
            when (val purchase = purchaseState) {
                is PurchaseState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CustomProgressIndicator(size = ProgressIndicatorSize.Small)
                                Text(
                                    text = "구매 처리 중...",
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }

                is PurchaseState.Pending -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CustomProgressIndicator(size = ProgressIndicatorSize.Small)
                                Text(
                                    text = "결제 처리 중...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(
                                    text = "구매가 완료되면 자동으로 처리됩니다",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }

                is PurchaseState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "구매 실패",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = purchase.message,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Button(
                                    onClick = { viewModel.resetPurchaseState() },
                                    modifier = Modifier.padding(top = 16.dp),
                                ) {
                                    Text("확인")
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

/**
 * 아이템 목록
 */
@Composable
private fun ItemList(
    items: List<CosmeticItem>,
    purchasedProductIds: Set<String>,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { item ->
            ItemCard(
                item = item,
                isPurchased = item.productId in purchasedProductIds,
                onPurchaseClick = { onPurchaseClick(item.productId) },
            )
        }
    }
}

/**
 * 아이템 카드
 */
@Composable
private fun ItemCard(
    item: CosmeticItem,
    isPurchased: Boolean,
    onPurchaseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Button(
                onClick = onPurchaseClick,
                enabled = !isPurchased,
            ) {
                Text(if (isPurchased) "구매 완료" else "구매")
            }
        }
    }
}








