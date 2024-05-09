
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.klyrics.Lyrics
import klyrics.example.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi


@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)


@Composable
fun App() {

//    val style = androidx.compose.ui.text.TextStyle(
//        fontSize = 32.sp
//    )
//
//    val measurer = rememberTextMeasurer()
//
//    val brush =  SolidColor(Color.White.copy(.5f))
//    Spacer(Modifier.fillMaxSize().background(Color.Black).drawWithCache {
//        onDrawBehind {
//            val t1 = measurer.measure("White with 0.5 alpha", style)
//
//            drawText(t1, brush = brush)
//        }
//    })
//    return

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {

            val resourceLyrics: Lyrics? by produceState<Lyrics?>(null) {
                value = loadLyrics("files/desperado/lyrics.json")
            }

            val lyrics = resourceLyrics

            if (lyrics == null) {
                CircularProgressIndicator(
                    color = LocalContentColor.current.copy(alpha = .5f)
                )
            } else {
                SongScreen(
                    song = remember(lyrics) {
                        Song(
                            lyrics = lyrics,
                            url = Res.getUri("files/desperado/audio.mp3"),
                            name = "Desperado",
                            artist = "Rihanna"
                        )
                    }
                )
            }
        }
    }
}
