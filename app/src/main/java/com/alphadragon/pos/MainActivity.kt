package com.alphadragon.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alphadragon.pos.ui.navigation.AlphaDragonNavHost
import com.alphadragon.pos.ui.theme.AlphaDragonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlphaDragonTheme {
                AlphaDragonNavHost()
            }
        }
    }
}
