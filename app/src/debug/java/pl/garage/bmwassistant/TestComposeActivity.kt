package pl.garage.bmwassistant

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class TestComposeActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        setContent {
            TestComposeContentRegistry.content()
        }
    }
}

object TestComposeContentRegistry {
    var content: @Composable () -> Unit = {
        Text("Test host ready")
    }
}
