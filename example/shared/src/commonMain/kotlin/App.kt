import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsLine
import io.github.alexzhirkevich.klyrics.LyricsWord
import io.github.alexzhirkevich.klyrics.rememberLyricsState
import klyrics.example.shared.generated.resources.Res
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi


@Serializable
class JsonLyrics(
    val duration : Int,
    val lines: List<JsonLyricsLine>
)

@Serializable
class JsonLyricsLine(
    val start : Int,
    val end : Int,
    val singer : Int,
    val words : List<JsonLyricsWord>
)

@Serializable
class JsonLyricsWord(
    val start : Int,
    val end : Int,
    val content : String
)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {

    val resourceLyrics: Lyrics? by produceState<Lyrics?>(null) {
        val json = Res.readBytes("files/work.json").decodeToString()

        val l = Json.decodeFromString<JsonLyrics>(json)

        value = Lyrics.WordSynced(
            duration = l.duration,
            lines = l.lines.map {
                LyricsLine.WordSynced(
                    start = it.start,
                    end = it.words.last().end,
                    alignment = if (it.singer == 1) Alignment.Start else Alignment.End,
                    words = it.words.map {
                        LyricsWord(
                            start = it.start,
                            end = it.end,
                            content = it.content
                        )
                    }
                )
            }
        )
    }

    val scope = rememberCoroutineScope()

    resourceLyrics?.let { lyrics ->

        val transition = remember {
            IntAnimatable(initialValue = 0)
        }

        LaunchedEffect(0) {
            transition.animateTo(
                targetValue = lyrics.duration,
                animationSpec = tween(lyrics.duration, easing = LinearEasing)
            )
        }

        val state = rememberLyricsState()

        val density = LocalDensity.current

        val textStyle =  TextStyle(
            fontSize = 32.sp,
            lineHeight = 38.sp
        ).copy(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )

        BoxWithConstraints {
            Lyrics(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                state = state,
                autoscrollOffset = 150.dp,
                lineModifier = { idx ->
                    val line = lyrics.lines[idx]

                    val blurRadius = when {
                        !state.autoscrollEnabled -> 0.dp
                        state.currentLine < idx -> 1.5.dp * (idx - state.currentLine)
                        else -> 3.dp * (state.currentLine - idx)
                    }

                    Modifier
                        .widthIn(max = density.run { constraints.maxWidth.toDp() * 2 / 3 })
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            scope.launch {
                                transition.snapTo(line.start)
                                transition.animateTo(
                                    targetValue = lyrics.duration,
                                    animationSpec = tween(
                                        lyrics.duration - line.start,
                                        easing = LinearEasing
                                    )
                                )
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 12.dp)
                        .then(
                            Modifier.blur(
                                radius = blurRadius,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            )
                        )
                },
                textStyle = {
                    if (lyrics.lines[it].alignment == Alignment.End)
                        textStyle.copy(
                            textAlign = TextAlign.End
                        )
                    else textStyle
                },
                lyrics = lyrics,
                time = { transition.value },
                focusedColor = Color.Red,
                unfocusedColor = Color.LightGray,
            )
        }
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