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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import java.time.LocalDate
import java.time.YearMonth

/**
 * iOS 스타일 회전 날짜 피커 컴포넌트
 *
 * Figma 디자인에 맞춘 iOS 스타일의 날짜 선택 피커입니다.
 * 년, 월, 일을 각각 독립적으로 회전하여 선택할 수 있습니다.
 *
 * @param selectedDate 현재 선택된 날짜
 * @param modifier Modifier
 * @param visibleCount 화면에 보이는 항목 개수 (기본 3, 홀수여야 함)
 * @param itemHeight 각 항목의 높이
 * @param minYear 선택 가능한 최소 년도
 * @param maxYear 선택 가능한 최대 년도
 * @param onDateSelected 날짜가 변경될 때 호출되는 콜백
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosDatePicker(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    itemHeight: Dp = 48.dp,
    minYear: Int = 1900,
    maxYear: Int = 2100,
    onDateSelected: (LocalDate) -> Unit,
) {
    // visibleCount는 반드시 홀수
    val safeVisible = if (visibleCount % 2 == 0) visibleCount + 1 else visibleCount
    val sideCount = (safeVisible - 1) / 2

    // 날짜 범위 생성
    val years = (minYear..maxYear).toList()
    val months = (1..12).toList()

    // 각 컬럼의 선택된 인덱스 - selectedDate를 기준으로 초기화
    var selectedYearIndex by remember(selectedDate) { 
        mutableStateOf(years.indexOf(selectedDate.year).coerceIn(0, years.lastIndex)) 
    }
    var selectedMonthIndex by remember(selectedDate) { 
        mutableStateOf(months.indexOf(selectedDate.monthValue).coerceIn(0, months.lastIndex)) 
    }
    
    // 선택된 년/월에 따른 일 범위 계산
    val currentYear = years[selectedYearIndex]
    val currentMonth = months[selectedMonthIndex]
    val daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth()
    val days = (1..daysInMonth).toList()
    
    // 일 인덱스는 selectedDate를 기준으로 초기화
    var selectedDayIndex by remember(selectedDate) { 
        val dateDaysInMonth = YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()
        val dayOfMonth = selectedDate.dayOfMonth.coerceIn(1, dateDaysInMonth)
        mutableStateOf(dayOfMonth - 1) // 1일이 인덱스 0이므로
    }

    // 날짜 문자열 생성
    val yearItems = years.map { "${it}년" }
    val monthItems = months.map { "${it}월" }
    val dayItems = days.map { "${it}일" }

    // 사용자 상호작용으로 인덱스가 변경되었는지 추적하는 플래그
    var isUserInteraction by remember { mutableStateOf(false) }

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

    // 날짜 변경 감지 및 콜백 호출 (사용자가 직접 스크롤해서 변경한 경우만)
    LaunchedEffect(selectedYearIndex, selectedMonthIndex, selectedDayIndex) {
        // 사용자 상호작용이 아니면 콜백 호출하지 않음
        if (!isUserInteraction) return@LaunchedEffect
        
        // 인덱스가 유효한 범위인지 확인
        if (selectedYearIndex !in years.indices || 
            selectedMonthIndex !in months.indices) {
            return@LaunchedEffect
        }
        
        val year = years[selectedYearIndex]
        val month = months[selectedMonthIndex]
        
        // 일 범위 재계산
        val currentDaysInMonth = YearMonth.of(year, month).lengthOfMonth()
        val validDayIndex = selectedDayIndex.coerceIn(0, currentDaysInMonth - 1)
        val day = validDayIndex + 1 // 인덱스는 0부터 시작하므로 +1
        
        val newDate = LocalDate.of(year, month, day)
        
        // 현재 선택된 날짜와 다를 때만 콜백 호출
        if (newDate != selectedDate) {
            onDateSelected(newDate)
            // 콜백 호출 후 플래그 리셋 (외부 업데이트가 실행될 수 있도록)
            isUserInteraction = false
        } else {
            // 날짜가 같으면 사용자 상호작용 완료로 간주
            isUserInteraction = false
        }
    }
    
    // 외부에서 selectedDate가 변경되면 인덱스 업데이트
    LaunchedEffect(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth) {
        // 사용자 상호작용 중이면 외부 업데이트 무시
        if (isUserInteraction) return@LaunchedEffect
        
        val yearIndex = years.indexOf(selectedDate.year).coerceIn(0, years.lastIndex)
        val monthIndex = months.indexOf(selectedDate.monthValue).coerceIn(0, months.lastIndex)
        
        // 현재 선택된 년/월에 따른 일 범위 재계산
        val currentDaysInMonth = YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()
        val dayOfMonth = selectedDate.dayOfMonth.coerceIn(1, currentDaysInMonth)
        val dayIndex = dayOfMonth - 1 // 1일이 인덱스 0이므로
        
        // 현재 인덱스로부터 계산된 날짜
        val currentYear = years.getOrNull(selectedYearIndex)
        val currentMonth = months.getOrNull(selectedMonthIndex)
        val currentDaysInMonthForIndex = if (currentYear != null && currentMonth != null) {
            YearMonth.of(currentYear, currentMonth).lengthOfMonth()
        } else {
            currentDaysInMonth
        }
        val currentDayIndex = selectedDayIndex.coerceIn(0, currentDaysInMonthForIndex - 1)
        val currentDay = currentDayIndex + 1
        val currentDate = if (currentYear != null && currentMonth != null) {
            LocalDate.of(currentYear, currentMonth, currentDay)
        } else {
            null
        }
        
        // 현재 인덱스로 계산된 날짜가 selectedDate와 다를 때만 업데이트
        if (currentDate != selectedDate) {
            // 모든 인덱스를 동시에 업데이트
            selectedYearIndex = yearIndex
            selectedMonthIndex = monthIndex
            if (dayIndex in 0 until currentDaysInMonth) {
                selectedDayIndex = dayIndex
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SemanticColor.backgroundWhitePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 20.dp),
        ) {
            // 통합된 중앙 선택 영역 하이라이트 (하나의 회색 박스) - 먼저 그려서 텍스트가 위에 표시되도록
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
                DatePickerColumn(
                    items = yearItems,
                    selectedIndex = selectedYearIndex,
                    onIndexChanged = { 
                        isUserInteraction = true
                        selectedYearIndex = it 
                    },
                    visibleCount = safeVisible,
                    itemHeight = itemHeight,
                    sideCount = sideCount,
                    modifier = Modifier.weight(1f),
                    showHighlight = false, // 개별 하이라이트 제거
                )

                // 월 피커
                DatePickerColumn(
                    items = monthItems,
                    selectedIndex = selectedMonthIndex,
                    onIndexChanged = { 
                        isUserInteraction = true
                        selectedMonthIndex = it 
                    },
                    visibleCount = safeVisible,
                    itemHeight = itemHeight,
                    sideCount = sideCount,
                    modifier = Modifier.weight(1f),
                    showHighlight = false, // 개별 하이라이트 제거
                )

                // 일 피커
                DatePickerColumn(
                    items = dayItems,
                    selectedIndex = selectedDayIndex,
                    onIndexChanged = { 
                        isUserInteraction = true
                        selectedDayIndex = it 
                    },
                    visibleCount = safeVisible,
                    itemHeight = itemHeight,
                    sideCount = sideCount,
                    modifier = Modifier.weight(1f),
                    showHighlight = false, // 개별 하이라이트 제거
                )
            }
        }
    }
}

/**
 * 날짜 피커의 개별 컬럼 컴포넌트
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatePickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    visibleCount: Int,
    itemHeight: Dp,
    sideCount: Int,
    modifier: Modifier = Modifier,
    showHighlight: Boolean = true, // 하이라이트 표시 여부
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
        // lastEmit과 다를 때만 업데이트 (무한 루프 방지)
        if (safeIndex != lastEmit) {
            lastEmit = safeIndex
            val targetScrollIndex = safeIndex + sideCount
            // 스크롤 위치가 다를 때만 애니메이션
            val currentFirstVisible = listState.firstVisibleItemIndex
            val currentOffset = listState.firstVisibleItemScrollOffset
            // 현재 스크롤 위치가 목표 위치와 다를 때만 스크롤
            if (currentFirstVisible != targetScrollIndex || 
                (currentFirstVisible == targetScrollIndex && currentOffset != 0)) {
                isScrollingProgrammatically = true
                listState.animateScrollToItem(targetScrollIndex)
                isScrollingProgrammatically = false
            }
        }
    }

    // 스크롤 위치에 따라 선택된 인덱스 업데이트 (사용자 스크롤만)
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, listState.isScrollInProgress) {
        // 스크롤 중이거나 프로그래밍 방식으로 스크롤 중이면 무시
        if (listState.isScrollInProgress || isScrollingProgrammatically) return@LaunchedEffect
        
        val layout = listState.layoutInfo
        if (layout.visibleItemsInfo.isEmpty()) return@LaunchedEffect
        
        val centerPx = with(density) { pickerHeight.toPx() } / 2f

        // 실제 아이템만 고려 (spacer 제외)
        val actualItems = layout.visibleItemsInfo.filter { item ->
            // spacer는 sideCount 범위 밖에 있음
            item.index >= sideCount && item.index < sideCount + items.size
        }
        
        if (actualItems.isEmpty()) return@LaunchedEffect
        
        // 중앙에 가장 가까운 아이템 찾기
        val nearest = actualItems.minByOrNull { item ->
            val itemCenter = item.offset + item.size / 2f
            kotlin.math.abs(itemCenter - centerPx)
        }

        if (nearest == null) return@LaunchedEffect
        
        // 절대 인덱스에서 실제 아이템 인덱스로 변환
        // nearest.index는 LazyColumn의 절대 인덱스이므로 sideCount를 빼야 함
        val absoluteItemIndex = nearest.index
        val realIndex = (absoluteItemIndex - sideCount).coerceIn(0, items.lastIndex)

        // 값이 변경되었고 유효한 범위 내에 있을 때만 업데이트
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
            // 중앙 선택 영역 하이라이트 (개별 컬럼용, showHighlight가 true일 때만 표시)
            // 먼저 그려서 텍스트가 위에 표시되도록 함
            if (showHighlight) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(itemHeight * 1.08f)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SemanticColor.backgroundWhiteSecondary),
                )
            }
            
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
                                    fontWeight = FontWeight.Medium,
                                )
                            } else {
                                MaterialTheme.walkItTypography.headingS.copy(
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

            // Fade overlay (상하 그라데이션) - 마지막에 그려서 위에 표시
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

@Composable
@Preview(showBackground = true)
fun IosDatePickerPreview() {
    WalkItTheme {
        IosDatePicker(
            selectedDate = LocalDate.of(2000, 12, 18),
            modifier = Modifier.padding(16.dp),
        ) { date ->
            // 날짜 변경 콜백
        }
    }
}

