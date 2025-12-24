package team.swyp.sdu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography
import java.time.LocalDate
import java.time.YearMonth

/**
 * Wheel Date Picker 다이얼로그 컴포넌트
 * LazyColumn + Snap 방식을 사용한 날짜 선택 다이얼로그
 *
 * @param showDialog 다이얼로그 표시 여부
 * @param initialDate 초기 선택 날짜
 * @param onDateSelected 날짜 선택 시 호출되는 콜백
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param visibleCount 화면에 보이는 항목 개수 (기본 3, 홀수여야 함)
 * @param itemHeight 각 항목의 높이
 * @param minYear 선택 가능한 최소 년도
 * @param maxYear 선택 가능한 최대 년도
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelDatePickerDialog(
    showDialog: Boolean,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    visibleCount: Int = 3,
    itemHeight: Dp = 48.dp,
    minYear: Int = 1900,
    maxYear: Int = 2100,
) {
    if (showDialog) {
        // visibleCount는 반드시 홀수
        val safeVisible = if (visibleCount % 2 == 0) visibleCount + 1 else visibleCount
        val sideCount = (safeVisible - 1) / 2

        // 날짜 범위 생성
        val years = (minYear..maxYear).toList()
        val months = (1..12).toList()

        // 각 컬럼의 선택된 인덱스 - initialDate를 기준으로 초기화
        var selectedYearIndex by remember(initialDate) {
            mutableStateOf(years.indexOf(initialDate.year).coerceIn(0, years.lastIndex))
        }
        var selectedMonthIndex by remember(initialDate) {
            mutableStateOf(months.indexOf(initialDate.monthValue).coerceIn(0, months.lastIndex))
        }

        // 선택된 년/월에 따른 일 범위 계산
        val currentYear = years[selectedYearIndex]
        val currentMonth = months[selectedMonthIndex]
        val daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth()
        val days = (1..daysInMonth).toList()

        // 일 인덱스는 initialDate를 기준으로 초기화
        var selectedDayIndex by remember(initialDate) {
            val dateDaysInMonth = YearMonth.of(initialDate.year, initialDate.monthValue).lengthOfMonth()
            val dayOfMonth = initialDate.dayOfMonth.coerceIn(1, dateDaysInMonth)
            mutableStateOf(dayOfMonth - 1) // 1일이 인덱스 0이므로
        }

        // 날짜 문자열 생성
        val yearItems = years.map { "${it}년" }
        val monthItems = months.map { "${it}월" }
        val dayItems = days.map { "${it}일" }

        // 년도/월 변경 시 일 범위 조정
        LaunchedEffect(selectedYearIndex, selectedMonthIndex) {
            val year = years.getOrNull(selectedYearIndex) ?: return@LaunchedEffect
            val month = months.getOrNull(selectedMonthIndex) ?: return@LaunchedEffect

            val currentDaysInMonth = YearMonth.of(year, month).lengthOfMonth()
            // 일 인덱스가 유효 범위를 벗어나면 조정
            if (selectedDayIndex >= currentDaysInMonth) {
                selectedDayIndex = currentDaysInMonth - 1
            }
        }

        // 선택된 날짜 계산
        val selectedDate = remember(selectedYearIndex, selectedMonthIndex, selectedDayIndex) {
            val year = years.getOrNull(selectedYearIndex) ?: minYear
            val month = months.getOrNull(selectedMonthIndex) ?: 1
            val currentDaysInMonth = YearMonth.of(year, month).lengthOfMonth()
            val day = (selectedDayIndex + 1).coerceIn(1, currentDaysInMonth)
            LocalDate.of(year, month, day)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "날짜 선택",
                    style = MaterialTheme.walkItTypography.headingM
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 20.dp),
                ) {
                    // 통합된 중앙 선택 영역 하이라이트
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(itemHeight * 1.08f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SemanticColor.backgroundWhiteSecondary),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 년도 피커
                        WheelDatePickerColumn(
                            items = yearItems,
                            selectedIndex = selectedYearIndex,
                            onIndexChanged = { selectedYearIndex = it },
                            visibleCount = safeVisible,
                            itemHeight = itemHeight,
                            sideCount = sideCount,
                            modifier = Modifier.weight(1f),
                        )

                        // 월 피커
                        WheelDatePickerColumn(
                            items = monthItems,
                            selectedIndex = selectedMonthIndex,
                            onIndexChanged = { selectedMonthIndex = it },
                            visibleCount = safeVisible,
                            itemHeight = itemHeight,
                            sideCount = sideCount,
                            modifier = Modifier.weight(1f),
                        )

                        // 일 피커
                        WheelDatePickerColumn(
                            items = dayItems,
                            selectedIndex = selectedDayIndex,
                            onIndexChanged = { selectedDayIndex = it },
                            visibleCount = safeVisible,
                            itemHeight = itemHeight,
                            sideCount = sideCount,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDateSelected(selectedDate)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * 날짜 피커의 개별 컬럼 컴포넌트
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelDatePickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    visibleCount: Int,
    itemHeight: Dp,
    sideCount: Int,
    modifier: Modifier = Modifier,
) {
    val pickerHeight = itemHeight * visibleCount
    val density = LocalDensity.current

    val listState = rememberLazyListState(selectedIndex + sideCount)
    val fling = rememberSnapFlingBehavior(listState)

    var lastEmit by remember { mutableStateOf(selectedIndex) }
    var isScrollingProgrammatically by remember { mutableStateOf(false) }

    // items가 변경되면 리스트 상태 재설정
    LaunchedEffect(items.size) {
        val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
        if (safeIndex != lastEmit) {
            lastEmit = safeIndex
            isScrollingProgrammatically = true
            listState.animateScrollToItem(safeIndex + sideCount)
            isScrollingProgrammatically = false
        }
    }

    // 외부에서 selectedIndex가 변경되면 스크롤 위치 업데이트
    LaunchedEffect(selectedIndex) {
        val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
        if (safeIndex != lastEmit) {
            lastEmit = safeIndex
            val targetScrollIndex = safeIndex + sideCount
            val currentFirstVisible = listState.firstVisibleItemIndex
            val currentOffset = listState.firstVisibleItemScrollOffset
            if (currentFirstVisible != targetScrollIndex ||
                (currentFirstVisible == targetScrollIndex && currentOffset != 0)
            ) {
                isScrollingProgrammatically = true
                listState.animateScrollToItem(targetScrollIndex)
                isScrollingProgrammatically = false
            }
        }
    }

    // 스크롤 위치에 따라 선택된 인덱스 업데이트
    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
        listState.isScrollInProgress
    ) {
        if (listState.isScrollInProgress || isScrollingProgrammatically) return@LaunchedEffect

        val layout = listState.layoutInfo
        if (layout.visibleItemsInfo.isEmpty()) return@LaunchedEffect

        val centerPx = with(density) { pickerHeight.toPx() } / 2f

        val actualItems = layout.visibleItemsInfo.filter { item ->
            item.index >= sideCount && item.index < sideCount + items.size
        }

        if (actualItems.isEmpty()) return@LaunchedEffect

        val nearest = actualItems.minByOrNull { item ->
            val itemCenter = item.offset + item.size / 2f
            kotlin.math.abs(itemCenter - centerPx)
        }

        if (nearest == null) return@LaunchedEffect

        val absoluteItemIndex = nearest.index
        val realIndex = (absoluteItemIndex - sideCount).coerceIn(0, items.lastIndex)

        if (realIndex != lastEmit && realIndex in items.indices) {
            lastEmit = realIndex
            onIndexChanged(realIndex)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
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
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item,
                            style = if (isSelected) {
                                MaterialTheme.walkItTypography.headingM.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            } else {
                                MaterialTheme.walkItTypography.headingS.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                )
                            },
                            color = if (isSelected) {
                                SemanticColor.textBorderPrimary
                            } else {
                                SemanticColor.textBorderSecondary
                            },
                        )
                    }
                }

                // 아래쪽 spacer
                items(sideCount) {
                    Spacer(modifier = Modifier.height(itemHeight))
                }
            }

            // Fade overlay (상하 그라데이션)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SemanticColor.backgroundWhitePrimary,
                                SemanticColor.backgroundWhitePrimary.copy(alpha = 0.35f),
                                SemanticColor.backgroundWhitePrimary.copy(alpha = 0f),
                                SemanticColor.backgroundWhitePrimary.copy(alpha = 0.35f),
                                SemanticColor.backgroundWhitePrimary,
                            ),
                            startY = 0f,
                            endY = with(density) { pickerHeight.toPx() },
                        ),
                    ),
            )
        }
    }
}

