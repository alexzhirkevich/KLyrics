
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
import klyrics.example.shared.generated.resources.mmlp2
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


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
                value = loadLyrics("files/monster/lyrics.json")
            }

            val lyrics = resourceLyrics

            if (lyrics == null) {
                CircularProgressIndicator(
                    color = LocalContentColor.current.copy(alpha = .5f)
                )
            } else {
                val cover = painterResource(Res.drawable.mmlp2)
                SongScreen(
                    song = remember(lyrics, cover) {
                        Song(
                            lyrics = lyrics,
                            url = Res.getUri("files/monster/audio.mp3"),
                            cover = cover,
                            name = "Monster (feat. Rihanna)",
                            artist = "Eminem"
                        )
                    }
                )
            }
        }
    }
}
