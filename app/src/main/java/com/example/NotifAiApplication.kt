package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.AiRepository
import com.example.data.security.CryptoEngine
import com.example.service.NotificationChatService

class NotifAiApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var aiRepository: AiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize AndroidKeyStore encryption
        CryptoEngine.init()

        // Initialize Room Database
        database = AppDatabase.getInstance(this)

        // Initialize AiRepository
        aiRepository = AiRepository(
            apiKeyDao = database.apiKeyDao(),
            chatDao = database.chatDao(),
            context = this
        )

        // Check if persistent notification should be running
        val prefs = getSharedPreferences("notifai_prefs", Context.MODE_PRIVATE)
        val isServiceEnabled = prefs.getBoolean("service_enabled", true)
        if (isServiceEnabled) {
            NotificationChatService.startService(this)
        }
    }

    companion object {
        lateinit var instance: NotifAiApplication
            private set
    }
}
