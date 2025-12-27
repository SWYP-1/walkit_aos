package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.ui.components.CtaButton
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
        .mapNotNull { entry -> cartItems.find { it.itemId == entry.key }?.price }
        .sum()


    Column(
        Modifier
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
                Icon(painterResource(R.drawable.ic_action_clear), contentDescription = "close")
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

        // 3️⃣ 아이템 리스트
        cartItems.forEach { item ->
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.walkItTypography.bodyM.copy(fontWeight = FontWeight.Medium),
                        color = SemanticColor.textBorderPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${formatNumber(item.price)}P",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderPrimary
                    )
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

        // 4️⃣ 하단 구매 버튼
        CtaButton(
            text = "구매하기",
            enabled = checkedItems.values.any { it },
            onClick = {
                val itemsToPurchase = cartItems.filter { checkedItems[it.itemId] == true }
                onPurchase(itemsToPurchase)
            },
            modifier = Modifier.fillMaxWidth(),
            icon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SemanticColor.textBorderPrimaryInverse),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = checkedItems.size.toString(),
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.stateGreenTertiary,
                    )
                }

            }
        )
    }
}

@Composable
@Preview(showBackground = true)
fun CartDialogPreview_SomeUnchecked() {
    val dummyItems = listOf(
        CosmeticItem(
            itemId = 1,
            name = "헤어1",
            price = 100,
            owned = false,
            imageName = "df",
            position = EquipSlot.HEAD
        ),
        CosmeticItem(
            itemId = 2,
            name = "헤어2",
            price = 2000,
            owned = false,
            imageName = "df",
            position = EquipSlot.BODY
        ),
        CosmeticItem(
            itemId = 3,
            name = "헤어3",
            price = 150,
            owned = false,
            imageName = "df",
            position = EquipSlot.FEET
        ),
    )
    // Preview용으로 일부 체크 해제
    WalkItTheme {
        CartDialog(
            cartItems = dummyItems,
            myPoints = 1000,
            onDismiss = {},
            onPurchase = {}
        )
    }
}
