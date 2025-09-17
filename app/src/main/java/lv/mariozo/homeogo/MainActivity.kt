package lv.mariozo.homeogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel // Added for viewModel()
import lv.mariozo.homeogo.ui.ElzaScreen // Added for ElzaScreen
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme // Corrected HomeoGOTheme import if necessary
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel // Added for ElzaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeoGOTheme {
                val elzaViewModel: ElzaViewModel = viewModel() // Get ViewModel instance
                ElzaScreen(vm = elzaViewModel) // Call your main screen
            }
        }
    }
}
