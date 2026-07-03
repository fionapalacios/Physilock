package com.example.physi_lock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.ui.theme.PhysiLockTheme

class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhysiLockTheme {
                LockScreen(onUnlocked = { finish() })
            }
        }
    }
}