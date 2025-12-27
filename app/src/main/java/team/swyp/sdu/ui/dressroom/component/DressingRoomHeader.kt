package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun DressingRoomHeader(
    grade: Grade,
    nickName: String,
    onBack: () -> Unit = {},
    onClickQuestion: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(modifier = Modifier.size(24.dp), onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_backward),
                contentDescription = "arrow back"
            )
        }


        Row() {
            GradeBadge(grade)
            Spacer(Modifier.width(8.dp))
            Text(
                text = nickName,

                // heading S/medium
                style = MaterialTheme.walkItTypography.headingS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary,
            )
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onClickQuestion),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info_question),
                contentDescription = "arrow back"
            )
        }

    }
}