package com.example.cybertura.game.renderer

import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.example.cybertura.R

/**
 * Imágenes del mundo y los personajes. Se decodifican una sola vez por ejecución: la pantalla
 * del mundo sale de la composición al abrir un reto, y con `remember` se recargaban al volver,
 * lo que detenía el hilo principal.
 */
class GameSprites private constructor(resources: Resources) {

    val world = WorldAssets(
        grassSheet    = image(resources, R.drawable.tile_earth),
        waterImg      = image(resources, R.drawable.tile_water),
        water5Img     = image(resources, R.drawable.water5_pipo),
        earthImg      = image(resources, R.drawable.tile_earth),
        baseImg       = image(resources, R.drawable.tile_base),
        cornerImg     = image(resources, R.drawable.tile_corner_ld),
        crossroadsImg = image(resources, R.drawable.tile_crossroads),
        lairImg       = image(resources, R.drawable.tile_lair),
        bonesImg      = image(resources, R.drawable.tile_bones),
        bushImg       = image(resources, R.drawable.tile_bush),
        cartelImg     = image(resources, R.drawable.tile_cartel),
        graveImg      = image(resources, R.drawable.tile_grave),
        lukeImg       = image(resources, R.drawable.tile_luke),
        mushroomImg   = image(resources, R.drawable.tile_mushroom),
        skullImg      = image(resources, R.drawable.tile_skull)
    )

    val player: ImageBitmap = image(resources, R.drawable.thief_idle_1)

    val npcs: Map<String, ImageBitmap> = mapOf(
        "npc_passwords"  to image(resources, R.drawable.alchemist_idle_1),
        "npc_phishing"   to image(resources, R.drawable.blacksmith_idle_1),
        "npc_privacy"    to image(resources, R.drawable.princess_idle_1),
        "npc_bullying"   to image(resources, R.drawable.mage_idle_1),
        "npc_social"     to image(resources, R.drawable.king_idle_1),
        "npc_villager_1" to image(resources, R.drawable.townsfolk_m_idle_1),
        "npc_villager_2" to image(resources, R.drawable.butcher_idle_1),
        "npc_villager_3" to image(resources, R.drawable.herald_idle_1),
        // Maestros y habitantes dentro de las casas
        "npc_wifi"       to image(resources, R.drawable.herald_idle_1),
        "npc_gaming"     to image(resources, R.drawable.executioner_idle_1),
        "npc_forest"     to image(resources, R.drawable.alchemist_idle_1),
        "npc_abuela"     to image(resources, R.drawable.princess_idle_1),
        "npc_tio_bits"   to image(resources, R.drawable.butcher_idle_1),
        "npc_mama_cora"  to image(resources, R.drawable.townsfolk_m_idle_1),
        "npc_hermana_luna" to image(resources, R.drawable.mage_idle_1)
    )

    private fun image(resources: Resources, id: Int): ImageBitmap = ImageBitmap.imageResource(resources, id)

    companion object {
        @Volatile
        private var loaded: GameSprites? = null

        fun get(resources: Resources): GameSprites =
            loaded ?: synchronized(this) {
                loaded ?: GameSprites(resources).also { loaded = it }
            }
    }
}
