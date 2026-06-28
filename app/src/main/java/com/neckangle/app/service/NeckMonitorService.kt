package com.neckangle.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neckangle.app.MainActivity
import com.neckangle.app.NeckAngleApp
import com.neckangle.app.engine.angle.MonitorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NeckMonitorService : Service() {

    private var scope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NeckAngleApp.CHANNEL_ID)
            .setContentTitle("颈椎监测中")
            .setContentText("正在监测颈椎姿势...")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope?.launch {
            MonitorEngine.instance.monitorState.collectLatest { state ->
                updateNotification(state)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    private fun updateNotification(state: com.neckangle.app.engine.angle.MonitorState) {
        val angleText = state.angle?.let { "${it.toInt()}°" } ?: "--°"
        val contentText = "$angleText | ${state.postureModeDisplay}"

        val notification = NotificationCompat.Builder(this, NeckAngleApp.CHANNEL_ID)
            .setContentTitle("颈椎监测中")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, NeckMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NeckMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
