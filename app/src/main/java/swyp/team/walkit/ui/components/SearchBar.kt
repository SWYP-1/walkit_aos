package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.Green4
import swyp.team.walkit.ui.theme.Pretendard
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.TypeScale
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 검색 바 컴포넌트
 *
 * 친구 검색 등에 사용되는 검색 입력 필드
 *
 * @param query 검색어
 * @param onQueryChange 검색어 변경 콜백
 * @param onClear 검색어 지우기 콜백
 * @param onSearch 검색 실행 콜백 (키보드 완료 버튼 클릭 시 호출)
 * @param placeholder 플레이스홀더 텍스트
 * @param modifier Modifier
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: (() -> Unit)? = null,
    placeholder: String = "친구의 닉네임을 검색해보세요.",
    modifier: Modifier = Modifier,
    backgroundColor: Color = SemanticColor.backgroundWhiteSecondary,
    textColor: Color = SemanticColor.textBorderPrimary,
    placeholderColor: Color = SemanticColor.textBorderTertiary,
    iconColor: Color = SemanticColor.iconBlack,
    borderColor: Color = Color.Transparent,
) {
    val textStyle = MaterialTheme.walkItTypography.bodyM.copy(
        color = textColor,
    )

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke() },
        ),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        } else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(color = placeholderColor),
                            )
                        }
                        innerTextField()
                    }

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "지우기",
                                tint = iconColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_search),
                            contentDescription = "검색",
                            tint = SemanticColor.iconDisabled,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    WalkItTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 기본 색상
            SearchBar(
                query = "",
                onQueryChange = {},
                onClear = {},
                onSearch = {},
            )

            // 입력된 상태
            SearchBar(
                query = "검색어",
                onQueryChange = {},
                onClear = {},
                onSearch = {},
            )

            // 커스텀 색상 + 테두리
            SearchBar(
                query = "커스텀 색상",
                onQueryChange = {},
                onClear = {},
                onSearch = {},
                backgroundColor = Color(0xFFFFF3E0),
                textColor = Color(0xFFBF360C),
                placeholderColor = Color(0xFFFFAB91),
                iconColor = Green4,
                borderColor = Color(0xFFD84315),
            )
        }
    }
}
