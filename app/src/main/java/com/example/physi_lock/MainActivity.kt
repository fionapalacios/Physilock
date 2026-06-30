package com.example.physi_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.physi_lock.ui.LockScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Using rememberSaveable ensures the state isn't reset by system reloads
                    var isLocked by remember { mutableStateOf(true) }

                    if (isLocked) {
                        LockScreen(onUnlocked = {
                            isLocked = false
                        })
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨ Freedom! Device Unlocked. ✨",
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}