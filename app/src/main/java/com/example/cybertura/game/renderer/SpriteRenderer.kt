package com.example.cybertura.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.cybertura.game.engine.Direction
import com.example.cybertura.game.engine.GameState
import com.example.cybertura.game.engine.GameWorld
import com.example.cybertura.game.engine.NpcData
import com.example.cybertura.game.engine.NpcRegistry
import com.example.cybertura.ui.theme.*
import androidx.compose.ui.graphics.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Función de extensión para DrawScope que dibuja todos los sprites (jugador y NPCs) 
 * utilizando ImageBitmaps en lugar de formas geométricas.
 */
fun DrawScope.drawSprites(
    state: GameState,
    npcs: List<NpcData>,
    playerSprite: ImageBitmap,
    npcSprites: Map<String, ImageBitmap>
) {
    val tileSizePx = GameWorld.TILE_SIZE

    // Esquina superior izquierda del jugador en pantalla. La cámara del mapa se centra en
    // (playerX + 0.5, playerY + 0.5), así que hay que restar medio tile: sin esto el sprite
    // (y los NPCs y la guía de la puerta) se dibujaban medio tile abajo-derecha de su
    // posición real, y el choque no coincidía con lo que se veía.
    val playerScreenX = size.width / 2f - tileSizePx / 2f
    val playerScreenY = size.height / 2f - tileSizePx / 2f

    // 0. Guía hacia la misión: si el maestro está en OTRO mapa (dentro de una casa), señalamos la puerta
    val targetNpc = state.nextQuestNpcId?.let { NpcRegistry.getNpcById(it) }
    val guidePortal = targetNpc?.let { GameWorld.guidePortal(state.currentMapId, it.mapId) }
    if (guidePortal != null) {
        val doorX = playerScreenX + (guidePortal.tileX - state.playerX) * tileSizePx
        val doorY = playerScreenY + (guidePortal.tileY - state.playerY) * tileSizePx
        drawDoorGuide(doorX, doorY, tileSizePx, state.frameCount)
    }

    // 1. Dibujar NPCs del mapa actual
    for (npc in npcs) {
        if (npc.mapId != state.currentMapId) continue
        val npcState = state.npcStates[npc.id] ?: continue

        val diffX = npcState.x - state.playerX
        val diffY = npcState.y - state.playerY

        // Si está razonablemente cerca, lo dibujamos
        if (kotlin.math.abs(diffX) < 15 && kotlin.math.abs(diffY) < 15) {
            val npcScreenX = playerScreenX + (diffX * tileSizePx)
            val npcScreenY = playerScreenY + (diffY * tileSizePx)

            val spriteImage = npcSprites[npc.id] ?: playerSprite
            val isNearby = state.nearbyNpcId == npc.id
            val isQuestTarget = state.nextQuestNpcId == npc.id

            drawNpc(npcScreenX, npcScreenY, tileSizePx, npc, isNearby, isQuestTarget, state.frameCount, spriteImage)
        }
    }

    // 2. Dibujar Jugador
    drawPlayer(playerScreenX, playerScreenY, tileSizePx, state, playerSprite)

    // 3. Brújula: flecha dorada alrededor del jugador apuntando al destino si está lejos
    if (targetNpc != null && !state.showDialog) {
        val destX: Float
        val destY: Float
        if (guidePortal != null) {
            destX = guidePortal.tileX.toFloat()
            destY = guidePortal.tileY.toFloat()
        } else {
            val live = state.npcStates[targetNpc.id]
            destX = live?.x ?: targetNpc.tileX
            destY = live?.y ?: targetNpc.tileY
        }
        drawCompassArrow(
            cx = playerScreenX + tileSizePx / 2f,
            cy = playerScreenY + tileSizePx / 2f,
            dx = destX - state.playerX,
            dy = destY - state.playerY,
            frameCount = state.frameCount
        )
    }
}

/** Signo de exclamación dorado pulsante (misión activa). [baseY] es la altura base sobre el objetivo. */
private fun DrawScope.drawQuestBang(cx: Float, baseY: Float, frameCount: Long) {
    val pulse = kotlin.math.abs(sin(frameCount / 20f)) // 0..1 pulsante
    val markerAlpha = 0.7f + pulse * 0.3f
    val bounce = pulse * 8f
    val markerY = baseY - 42f - bounce

    // Halo brillante
    drawCircle(
        color = NeonGold.copy(alpha = 0.25f * markerAlpha),
        radius = 16f,
        center = Offset(cx, markerY - 2f)
    )
    // Cuerpo del !
    drawRect(
        color = NeonGold.copy(alpha = markerAlpha),
        topLeft = Offset(cx - 3.5f, markerY - 14f),
        size = Size(7f, 16f)
    )
    // Punto del !
    drawCircle(
        color = NeonGold.copy(alpha = markerAlpha),
        radius = 4.5f,
        center = Offset(cx, markerY + 6f)
    )
}

