package swyp.team.walkit.ui.onboarding.howtouse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

// -------------------- 데이터 --------------------

val titleList = listOf(
    "매일 색다른 산책 기록과\n감정 변화를 기록해요",
    "서로의 산책 기록을 보며\n꾸준히 걸어볼까요?",
    "목표를 달성하여\n캐릭터를 성장시켜 보세요!"
)

val descriptionList = listOf(
    "산책을 통해 내 생각과 감정을 돌아보세요\n산책 중 찍은 사진과 일기도 같이 기록할 수 있어요",
    "친구를 팔로우하고 산책 기록을 함께 확인하며,\n서로의 걸음을 가볍게 응원해보세요\n감정 일기는 나만 볼 수 있으니 편하게 기록해도 괜찮아요",
    "목표를 달성할 때마다 캐릭터 경험치가 쌓여요\n한 걸음씩 걷다 보면, 어느새 자라난 캐릭터와\n산책의 즐거움을 느낄 수 있어요 🚶‍♀️"
)

val bgImage = listOf(
    R.drawable.bg_howtouse_01,
    R.drawable.bg_howtouse_02,
    R.drawable.bg_howtouse_03,
)

// -------------------- Screen --------------------

@Composable
fun HowToUseOnboardingScreen(
    modifier: Modifier = Modifier,
    onStart: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { titleList.size })
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {

        // 🔥 Gradient Layer 1 (이미지 영역용)
        val imageCenter: Offset
        val imageRadius: Float

        with(density) {
            imageCenter = Offset(
                x = (-196).dp.toPx() + 317.5.dp.toPx(),
                y = (-64).dp.toPx() + 317.5.dp.toPx()
            )
            imageRadius = 317.5.dp.toPx()
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE4FBFF).copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = imageCenter,
                        radius = imageRadius
                    )
                )
        )

        // 🔥 Gradient Layer 2 (텍스트 영역용)
        val textCenter: Offset
        val textRadius: Float

        with(density) {
            textCenter = Offset(
                x = (346.5 - 91).dp.toPx(),
                y = (330 + 346.5).dp.toPx()
            )
            textRadius = 346.5.dp.toPx()
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD8FFD6).copy(alpha = 0.7f),
                            Color(0x00D8FFD6),
                        ),
                        center = textCenter,
                        radius = textRadius
                    )
                )
        )

        // 🔥 실제 UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {

            Spacer(Modifier.height(66.dp))

            // ================= Pager =================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) { page ->
                Column(modifier = Modifier.fillMaxSize()) {

                    // 이미지 영역 (gradient 제거됨)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(375 / 403f)
                    ) {
                        Image(
                            painter = painterResource(bgImage[page]),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 텍스트 영역 (gradient 제거됨)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(29.dp))

                            Text(
                                text = titleList[page],
                                style = MaterialTheme.walkItTypography.headingS.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = SemanticColor.textBorderPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = descriptionList[page],
                                style = MaterialTheme.walkItTypography.bodyS,
                                color = SemanticColor.textBorderSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ================= 하단 =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleIndicator(pagerState.currentPage)

                Spacer(Modifier.height(40.dp))

                CtaButton(
                    text = if (pagerState.currentPage < titleList.size - 1) "다음으로" else "시작하기",
                    onClick = {
                        if (pagerState.currentPage < titleList.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onStart()
                        }
                    }
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// -------------------- Preview --------------------

@Preview(showBackground = true)
@Composable
fun Preview_HowToUseOnboarding() {
    WalkItTheme {
        HowToUseOnboardingScreen(
            onStart = {}
        )
    }
}