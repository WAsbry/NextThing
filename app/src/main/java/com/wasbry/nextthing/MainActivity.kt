package com.wasbry.nextthing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wasbry.nextthing.navigation.NavigationGraph
import com.wasbry.nextthing.ui.theme.NextThingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextThingTheme {
                NavigationGraph(context = this)
            }
        }
    }
}