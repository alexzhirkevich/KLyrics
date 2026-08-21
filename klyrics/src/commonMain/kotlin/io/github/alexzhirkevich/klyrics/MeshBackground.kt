package io.github.alexzhirkevich.klyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ImageMeshGradient(
    image : Painter,
    modifier: Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(45_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
    )

    // Keep track of the actual view bounds dynamically
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val distortion = rememberDistortionEffect(viewSize, time)

    Box(
        modifier = modifier
            .clipToBounds()
    ) {
        Image(
            painter = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .graphicsLayer(
                    scaleX = 1.25f,
                    scaleY = 1.25f,
                    alpha = .75f
                )
        )

        Image(
            painter = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { viewSize = it }
                .blur(60.dp)
                .alpha(.35f)
                .graphicsLayer {
                    rotationZ = time * 360f
                }
                .graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                    renderEffect = distortion
                }
        )

//        Image(
//            painter = image,
//            contentDescription = null,
//            contentScale = ContentScale.Crop,
//            modifier = Modifier
//                .matchParentSize()
//                .onSizeChanged { viewSize = it }
//                .offset {
//                    IntOffset(0, viewSize.height/4)
//                }
//                .rotate(135f)
//                .blur(60.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
//                .alpha(.35f)
//                .graphicsLayer(
//                    scaleX = 2f,
//                    scaleY = 2f,
//                    renderEffect = distortion
//                )
//
//        )
    }
}
@Composable
expect fun rememberDistortionEffect(
    size: IntSize,
    time: Float
): RenderEffect?
