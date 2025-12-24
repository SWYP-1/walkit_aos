package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography
import java.time.LocalDate
import java.time.YearMonth

/**
 * 커스텀 Wheel Date Picker 컴포넌트
 * 년, 월, 일 순서로 배치된 Wheel 스타일 날짜 선택기
 *
 * @param selectedDate 현재 선택된 날짜
 * @param modifier Modifier
 * @param visibleCount 화면에 보이는 항목 개수 (기본 3, 홀수여야 함)
 * @param itemHeight 각 항목의 높이
 * @param minYear 선택 가능한 최소 년도
 * @param maxYear 선택 가능한 최대 년도
 * @param onDateSelected 날짜가 변경될 때 호출되는 콜백
 */
@Composable
fun CustomWheelDatePicker(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    visibleCount: Int = 3,
    itemHeight: Dp = 48.dp,
    minYear: Int = 1900,
    maxYear: Int = 2100,
    onDateSelected: (LocalDate) -> Unit,
) {
    // 날짜 범위 생성
    val years = (minYear..maxYear).toList()
    val months = (1..12).toList()

    // 각 컬럼의 선택된 인덱스
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
    var selectedDayIndex by remember(selectedDate, currentYear, currentMonth) {
        val dateDaysInMonth = YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()
        val dayOfMonth = if (selectedDate.year == currentYear && selectedDate.monthValue == currentMonth) {
            selectedDate.dayOfMonth.coerceIn(1, dateDaysInMonth)
        } else {
            daysInMonth // 년/월이 다르면 마지막 일로
        }
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

    // 사용자 상호작용으로 인덱스가 변경되었는지 추적하는 플래그
    var isUserInteraction by remember { mutableStateOf(false) }

    // 날짜 변경 감지 및 콜백 호출 (사용자가 직접 스크롤해서 변경한 경우만)
    LaunchedEffect(selectedYearIndex, selectedMonthIndex, selectedDayIndex) {
        // 사용자 상호작용이 아니면 콜백 호출하지 않음
        if (!isUserInteraction) return@LaunchedEffect

        // 인덱스가 유효한 범위인지 확인
        if (selectedYearIndex !in years.indices ||
            selectedMonthIndex !in months.indices
        ) {
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
            isUserInteraction = false // 플래그 리셋
        } else {
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
                // 년도 피커 (첫 번째)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WheelPicker(
                        items = yearItems,
                        modifier = Modifier.height(itemHeight * visibleCount),
                        visibleCount = visibleCount,
                        itemHeight = itemHeight,
                        initialIndex = selectedYearIndex,
                        onSelected = { index, _ ->
                            isUserInteraction = true
                            selectedYearIndex = index
                        },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 월 피커 (두 번째)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WheelPicker(
                        items = monthItems,
                        modifier = Modifier.height(itemHeight * visibleCount),
                        visibleCount = visibleCount,
                        itemHeight = itemHeight,
                        initialIndex = selectedMonthIndex,
                        onSelected = { index, _ ->
                            isUserInteraction = true
                            selectedMonthIndex = index
                        },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 일 피커 (세 번째)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WheelPicker(
                        items = dayItems,
                        modifier = Modifier.height(itemHeight * visibleCount),
                        visibleCount = visibleCount,
                        itemHeight = itemHeight,
                        initialIndex = selectedDayIndex,
                        onSelected = { index, _ ->
                            isUserInteraction = true
                            selectedDayIndex = index
                        },
                    )
                }
            }
        }
    }
}

