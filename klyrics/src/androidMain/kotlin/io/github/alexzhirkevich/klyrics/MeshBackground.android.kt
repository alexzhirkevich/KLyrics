package io.github.alexzhirkevich.klyrics

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.IntSize

const val AGSL_DISTORTION_SHADER = """
    uniform shader content;
uniform float2 size;
uniform float time; // expected range: 0.0 to 1.0

half4 main(float2 fragCoord) {
    // Guard against zero/invalid size (e.g. before layout is ready)
    if (size.x <= 0.0 || size.y <= 0.0) {
        return half4(0.0, 0.0, 0.0, 1.0);
    }

    // Normalize coordinates between 0.0 and 1.0
    float2 uv = fragCoord / size;

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
    float2 distortedUV = uv + float2(waveX + waveDiag, waveY - waveDiag);

    // Clamp to valid texture bounds
    distortedUV = clamp(distortedUV, 0.0, 1.0);

    return content.eval(distortedUV * size);
}
"""

@Composable
actual fun rememberDistortionEffect(
    size: IntSize,
    time: Float
): RenderEffect? {
    // AGSL RuntimeShader-backed RenderEffect requires API 33 (Tiramisu)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return null
    }

    return rememberDistortionEffectApi33(size, time)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun rememberDistortionEffectApi33(
    size: IntSize,
    time: Float
): RenderEffect? {
    val runtimeShader = remember { RuntimeShader(AGSL_DISTORTION_SHADER) }

    return produceState<RenderEffect?>(null, size, time, runtimeShader) {
        if (size.width <= 0 || size.height <= 0) {
            value = null
        } else {
            runtimeShader.setFloatUniform(
                "size",
                size.width.toFloat(),
                size.height.toFloat()
            )
            runtimeShader.setFloatUniform("time", time)

            val androidRenderEffect = AndroidRenderEffect.createRuntimeShaderEffect(
                runtimeShader,
                "content"
            )

            value = androidRenderEffect.asComposeRenderEffect()
        }
    }.value
}