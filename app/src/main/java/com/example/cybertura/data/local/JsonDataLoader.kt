package com.example.cybertura.data.local

import android.content.Context
import android.util.Log
import com.example.cybertura.data.model.GameData
import com.google.gson.Gson
import java.io.InputStreamReader

class JsonDataLoader(private val context: Context) {

    private val gson = Gson()

    fun loadEducationalContent(fileName: String = "cyber_content.json"): GameData {
        return try {
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, GameData::class.java) ?: GameData()
                }
            }
        } catch (e: Exception) {
            Log.e("JsonDataLoader", "Error reading $fileName from assets", e)
            GameData()
        }
    }
}
