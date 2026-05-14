package com.m2h.m2haichatbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.m2h.m2haichatbot.presentation.navigation.NavGraph
import com.m2h.m2haichatbot.presentation.navigation.Screen
import com.m2h.m2haichatbot.presentation.theme.M2HAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        setContent {
            val themeViewModel: com.m2h.m2haichatbot.presentation.theme.ThemeViewModel = hiltViewModel()
            val useDarkTheme by themeViewModel.darkTheme.collectAsState()
            
            val authViewModel: com.m2h.m2haichatbot.presentation.auth.AuthViewModel = hiltViewModel()
            val authState by authViewModel.state.collectAsState()
            
            M2HAITheme(darkTheme = useDarkTheme ?: isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show a simple loading indicator while auth status is being determined for the first time
                    // (Assuming authState.isLoading is true until session is loaded)
                    // If your AuthViewModel doesn't set isLoading=true initially, you might need to check if it's "unknown"
                    
                    val navController = rememberNavController()
                    
                    // We only want to decide startDestination once we have a definite answer about the user
                    // For now, let's use the current state. 
                    // If authState.user is null and isLoading is true, we could wait.
                    
                    if (authState.isLoading && authState.user == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val startDestination = if (authState.user != null) {
                            Screen.Home.route
                        } else {
                            Screen.Login.route
                        }
                        
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}
