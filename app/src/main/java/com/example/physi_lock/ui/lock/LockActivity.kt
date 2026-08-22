package com.example.physi_lock.ui.lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.physi_lock.service.AppMonitorService
import com.example.physi_lock.ui.theme.PhysiLockTheme

class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(AppMonitorService.EXTRA_PACKAGE_NAME) ?: ""
        setContent {
            PhysiLockTheme {
                LockScreen(packageName = packageName, onUnlocked = { finish() })
            }
        }
    }
}