/** Resalta una puerta: anillo pulsante, ❗ y flecha apuntando hacia ella. */
private fun DrawScope.drawDoorGuide(x: Float, y: Float, size: Float, frameCount: Long) {
    val pulse = kotlin.math.abs(sin(frameCount / 20f))
    val cx = x + size / 2f
    val cy = y + size / 2f

    // Resplandor sobre el tile de la puerta
    drawRect(
        color = NeonGold.copy(alpha = 0.18f + pulse * 0.2f),
        topLeft = Offset(x, y),
        size = Size(size, size)
    )
    drawCircle(
        color = NeonGold.copy(alpha = 0.55f),
        radius = size * (0.55f + pulse * 0.2f),
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
    drawQuestBang(cx, y + 4f, frameCount)

    // Flecha hacia abajo sobre la puerta
    val arrowTop = y - 14f - pulse * 6f
    val arrow = Path().apply {
        moveTo(cx, arrowTop + 14f)
        lineTo(cx - 9f, arrowTop)
        lineTo(cx + 9f, arrowTop)
        close()
    }
    drawPath(arrow, NeonGold)
}

/** Flecha dorada que orbita al jugador y apunta al destino cuando está a más de 4 tiles. */
private fun DrawScope.drawCompassArrow(cx: Float, cy: Float, dx: Float, dy: Float, frameCount: Long) {
    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
    if (dist < 4f) return

    val angle = atan2(dy, dx)
    val pulse = kotlin.math.abs(sin(frameCount / 15f))
    val radius = 82f + pulse * 6f
    val ux = cos(angle)
    val uy = sin(angle)
    // Vector perpendicular
    val px = -uy
    val py = ux

    val tipX = cx + ux * (radius + 14f)
    val tipY = cy + uy * (radius + 14f)
    val baseX = cx + ux * radius
    val baseY = cy + uy * radius

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(baseX + px * 9f, baseY + py * 9f)
        lineTo(baseX - px * 9f, baseY - py * 9f)
        close()
    }
    drawPath(path, NeonGold.copy(alpha = 0.55f + pulse * 0.4f))
    drawPath(
        path,
        Color.Black.copy(alpha = 0.5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
    )
}

/**
 * Dibuja un NPC en coordenadas de pantalla usando su imagen PNG.
 * @param isNearby     El jugador está lo suficientemente cerca para interactuar.
 * @param isQuestTarget Este NPC es el objetivo actual de la misión principal.
 */
private fun DrawScope.drawNpc(
    x: Float, y: Float, size: Float,
    npc: NpcData,
    isNearby: Boolean,
    isQuestTarget: Boolean,
    frameCount: Long,
    sprite: ImageBitmap
) {
    // Sombra ovalada por debajo del sprite
    drawOval(
        color = Color.Black.copy(alpha = 0.4f),
        topLeft = Offset(x + size * 0.2f, y + size * 0.75f),
        size = Size(size * 0.6f, size * 0.25f)
    )

    // Animación de "flote" suave
    val floatOffset = sin(frameCount / 30f + npc.tileX) * 4f
    val drawY = y + floatOffset

    // DIBUJAR SPRITE PNG
    drawImage(
        image = sprite,
        dstOffset = IntOffset(x.toInt(), drawY.toInt()),
        dstSize = IntSize(size.toInt(), size.toInt()),
        filterQuality = FilterQuality.None
    )

    // ── Marcador de misión activa (❗ dorado grande) ────────────────────────
    if (isQuestTarget && !isNearby) {
        val pulse = kotlin.math.abs(sin(frameCount / 20f)) // 0..1 pulsante
        val markerAlpha = 0.7f + pulse * 0.3f
        val bounce = pulse * 8f
        val cx = x + size / 2f
        val markerY = drawY - 42f - bounce

        // Halo brillante
        drawCircle(
            color = NeonGold.copy(alpha = 0.25f * markerAlpha),
            radius = 16f,
            center = Offset(cx, markerY - 2f)
        )
        // Cuerpo del !
        drawRect(
            color = NeonGold.copy(alpha = markerAlpha),
            topLeft = Offset(cx - 3.5f, markerY - 14f),
            size = Size(7f, 16f)
        )
        // Punto del !
        drawCircle(
            color = NeonGold.copy(alpha = markerAlpha),
            radius = 4.5f,
            center = Offset(cx, markerY + 6f)
        )
    }

    // ── Indicador de interacción cercana (punto cian pequeño) ─────────────
    if (isNearby) {
        val bounce = kotlin.math.abs(sin(frameCount / 10f)) * 5f
        drawCircle(
            color = NeonCyan,
            radius = 5f,
            center = Offset(x + size / 2f, drawY - 18f - bounce)
        )
        drawRect(
            color = NeonCyan,
            topLeft = Offset(x + size / 2f - 2f, drawY - 32f - bounce),
            size = Size(4f, 11f)
        )
    }
}

/**
 * Dibuja al jugador en el centro de la pantalla usando su imagen PNG.
 */
private fun DrawScope.drawPlayer(x: Float, y: Float, size: Float, state: GameState, sprite: ImageBitmap) {
    // Sombra ovalada por debajo del sprite
    drawOval(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = Offset(x + size * 0.2f, y + size * 0.75f),
        size = Size(size * 0.6f, size * 0.25f)
    )

    // Si está caminando, alternamos offset Y para simular pasos (bobbing)
    val bobbingOffset = if (state.isWalking) {
        if (state.walkFrame == 0) -2f else 2f
    } else 0f

    val drawY = y + bobbingOffset

    // DIBUJAR SPRITE PNG
    drawImage(
        image = sprite,
        dstOffset = IntOffset(x.toInt(), drawY.toInt()),
        dstSize = IntSize(size.toInt(), size.toInt()),
        filterQuality = FilterQuality.None 
    )
}
