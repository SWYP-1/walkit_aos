package team.swyp.sdu.ui.friend.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.ui.theme.Pretendard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.TypeScale

/**
 * 버튼 설정 데이터 클래스
 */
private data class ButtonConfig(
    val text: String,
    val backgroundColor: Color,
    val textColor: Color,
    val fontWeight: FontWeight,
)

/**
 * 친구 카드 컴포넌트
 *
 * Figma 디자인 기반 친구 목록/검색 결과 카드
 * - 프로필 이미지 (36x36, 원형)
 * - 닉네임 텍스트 (body M/medium, 16px)
 * - 팔로우 버튼 (상태에 따라 변경)
 * - 하단 Divider
 *
 * @param nickname 닉네임
 * @param imageName 프로필 이미지 이름 (선택사항)
 * @param followStatus 친구 요청 상태
 * @param onFollowClick 팔로우 버튼 클릭 핸들러
 * @param enabled 버튼 활성화 여부 (기본값: true)
 * @param modifier Modifier
 */
@Composable
fun FriendCard(
    nickname: String,
    imageName: String? = null,
    followStatus: FollowStatus,
    onCardClick: (String, FollowStatus) -> Unit,
    onFollowClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clickable(onClick = { onCardClick(nickname, followStatus) })) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticColor.backgroundWhitePrimary)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 프로필 이미지 + 닉네임
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 프로필 이미지 (36x36, 원형)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            SemanticColor.textBorderSecondaryInverse,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageName)
                            .build(),
                        contentDescription = "목걸이",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // 닉네임
                Text(
                    text = nickname,
                    fontFamily = Pretendard,
                    fontSize = TypeScale.BodyM, // 16sp
                    fontWeight = FontWeight.Medium, // Medium
                    lineHeight = (TypeScale.BodyM.value * 1.5f).sp, // lineHeight 1.5
                    letterSpacing = (-0.16f).sp, // letterSpacing -0.16px
                    color = SemanticColor.textBorderPrimary,
                )
            }

            // 오른쪽: 팔로우 버튼
            FollowButton(
                followStatus = followStatus,
                onClick = onFollowClick,
                enabled = enabled,
            )
        }

        // 하단 Divider
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = SemanticColor.textBorderSecondaryInverse,
            thickness = 1.dp,
        )
    }
}

/**
 * 팔로우 버튼 컴포넌트
 *
 * 상태에 따라 버튼 텍스트와 스타일이 변경됩니다.
 * - EMPTY/NONE: "팔로우" (기본 버튼 스타일)
 * - PENDING: "요청 중" (회색 배경, 흰색 텍스트)
 * - ACCEPTED/FOLLOWING: "팔로잉" (비활성화 상태)
 *
 * @param followStatus 팔로우 상태
 * @param onClick 클릭 핸들러
 * @param enabled 버튼 활성화 여부 (기본값: true)
 * @param modifier Modifier
 */
@Composable
private fun FollowButton(
    followStatus: FollowStatus,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val buttonConfig = when (followStatus) {
        FollowStatus.EMPTY -> {
            ButtonConfig(
                text = "팔로우",
                backgroundColor = SemanticColor.buttonPrimaryDefault,
                textColor = SemanticColor.textBorderPrimaryInverse,
                fontWeight = FontWeight.Medium,
            )
        }

        FollowStatus.PENDING -> {
            // Figma 디자인: 요청 중 버튼 (회색 배경 #818185, 흰색 텍스트, body S/semibold)
            ButtonConfig(
                text = "요청 중",
                backgroundColor = SemanticColor.iconGrey,
                textColor = SemanticColor.textBorderPrimaryInverse,
                fontWeight = FontWeight.SemiBold,
            )
        }

        FollowStatus.ACCEPTED -> {
            ButtonConfig(
                text = "팔로잉",
                backgroundColor = SemanticColor.buttonDisabled,
                textColor = SemanticColor.textBorderDisabled,
                fontWeight = FontWeight.Medium,
            )
        }

        FollowStatus.MYSELF -> {
            ButtonConfig(
                text = "",
                backgroundColor = Color.Transparent,
                textColor = Color.Transparent,
                fontWeight = FontWeight.Medium,
            )
        }

        FollowStatus.REJECTED -> {
            ButtonConfig(
                text = "거절됨",
                backgroundColor = Color.Transparent,
                textColor = Color.Transparent,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    val buttonText = buttonConfig.text
    val buttonColor = buttonConfig.backgroundColor
    val textColor = buttonConfig.textColor
    val fontWeight = buttonConfig.fontWeight

    // 자기 자신인 경우 버튼을 표시하지 않음
    if (followStatus == FollowStatus.MYSELF) {
        return
    }

    // PENDING 상태일 때는 body S 사용, 나머지는 body M 사용
    val fontSize = if (followStatus == FollowStatus.PENDING) {
        TypeScale.BodyS
    } else {
        TypeScale.BodyM
    }

    // PENDING 상태일 때는 vertical 패딩 4px, 나머지는 8px
    val verticalPadding = if (followStatus == FollowStatus.PENDING) {
        4.dp
    } else {
        8.dp
    }

    // PENDING 상태일 때는 letterSpacing -0.14px, 나머지는 -0.16px
    val letterSpacing = if (followStatus == FollowStatus.PENDING) {
        (-0.14f).sp
    } else {
        (-0.16f).sp
    }

    //TODO : && followStatus != FollowStatus.REJECTED 거절당하면 두번다시 못누르나?
    Button(
        onClick = onClick,
        enabled = enabled && followStatus != FollowStatus.ACCEPTED,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor,
            disabledContainerColor = buttonColor,
            disabledContentColor = textColor,
        ),
        shape = RoundedCornerShape(8.dp), // 둥근 모서리
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = verticalPadding,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = buttonText,
            fontFamily = Pretendard,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = (fontSize.value * 1.5f).sp, // lineHeight 1.5
            letterSpacing = letterSpacing,
            color = textColor,
        )
    }
}

