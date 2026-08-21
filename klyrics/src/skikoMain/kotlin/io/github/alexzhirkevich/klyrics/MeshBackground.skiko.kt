package io.github.alexzhirkevich.klyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

const val SKSL_DISTORTION_SHADER = """
   uniform shader content;
uniform vec2 size;
uniform float time; // expected range: 0.0 to 1.0

half4 main(vec2 fragCoord) {
    // Guard against zero/invalid size (e.g. before layout is ready)
    if (size.x <= 0.0 || size.y <= 0.0) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }

    // Normalize coordinates between 0.0 and 1.0
    vec2 uv = fragCoord / size;

    // Map time (0..1) to a full oscillation cycle so motion is smooth
    // and loops cleanly instead of feeling linear/clamped.
    float t = time * 6.28318530718; // 0..2*PI

    // Layered sine/cosine waves at different frequencies and phases
    // for organic, multi-directional distortion
    float waveX = sin(uv.y * 4.0 + t * 0.5) * 0.18
                + sin(uv.y * 9.0 - t * 0.9) * 0.09
                + sin(uv.x * 6.0 + t * 0.35) * 0.06;

    float waveY = cos(uv.x * 3.0 - t * 0.4) * 0.18
                + cos(uv.x * 8.0 + t * 0.75) * 0.09
                + cos(uv.y * 5.0 - t * 0.6) * 0.06;

    // Diagonal cross-term so corners/edges shift too, not just axis-aligned bands
    float waveDiag = sin((uv.x + uv.y) * 5.0 + t * 0.6) * 0.07;

    // Apply distortion to the UV coordinates
    vec2 distortedUV = uv + vec2(waveX + waveDiag, waveY - waveDiag);

    // Clamp to valid texture bounds (safe now that inputs can't be NaN/Inf)
    distortedUV = clamp(distortedUV, 0.0, 1.0);

    return content.eval(distortedUV * size);
}
"""

@Composable
actual fun rememberDistortionEffect(
    size: IntSize,
    time: Float
): RenderEffect? {
    val runtimeEffect = remember { RuntimeEffect.makeForShader(SKSL_DISTORTION_SHADER) }
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
                shaderName = "content",
                input = null
            )

            value = skiaImageFilter.asComposeRenderEffect()
        }
    }.value
}