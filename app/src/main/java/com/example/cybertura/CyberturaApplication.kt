package com.example.cybertura

import android.app.Application
import com.example.cybertura.audio.SoundManager
import com.example.cybertura.data.local.DataStoreManager
import com.example.cybertura.data.local.JsonDataLoader
import com.example.cybertura.data.repository.CyberRepository

class CyberturaApplication : Application() {

    lateinit var repository: CyberRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val jsonDataLoader = JsonDataLoader(this)
        val dataStoreManager = DataStoreManager(this)
        repository = CyberRepository(jsonDataLoader, dataStoreManager)
        SoundManager.init(this)
    }
}
