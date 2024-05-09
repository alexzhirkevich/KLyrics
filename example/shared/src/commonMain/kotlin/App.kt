
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.klyrics.Lyrics
import klyrics.example.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi


@OptIn(ExperimentalResourceApi::class)


@Composable
fun App() {

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
