package team.swyp.sdu.ui.dressroom.component

import android.graphics.drawable.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.InfoBanner
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.NumberUtils.formatNumber

@Composable
fun CartDialog(
    cartItems: List<CosmeticItem>,
    myPoints: Int,
    onDismiss: () -> Unit,
    onPurchase: (List<CosmeticItem>) -> Unit
) {
    // 1️⃣ 체크 상태 관리
    val checkedItems = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            cartItems.forEach { this[it.itemId] = true } // 기본 모두 체크
        }
    }

    // 2️⃣ 총합 계산
    val totalPrice = checkedItems.entries
        .filter { it.value } // 체크된 것만
        .mapNotNull { entry -> cartItems.find { it.itemId == entry.key }?.point }
        .sum()

    // 3️⃣ 포인트 부족 여부 확인
    val isInsufficientPoints = totalPrice > myPoints
    val hasCheckedItems = checkedItems.values.any { it }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SemanticColor.backgroundWhitePrimary)
                    .padding(vertical = 16.dp, horizontal = 20.dp)
            ) {
                // 1️⃣ Header
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "구매할 아이템을 확인해주세요!",
                        style = MaterialTheme.walkItTypography.bodyL.copy(fontWeight = FontWeight.Medium),
                        color = SemanticColor.textBorderPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_action_clear),
                            contentDescription = "close"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2️⃣ 포인트 표시
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(SemanticColor.backgroundWhiteTertiary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "보유 포인트",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderSecondary
                    )
                    Text(
                        text = "${formatNumber(myPoints)}P",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderSecondary
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 3️⃣ 아이템 리스트 (스크롤 가능)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) // 콘텐츠에 따라 높이 조정
                        .heightIn(max = 300.dp), // 최대 높이 제한
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cartItems) { item ->
                        val checked = checkedItems[item.itemId] ?: true
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    checkedItems[item.itemId] = isChecked
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SemanticColor.stateGreenPrimary,
                                    uncheckedColor = SemanticColor.textBorderSecondary
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.walkItTypography.bodyM.copy(fontWeight = FontWeight.Medium),
                                    color = SemanticColor.textBorderPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${formatNumber(item.point)}P",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                    color = SemanticColor.textBorderPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                HorizontalDivider(thickness = 1.dp, color = SemanticColor.backgroundWhiteTertiary)

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "총 사용 포인트",

                        // body M/medium
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "-",
                            // body M/semibold
                            style = MaterialTheme.walkItTypography.bodyM.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = SemanticColor.stateRedPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${formatNumber(totalPrice)}P",
                            // body M/semibold
                            style = MaterialTheme.walkItTypography.bodyM.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = SemanticColor.stateRedPrimary
                        )
                    }

                }


                Spacer(Modifier.height(24.dp))

                // 포인트 부족 시 InfoBanner 표시
                if (isInsufficientPoints) {
                    InfoBanner(
                        title = "보유 포인트를 초과해 구매가 어렵습니다",
                        backgroundColor = SemanticColor.stateRedTertiary,
                        textColor = SemanticColor.stateRedPrimary,
                        iconTint = SemanticColor.stateRedPrimary,
                        borderColor = SemanticColor.stateRedSecondary,
                        icon = { tint ->
                            Icon(
                                painter = painterResource(R.drawable.ic_action_clear),
                                contentDescription = "",
                                tint = tint
                            )
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // 4️⃣ 하단 구매 버튼
                CtaButton(
                    text = "구매하기 (${checkedItems.values.count { it }})",
                    enabled = hasCheckedItems && !isInsufficientPoints, // 체크된 아이템 있고 포인트 충분할 때만 활성화
                    onClick = {
                        val itemsToPurchase = cartItems.filter { checkedItems[it.itemId] == true }
                        onPurchase(itemsToPurchase)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun CartDialogPreview_SomeUnchecked() {
    val dummyItems = List(8) { index -> // 8개 아이템으로 스크롤 테스트
        CosmeticItem(
            itemId = index + 1,
            name = "아이템${index + 1}",
            point = (index + 1) * 100,
            owned = false,
            worn = false,
            imageName = "df",
            position = when (index % 3) {
                0 -> EquipSlot.HEAD
                1 -> EquipSlot.BODY
                else -> EquipSlot.FEET
            }
        )
    }
    // Preview용으로 일부 체크 해제
    WalkItTheme {
        CartDialog(
            cartItems = dummyItems,
            myPoints = 1000, // 포인트 부족 상태
            onDismiss = {},
            onPurchase = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun CartDialogPreview_SufficientPoints() {
    val dummyItems = listOf(
        CosmeticItem(
            itemId = 1,
            name = "헤어1",
            point = 100,
            owned = false,
            worn = false,
            imageName = "df",
            position = EquipSlot.HEAD
        ),
        CosmeticItem(
            itemId = 2,
            name = "헤어2",
            point = 200,
            owned = false,
            worn = false,
            imageName = "df",
            position = EquipSlot.BODY
        ),
    )
    WalkItTheme {
        CartDialog(
            cartItems = dummyItems,
            myPoints = 500, // 총합 300P < 500P → 포인트 충분 상태
            onDismiss = {},
            onPurchase = {}
        )
    }
}
