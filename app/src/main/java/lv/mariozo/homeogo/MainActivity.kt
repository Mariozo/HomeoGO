// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Module: HomeoGO
// Purpose: Compose entry; shows a visible SmokeTestScreen to avoid blank screen
// Created: 17.sep.2025   17:05
// ver. 1.1

// # 1.  ------ Package & Imports ---------------------------------------------
package lv.mariozo.homeogo

// # 1.1  ------ Import fix: add Composable -----------------------------------
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

// # 2.  ------ Activity --------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeoGOTheme {
                // LV: “Dūmu tests” – skaidri redzams ekrāns (lai izslēgtu tukšu kompozīciju)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmokeTestScreen()
                }
            }
        }
    }
}

// # 3.  ------ Smoke Test Screen ----------------------------------------------
// EN text; LV comments allowed per protocol.
@Composable
private fun SmokeTestScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "HomeoGO is running 🚀",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
