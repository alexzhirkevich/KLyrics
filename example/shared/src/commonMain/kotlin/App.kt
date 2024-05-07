import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.klyrics.Lyrics
import io.github.alexzhirkevich.klyrics.LyricsLine
import io.github.alexzhirkevich.klyrics.LyricsWord
import kotlinx.coroutines.launch

val wordLength = 3000/12

val content = "Work work work work work work work work work work work work"

val lineDuration = 3000
val lineCount = 30

private val lyrics = Lyrics.NotSynced(
    duration = lineDuration * lineCount,
    lines = List(lineCount) {
        val start = it * lineDuration
        val end = (it+1) * lineDuration

        LyricsLine(
            start = start,
            end = end,
            content = content,
            words = (0..11).map {
                LyricsWord(
                    start = start + it * wordLength,
                    end = start + (it + 1) * wordLength
                )
            }
        )
    }
)

@Composable
fun App() {

    val transition = remember {
        IntAnimatable(initialValue = 0)
    }

    LaunchedEffect(0){
        transition.animateTo(
            targetValue = lyrics.duration,
            animationSpec = tween(lyrics.duration, easing = LinearEasing)
        )
    }

    val scope = rememberCoroutineScope()

    Lyrics(
        modifier = Modifier
            .fillMaxHeight()
            .width(350.dp),
        lineModifier = {
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    scope.launch {
                        transition.snapTo(it.start)
                        transition.animateTo(
                            targetValue = lyrics.duration,
                            animationSpec = tween(lyrics.duration - it.start, easing = LinearEasing)
                        )
                    }
                }.padding(vertical = 8.dp, horizontal = 16.dp)
        },
        lyrics = lyrics,
        time = { transition.value },
        focusedColor = Color.Black,
        unfocusedColor = Color.LightGray,
        style = TextStyle(
            fontSize = 32.sp,
            lineHeight = 38.sp
        )
    )
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