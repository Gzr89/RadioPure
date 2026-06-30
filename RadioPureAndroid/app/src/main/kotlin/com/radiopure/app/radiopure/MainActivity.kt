package com.radiopure.app.radiopure

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.radiopure.app.radiopure.service.PlaybackService
import com.radiopure.app.radiopure.ui.ContentScreen
import com.radiopure.app.radiopure.ui.MainViewModel
import com.radiopure.app.radiopure.ui.theme.RadioPureTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var attachAttempts = 0

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 拒绝时仍可前台播放 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            RadioPureTheme {
                ContentScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, PlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        attachAttempts = 0
        scheduleAttachPlayer()
    }

    override fun onStop() {
        mainHandler.removeCallbacksAndMessages(null)
        viewModel.detachRadioPlayer()
        super.onStop()
    }

    private fun scheduleAttachPlayer() {
        val player = PlaybackService.instance?.radioPlayer
        if (player != null) {
            viewModel.attachRadioPlayer(player)
            return
        }
        if (attachAttempts++ < 40) {
            mainHandler.postDelayed({ scheduleAttachPlayer() }, 50)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
