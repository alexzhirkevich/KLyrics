package io.github.alexzhirkevich.klyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

const val SKSL_LIQUID_SHADER = """
    uniform shader content;
    uniform vec2 size;
    uniform float time;

    half4 main(vec2 fragCoord) {
        // Normalize coordinates between 0.0 and 1.0
        vec2 uv = fragCoord / size;

        // Create organic, fluid distortion using sine waves
        float waveX = sin(uv.y * 4.0 + time * 0.5) * 0.08;
        float waveY = cos(uv.x * 3.0 - time * 0.4) * 0.08;

        // Apply distortion to the UV coordinates
        vec2 distortedUV = uv + vec2(waveX, waveY);

        // Ensure coordinates stay within texture bounds
        distortedUV = clamp(distortedUV, 0.0, 1.0);

        // Sample the underlying content texture at distorted coordinates
        return content.eval(distortedUV * size);
    }
"""

@Composable
actual fun rememberDistortionEffect(
    size: IntSize,
    time: Float
): RenderEffect? {
    val runtimeEffect = remember { RuntimeEffect.makeForShader(SKSL_LIQUID_SHADER) }
    return produceState<RenderEffect?>(null, size, time, runtimeEffect) {
        if (size.width <= 0 || size.height <= 0) {
            value = null
        } else {

            val shaderBuilder = RuntimeShaderBuilder(runtimeEffect).apply {
                uniform("size", size.width.toFloat(), size.height.toFloat())
                uniform("time", time)
            }
            val skiaImageFilter = ImageFilter.makeRuntimeShader(
                runtimeShaderBuilder = shaderBuilder,
                shaderName = "content", // Binds to your SKSL input
                input = null
            )

            value = skiaImageFilter.asComposeRenderEffect()
        }
    }.value
}