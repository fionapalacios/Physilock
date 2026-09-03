package com.example.physi_lock

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * No `google-services.json` locally (gitignored, per-dev file from the Firebase console),
 * so the `google-services` plugin is disabled and Firebase never auto-initializes. Without
 * this, `FirebaseAuth.getInstance()` in [com.example.physi_lock.data.auth.FirebaseAccountRepository]
 * throws immediately, crashing the app before MainActivity can even show a screen. This
 * placeholder init only unblocks that crash — auth/Firestore calls still fail since there's
 * no real backend behind it. Remove once a real google-services.json is added and the plugin
 * is re-enabled.
 */
class PhysiLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000000000")
                    .setApiKey("no-google-services-json-placeholder")
                    .setProjectId("physi-lock-placeholder")
                    .build()
            )
        }
    }
}
