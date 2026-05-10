package pl.garage.bmwassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7EC8E3),
            secondary = Color(0xFFD2B48C),
            background = Color(0xFF07111C),
            surface = Color(0xFF101B26),
            onPrimary = Color(0xFF061018),
            onSecondary = Color(0xFF21160C),
            onBackground = Color(0xFFE8EEF2),
            onSurface = Color(0xFFE8EEF2)
        ),
        content = content
    )
}
