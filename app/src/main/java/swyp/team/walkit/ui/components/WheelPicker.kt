package swyp.team.walkit.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.GradientUtils

/**
 * WheelPicker – iOS 스타일의 휠 피커 구현 (Jetpack Compose)
 *
 * 이 컴포넌트는 LazyColumn + SnapFlingBehavior를 이용해
 * "가운데 1개의 항목이 선택되는" Wheel Picker 동작을 제공합니다.
 *
 * 📌 특징
 * - iOS UIDatePicker와 유사한 휠 스크롤 UX
 * - Snap fling 으로 스크롤 후 자동 중앙 정렬
 * - 중앙 선택 Highlight 영역 제공
 * - 상/하 Fade 처리로 휠 느낌 극대화
 * - 별도 라이브러리 필요 없음
 *
 * ---------------------------------------------------------------
 *
 * ⚠️ 중요: visibleCount 는 반드시 ‘홀수’여야 합니다!
 *
 * WheelPicker는 "항상 중앙에 1개의 항목이 존재"한다는 구조로 동작합니다.
 *
 * 예: visibleCount = 5
 *
 *   [위 패딩]
 *   Item 1
 * > Item 2   ← 중앙 선택 (항상 1개)
 *   Item 3
 *   [아래 패딩]
 *
 * visibleCount가 짝수일 경우 중앙이 2개가 되어
 * - 하이라이트와 selection 계산이 틀어지고
 * - 스냅 시 중앙 정렬이 불가능
 * - 리스트 첫/마지막 요소가 정확히 중앙에 오지 않는 문제가 발생합니다.
 *
 * 따라서 visibleCount가 짝수면 내부적으로 강제로 +1 하여 홀수로 바꿉니다.
 *
 * ---------------------------------------------------------------
 *
 * 📌 내부 동작 원리
 *
 * - safeVisible = visibleCount을 홀수로 보정한 값
 * - sideCount = safeVisible / 2
 *
 * LazyColumn의 **맨 앞/뒤에 sideCount 개의 Spacer**를 추가하여
 * 리스트의 첫 번째와 마지막 항목이 화면 중앙에 정확히 올 수 있도록 합니다.
 *
 * 리스트의 실제 중앙 위치는:
 *   pickerCenterPx = (itemHeight * safeVisible) / 2
 *
 * LazyColumn에서 현재 보이는 모든 Item의 center 좌표를 비교하여
 * 가장 중심에 가까운 항목을 선택값으로 결정합니다.
 *
 * ---------------------------------------------------------------
 *
 * @param items 보여줄 문자열 목록
 * @param modifier Compose Modifier
 * @param visibleCount 화면에 보이는 항목 개수 (기본 5, 반드시 홀수)
 * @param itemHeight 항목 높이
 * @param initialIndex 처음 선택될 항목의 index
 * @param onSelected 선택이 변경될 때마다 콜백 (centerIndex, value)
 *
 * ---------------------------------------------------------------
 *
 * 📌 사용 예시
 *
 * WheelPicker(
 *     items = (1..31).map { "$it 일" },
 *     visibleCount = 5,
 *     itemHeight = 40.dp,
 *     initialIndex = 0,
 *     onSelected = { index, value ->
 *         println("선택된 값: $value")
 *     }
 * )
 *
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 44.dp,
    initialIndex: Int = 0,
    onSelected: (index: Int, value: String) -> Unit,
) {
    // visibleCount 는 반드시 홀수
    val safeVisible = visibleCount.let { if (it % 2 == 0) it + 1 else it }
    val sideCount = (safeVisible - 1) / 2

    val initial = initialIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initial + sideCount)
    val fling = rememberSnapFlingBehavior(listState)

    var selectedIndex by remember { mutableStateOf(initial) }
    var lastEmit by remember { mutableStateOf(initial) }

    // picker 전체 높이
    val pickerHeight = itemHeight * safeVisible

    val density = LocalDensity.current

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val layout = listState.layoutInfo

        // 🎯 toPx() 올바른 사용
        val centerPx = with(density) { pickerHeight.toPx() } / 2f

        val nearest = layout.visibleItemsInfo.minByOrNull { item ->
            val itemCenter = item.offset + item.size / 2f
            kotlin.math.abs(itemCenter - centerPx)
        }

        val absIndex = nearest?.index ?: sideCount
        val realIndex = (absIndex - sideCount).coerceIn(0, items.lastIndex)

        if (realIndex != lastEmit) {
            lastEmit = realIndex
            selectedIndex = realIndex
            onSelected(realIndex, items[realIndex])
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier =
                    Modifier
                        .height(pickerHeight)
                        .fillMaxWidth(),
            ) {
                LazyColumn(
                    state = listState,
                    flingBehavior = fling,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 위쪽 spacer
                    items(sideCount) {
                        Spacer(modifier = Modifier.height(itemHeight))
                    }

                    // 실제 아이템
                    itemsIndexed(items) { index, item ->
                        val sel = index == selectedIndex
                        Box(
                            modifier =
                                Modifier
                                    .height(itemHeight)
                                    .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item,
                                style =
                                    if (sel) MaterialTheme.typography.headlineMedium
                                    else MaterialTheme.typography.bodyMedium,
                                color =
                                    if (sel) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                    }

                    // 아래쪽 spacer
                    items(sideCount) {
                        Spacer(modifier = Modifier.height(itemHeight))
                    }
                }

                // Fade overlay
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(
                                GradientUtils.centerFade(
                                    surfaceColor = MaterialTheme.colorScheme.surface,
                                    startY = 0f,
                                    endY = with(density) { pickerHeight.toPx() },
                                ),
                            ),
                )

                // 중앙 highlight
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(itemHeight * 1.08f)
                            .padding(horizontal = 36.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                )
            }
        }
    }
}











