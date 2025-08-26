package com.example.nextthingb1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.nextthingb1.presentation.navigation.NextThingNavigation
import com.example.nextthingb1.presentation.theme.NextThingB1Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextThingB1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NextThingApp()
                }
            }
        }
    }
}

@Composable
fun NextThingApp() {
    val navController = rememberNavController()
    
    NextThingNavigation(navController = navController)
}

@Preview(showBackground = true)
@Composable
fun NextThingAppPreview() {
    NextThingB1Theme {
        NextThingApp()
    }
} 