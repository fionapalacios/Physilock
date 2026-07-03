package com.prototype.physi_lock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.prototype.physi_lock.ui.navigation.PhysiLockNavGraph
import com.prototype.physi_lock.ui.theme.PhysiLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhysiLockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhysiLockNavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}