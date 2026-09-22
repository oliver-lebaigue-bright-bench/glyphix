import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.glyphix.app.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Glyphix") {
        App()
    }
}
