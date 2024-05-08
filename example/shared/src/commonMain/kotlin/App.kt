
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.klyrics.DefaultLyricsIdleIndicator
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsState
import io.github.alexzhirkevich.klyrics.rememberLyricsState
import klyrics.example.shared.generated.resources.Res
import klyrics.example.shared.generated.resources.anti
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private val HorizontalPadding = 24.dp

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)


@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {

        val transition = remember {
            IntAnimatable(initialValue = 0)
        }

        val resourceLyrics: Lyrics? by produceState<Lyrics?>(null) {
//        value = loadLyrics("files/work.json")
            value = loadLyrics("files/desperado.json")
        }

        val scope = rememberCoroutineScope()

        resourceLyrics?.let { lyrics ->

            fun snapTo(ms: Int) {
                scope.launch {
                    transition.snapTo(ms)
                    transition.animateTo(
                        targetValue = lyrics.duration,
                        animationSpec = tween(
                            lyrics.duration - ms,
                            easing = LinearEasing
                        )
                    )
                }
            }

            Scaffold(
                Modifier.fillMaxSize(),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0f to MaterialTheme.colorScheme.background,
                                    .9f to MaterialTheme.colorScheme.background,
                                    1f to Color.Transparent
                                )
                            )
                            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                            .padding(HorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.anti),
                            contentDescription = "Cover",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(MaterialTheme.shapes.small)
                        )

                        Column {
                            Text(
                                text = "Desperado",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Riana",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                bottomBar = {
                    Slider(
                        value = transition.value.toFloat(),
                        valueRange = 0f..lyrics.duration.toFloat(),
                        onValueChange = {
                            snapTo(it.toInt())
                        },
                        modifier = Modifier
                            .padding(
                                horizontal = HorizontalPadding / 2,
                                vertical = HorizontalPadding / 2
                            )
                    )
                }
            ) {

                val textStyle = TextStyle(
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.SemiBold
                ).copy(
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                )

                val lyricsState = rememberLyricsState(lyrics) {
                    transition.value
                }

                val focusedColor = LocalContentColor.current
                val unfocusedColor = focusedColor.copy(alpha = .5f)


                LaunchedEffect(0) {
                    snapTo(0)
                }


                BoxWithConstraints {
                    Lyrics(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = lyricsState,
                        lineModifier = { idx ->
                            val line = lyrics.lanes[idx]

                            Modifier.appleMusicLane(
                                state = lyricsState,
                                idx = idx,
                                constraints = constraints,
                                singleArtist = lyrics.lanes.all { it.alignment == Alignment.Start },
                                onClick = {
                                    snapTo(line.start)
                                }
                            )
                        },
                        textStyle = {
                            if (lyrics.lanes[it].alignment == Alignment.End)
                                textStyle.copy(
                                    textAlign = TextAlign.End
                                )
                            else textStyle
                        },
                        focusedColor = focusedColor,
                        unfocusedColor = unfocusedColor,
                        contentPadding = PaddingValues(
                            top = 42.dp + it.calculateTopPadding(),
                            bottom = 20.dp + it.calculateBottomPadding()
                        ),
                        idleIndicator = {
                            DefaultLyricsIdleIndicator(
                                index = it,
                                state = lyricsState,
                                focusedColor = focusedColor,
                                unfocusedColor = unfocusedColor,
                                modifier = Modifier
                                    .padding(
                                        horizontal = HorizontalPadding,
                                        vertical = 6.dp
                                    )
                            )
                        }
                    )
                }
            }
        }
    }
}


private fun Modifier.appleMusicLane(
    state: LyricsState,
    idx : Int,
    singleArtist : Boolean,
    constraints : Constraints,
    onClick : () -> Unit
) = composed {
    val density = LocalDensity.current

    val blurRadius = when {
        !state.autoscrollEnabled -> 0.dp
        state.currentLine < idx -> 1.5.dp * (idx - state.currentLine).coerceAtMost(4)
        else -> 3.dp * (state.currentLine - idx).coerceAtMost(4)
    }

    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        when {
            pressed -> .975f
            state.currentLine == idx -> 1.025f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    // FIXME: width crash
    val width = remember {
        density.run { constraints.maxWidth.toDp() *  3/4 }
    }

    widthIn(max = width)
        .padding(
            vertical = 8.dp,
            horizontal = HorizontalPadding/2
        )
        .clip(RoundedCornerShape(HorizontalPadding/2))
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
        .padding(vertical = 6.dp, horizontal = HorizontalPadding/2)
        .blur(
            radius = blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }

}



private fun IntAnimatable(
    initialValue : Int,
) = Animatable(
    initialValue = initialValue,
    typeConverter = TwoWayConverter(
        convertToVector = {
            AnimationVector(it.toFloat())
        },
        convertFromVector = {
            it.value.toInt()
        }
    )
)