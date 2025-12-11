package team.swyp.sdu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import team.swyp.sdu.navigation.NavGraph
import team.swyp.sdu.presentation.viewmodel.UserViewModel
import team.swyp.sdu.ui.theme.TtTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TtTheme {
                val userViewModel: UserViewModel = hiltViewModel()
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    userViewModel = userViewModel,
                )
            }
        }
    }
}