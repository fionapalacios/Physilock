package com.example.physi_lock.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// ACTIVITY_RECOGNITION is only a real runtime permission on API 29+ (Q); below
// that, step-detector access needs no explicit grant.
fun isActivityRecognitionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
