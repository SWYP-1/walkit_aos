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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.walkItTypography
import java.time.LocalDate

/**
 * 날짜 선택 다이얼로그 컴포넌트
 * 년, 월, 일을 각각 선택할 수 있는 휠 피커 다이얼로그
 *
 * @param initialYear 초기 년도
 * @param initialMonth 초기 월 (1-12)
 * @param initialDay 초기 일
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 날짜 선택 확인 콜백 (year, month, day)
 */
@Composable
fun DatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit,
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    // 해당 월의 마지막 날짜 계산
    val daysInMonth = remember(selectedYear, selectedMonth) {
        try {
            LocalDate.of(selectedYear, selectedMonth, 1).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }

    // 년도 리스트 생성 (1901년 ~ 현재 년도)
    val years = remember {
        (1901..LocalDate.now().year).toList()
    }

    // 월 리스트 생성 (1-12)
    val months = remember {
        (1..12).toList()
    }

    // 일 리스트 생성 (선택된 월에 따라 동적 변경)
    val days = remember(selectedYear, selectedMonth, daysInMonth) {
        (1..daysInMonth).toList()
    }

    // 일자가 유효 범위를 벗어나면 자동으로 조정
    LaunchedEffect(selectedYear, selectedMonth, daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 년도 선택
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "년",
                            style = MaterialTheme.walkItTypography.bodyS,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        WheelPicker(
                            items = years.map { "$it" },
                            modifier = Modifier.height(200.dp),
                            visibleCount = 5,
                            itemHeight = 44.dp,
                            initialIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                            onSelected = { _, value ->
                                selectedYear = value.toInt()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 월 선택
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "월",
                            style = MaterialTheme.walkItTypography.bodyS,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        WheelPicker(
                            items = months.map { "$it" },
                            modifier = Modifier.height(200.dp),
                            visibleCount = 5,
                            itemHeight = 44.dp,
                            initialIndex = months.indexOf(selectedMonth).coerceAtLeast(0),
                            onSelected = { _, value ->
                                selectedMonth = value.toInt()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 일 선택
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "일",
                            style = MaterialTheme.walkItTypography.bodyS,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        WheelPicker(
                            items = days.map { "$it" },
                            modifier = Modifier.height(200.dp),
                            visibleCount = 5,
                            itemHeight = 44.dp,
                            initialIndex = days.indexOf(selectedDay).coerceAtLeast(0),
                            onSelected = { _, value ->
                                selectedDay = value.toInt()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedYear, selectedMonth, selectedDay)
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

