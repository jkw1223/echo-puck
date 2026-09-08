package com.steve.puckd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PuckService : Service() {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private var socket: WebSocket? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Starting"))
        connect()
    }

    private fun connect() {
        val request = Request.Builder()
            .url(RELAY_URL)
            .build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                update("Connected")
                sendHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.i(TAG, "relay: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                update("Closing")
                webSocket.close(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.w(TAG, "connection failed", t)
                update("Disconnected")
                android.os.Handler(mainLooper).postDelayed({ connect() }, 5_000)
            }
        })
    }

    private fun sendHeartbeat(webSocket: WebSocket) {
        val payload = JSONObject()
            .put("type", "heartbeat")
            .put("protocol", 1)
            .put("device", Build.DEVICE)
            .put("model", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("serial", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
        webSocket.send(payload.toString())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Puck daemon", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(status: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Steve Puck")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()

    private fun update(status: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(status))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onDestroy() {
        socket?.close(1000, "service stopped")
        client.dispatcher.executorService.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PuckService"
        private const val CHANNEL_ID = "puckd"
        private const val NOTIFICATION_ID = 1001
        private val RELAY_URL = BuildConfig.RELAY_URL
    }
}
