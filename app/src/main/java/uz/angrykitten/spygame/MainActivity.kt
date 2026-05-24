package uz.angrykitten.spygame

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uz.angrykitten.spygame.navigation.AppNavGraph
import uz.angrykitten.spygame.ui.settings.SettingsViewModel
import uz.angrykitten.spygame.ui.theme.CipherTheme


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            // Lifecycle-aware so theme collection pauses while backgrounded.
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsStateWithLifecycle()

            CipherTheme(darkTheme = isDarkTheme) {
                AppNavGraph()
            }
        }
    }
}
