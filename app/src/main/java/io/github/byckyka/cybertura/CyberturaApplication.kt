package io.github.byckyka.cybertura

import android.app.Application
import io.github.byckyka.cybertura.audio.Narrator
import io.github.byckyka.cybertura.audio.SoundManager
import io.github.byckyka.cybertura.data.local.DataStoreManager
import io.github.byckyka.cybertura.data.local.JsonDataLoader
import io.github.byckyka.cybertura.data.repository.CyberRepository
import io.github.byckyka.cybertura.research.ResearchLogger

class CyberturaApplication : Application() {

    lateinit var repository: CyberRepository
        private set

    /** Registro anónimo del piloto (solo graba con una sesión abierta en el modo investigador). */
    lateinit var researchLogger: ResearchLogger
        private set

    /** Voz que lee los textos a quien lo pida (puede no estar disponible en el teléfono). */
    lateinit var narrator: Narrator
        private set

    override fun onCreate() {
        super.onCreate()
        val jsonDataLoader = JsonDataLoader(this)
        val dataStoreManager = DataStoreManager(this)
        repository = CyberRepository(jsonDataLoader, dataStoreManager)
        researchLogger = ResearchLogger(this)
        SoundManager.init(this)
        narrator = Narrator(this)
    }
}
