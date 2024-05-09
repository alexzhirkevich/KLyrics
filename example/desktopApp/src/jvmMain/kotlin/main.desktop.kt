import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {

    application {
        val windowState = rememberWindowState(
            size = DpSize(400.dp, 800.dp),
            position = WindowPosition(300.dp, 50.dp)
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "KLyrics",
            state = windowState,
        ) {
           App()
        }
    }
}