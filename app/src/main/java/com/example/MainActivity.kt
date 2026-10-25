package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.MainAppContainer
import com.example.ui.theme.BizLedgerTheme
import com.example.ui.viewmodel.BizViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BizViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = if (settings.isDarkMode) true else systemDark

            BizLedgerTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }
}
