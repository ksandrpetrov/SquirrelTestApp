package com.example.heis2025

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.Calendar

class GreetingBackgroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var notificationCount = 0

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "greeting_background_channel"
        private const val CHANNEL_NAME = "Приветственные уведомления (Background)"
        private const val CHANNEL_DESCRIPTION = "Показывает приветствия через Background Service"

        fun startService(context: Context) {
            val intent = Intent(context, GreetingBackgroundService::class.java)
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GreetingBackgroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startGreetingLoop()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startGreetingLoop() {
        val runnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    notificationCount++
                    showGreetingNotification()

                    // Планируем следующее уведомление через 15 секунд
                    handler.postDelayed(this, 15000)
                }
            }
        }

        // Запускаем первое уведомление через 5 секунд
        handler.postDelayed(runnable, 5000)
    }

    private fun showGreetingNotification() {
        try {
            createNotificationChannel()

            val greeting = getGreetingMessage()
            val icon = getGreetingIcon()

            // Intent для открытия приложения при нажатии на уведомление
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(greeting.title)
                .setContentText(greeting.message)
                .setSmallIcon(icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + notificationCount, notification)
        } catch (e: Exception) {
            // Минимальное логирование ошибок
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getGreetingMessage(): GreetingMessage {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..11 -> GreetingMessage(
                title = "🌅 Доброе утро!",
                message = "Белка просыпается и готова собирать орехи!"
            )
            in 12..17 -> GreetingMessage(
                title = "☀️ Добрый день!",
                message = "Белка активно собирает орехи и слушает музыку!"
            )
            in 18..22 -> GreetingMessage(
                title = "🌆 Добрый вечер!",
                message = "Белка готовится к отдыху, но еще может поиграть!"
            )
            else -> GreetingMessage(
                title = "🌙 Доброй ночи!",
                message = "Белка спит, но Background Service продолжает работать!"
            )
        }
    }

    private fun getGreetingIcon(): Int {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..11 -> android.R.drawable.ic_menu_day
            in 12..17 -> android.R.drawable.ic_menu_myplaces
            in 18..22 -> android.R.drawable.ic_menu_agenda
            else -> android.R.drawable.ic_menu_recent_history
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }


    data class GreetingMessage(
        val title: String,
        val message: String
    )

}
