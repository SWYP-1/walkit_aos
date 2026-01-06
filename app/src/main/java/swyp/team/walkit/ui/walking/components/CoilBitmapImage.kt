package swyp.team.walkit.ui.walking.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.ByteArrayOutputStream

/**
 * 로컬 Bitmap을 Coil을 통해 표시하고 EXIF 회전 적용
 *
 * @param context Context
 * @param bitmap 로컬 Bitmap (카메라 촬영 등)
 * @param modifier Compose Modifier
 * @param contentScale 이미지 Crop/Scale 방식
 */
@Composable
fun CoilBitmapImage(
    context: Context,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    bitmap?.let { bmp ->
        // Bitmap -> ByteArray (Coil에서 EXIF 회전 적용 가능)
        val stream = ByteArrayOutputStream()
        bmp.compress(CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(byteArray)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = "산책 사진",
            modifier = modifier.fillMaxWidth(),
            contentScale = contentScale
        )
    }
}
