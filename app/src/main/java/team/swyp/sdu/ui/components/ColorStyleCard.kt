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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.TtTheme

@Composable
fun ColorStyleCard(modifier: Modifier = Modifier) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(492.dp),
        shape = RoundedCornerShape(0.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFAFAFA),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Title: "Use color styles"
            Text(
                text = "Use color styles",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF000000),
                letterSpacing = (-0.34).sp,
                lineHeight = 32.sp,
            )

            // Icon and description row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon: 4 dots arranged in a 2x2 grid
                Column(
                    modifier = Modifier.size(9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000).copy(alpha = 0.8f)),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000).copy(alpha = 0.8f)),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000).copy(alpha = 0.8f)),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000).copy(alpha = 0.8f)),
                        )
                    }
                }

                // Description text
                Text(
                    text = "Select a layer, then click on       next to the Fill property in the panel to the right.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF000000).copy(alpha = 0.8f),
                    letterSpacing = 0.055.sp,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Image placeholder area (337x332)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(332.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center,
            ) {
                // Placeholder for image
                Text(
                    text = "04",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom text: "Color me with a style"
            Text(
                text = "Color me with a style",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF007BE5),
                textAlign = TextAlign.Center,
                letterSpacing = 0.055.sp,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorStyleCardPreview() {
    TtTheme {
        ColorStyleCard(
            modifier = Modifier.padding(16.dp),
        )
    }
}
