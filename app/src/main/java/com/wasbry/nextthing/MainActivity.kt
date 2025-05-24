package com.wasbry.nextthing

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.wasbry.nextthing.navigation.NavigationGraph
import com.wasbry.nextthing.ui.theme.NextThingTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextThingTheme {
                NavigationGraph(context = this)
            }
        }
    }
}