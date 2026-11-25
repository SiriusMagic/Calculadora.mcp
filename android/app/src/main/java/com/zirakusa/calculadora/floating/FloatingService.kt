package com.zirakusa.calculadora.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zirakusa.calculadora.MainActivity
import com.zirakusa.calculadora.R
import com.zirakusa.calculadora.data.PrefsHistory
import com.zirakusa.calculadora.ui.CalcTheme
import com.zirakusa.calculadora.ui.CalculatorState

/**
 * «Ventana flotante»: a small calculator drawn over other apps (needs «Mostrar sobre otras apps»). It runs as a
 * foreground service with a notification, so Android does not close it while you use another app.
 */
class FloatingService : LifecycleService(), SavedStateRegistryOwner {
    private val saved = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
    private var view: ComposeView? = null

    override fun onCreate() {
        saved.performAttach(); saved.performRestore(null)
        super.onCreate()
        ServiceCompat.startForeground(this, 1, notification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_CLOSE) stopSelf()
        return START_NOT_STICKY
    }

    private fun show() {
        val windows = getSystemService(WindowManager::class.java)
        val density = resources.displayMetrics.density
        val params = WindowManager.LayoutParams((300 * density).toInt(), (470 * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = (40 * density).toInt(); y = (120 * density).toInt()
        }
        val state = CalculatorState(PrefsHistory(this)).also { floatingState = it }
        view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingService)
            setViewTreeSavedStateRegistryOwner(this@FloatingService)
            setContent {
                CalcTheme {
                    FloatingCalculator(state, onDrag = { dx, dy ->
                        params.x += dx.toInt(); params.y += dy.toInt(); windows.updateViewLayout(this, params)
                    }, onOpen = {
                        startActivity(Intent(this@FloatingService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        stopSelf()
                    }, onClose = { stopSelf() })
                }
            }
        }
        windows.addView(view, params)
    }

    /** The window's calculator: closing it keeps a result that was not saved with «=». */
    private var floatingState: CalculatorState? = null

    override fun onDestroy() {
        floatingState?.keepUnsaved()
        view?.let { getSystemService(WindowManager::class.java).removeView(it) }
        view = null
        super.onDestroy()
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.floating), NotificationManager.IMPORTANCE_LOW))
        val close = PendingIntent.getService(this, 0, Intent(this, FloatingService::class.java).setAction(ACTION_CLOSE),
            PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_monochrome)
            .setContentTitle(getString(R.string.floating_on)).setContentText(getString(R.string.floating_tap_close))
            .setContentIntent(close).setOngoing(true).build()
    }

    companion object {
        private const val CHANNEL = "floating"
        private const val ACTION_CLOSE = "close"

        /** Opens the window, or Android's «Mostrar sobre otras apps» page first. Returns true when the window opens. */
        fun open(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return false
            }
            context.startForegroundService(Intent(context, FloatingService::class.java))
            return true
        }
    }
}
