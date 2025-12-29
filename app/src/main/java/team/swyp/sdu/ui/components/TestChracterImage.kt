package team.swyp.sdu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import kotlin.math.roundToInt

data class CharacterPart(
    val imageUrl: String,
    val anchorX: Float = 0.5f, // 0f = 왼쪽, 1f = 오른쪽
    val anchorY: Float = 0.5f  // 0f = top, 1f = bottom
)

fun createCharacterParts(
    feetUrl: String,
    bodyUrl: String,
    headUrl: String,
    customFeetAnchorY: Float? = null,
    customBodyAnchorY: Float? = null,
    customHeadAnchorY: Float? = null,
    customAnchorX: Float? = null
): Triple<CharacterPart, CharacterPart, CharacterPart> {
    val anchorX = customAnchorX ?: 0.5f
    val feet = CharacterPart(feetUrl, anchorX, customFeetAnchorY ?: 1f)
    val body = CharacterPart(bodyUrl, anchorX, customBodyAnchorY ?: 0.5f)
    val head = CharacterPart(headUrl, anchorX, customHeadAnchorY ?: 0.2f)
    return Triple(feet, body, head)
}

@Composable
fun TestCharacterWithAnchor(
    modifier: Modifier = Modifier,
    feet: CharacterPart,
    body: CharacterPart,
    head: CharacterPart,
    characterUrl: String
) {
    // 이미지 크기 측정을 위한 상태
    var feetWidth by remember { mutableStateOf(0) }
    var feetHeight by remember { mutableStateOf(0) }
    var bodyWidth by remember { mutableStateOf(0) }
    var bodyHeight by remember { mutableStateOf(0) }
    var headWidth by remember { mutableStateOf(0) }
    var headHeight by remember { mutableStateOf(0) }

    var parentWidth by remember { mutableStateOf(0) }
    var parentHeight by remember { mutableStateOf(0) }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            parentWidth = coordinates.size.width
            parentHeight = coordinates.size.height
        }
    ) {
        // 1. 캐릭터 전체 이미지는 가장 뒤에
        AsyncImage(
            model = characterUrl,
            contentDescription = "Character Base",
            modifier = Modifier.matchParentSize()
        )

        // 2. 발 (하단 중앙)
        AsyncImage(
            model = feet.imageUrl,
            contentDescription = "Feet",
            modifier = Modifier.onGloballyPositioned { coordinates ->
                feetWidth = coordinates.size.width
                feetHeight = coordinates.size.height
            }
                .offset {
                    IntOffset(
                        x = (parentWidth * feet.anchorX - feetWidth * feet.anchorX).roundToInt(),
                        y = (parentHeight - feetHeight * feet.anchorY).roundToInt()
                    )
                }
        )

        // 3. 몸 (중앙)
        if (feetHeight > 0) {
            AsyncImage(
                model = body.imageUrl,
                contentDescription = "Body",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    bodyWidth = coordinates.size.width
                    bodyHeight = coordinates.size.height
                }
                    .offset {
                        IntOffset(
                            x = (parentWidth * body.anchorX - bodyWidth * body.anchorX).roundToInt(),
                            y = (parentHeight * body.anchorY - bodyHeight * body.anchorY).roundToInt()
                        )
                    }
            )
        }

        // 4. 머리 (상단)
        if (bodyHeight > 0) {
            AsyncImage(
                model = head.imageUrl,
                contentDescription = "Head",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    headWidth = coordinates.size.width
                    headHeight = coordinates.size.height
                }
                    .offset {
                        IntOffset(
                            x = (parentWidth * head.anchorX - headWidth * head.anchorX).roundToInt(),
                            y = (parentHeight * head.anchorY - headHeight * head.anchorY).roundToInt()
                        )
                    }
            )
        }
    }
}