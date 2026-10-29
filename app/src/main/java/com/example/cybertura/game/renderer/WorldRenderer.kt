package com.example.cybertura.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.cybertura.game.engine.GameWorld
import com.example.cybertura.game.engine.MapIds
import com.example.cybertura.game.engine.TileType
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path

// --- Paleta geométrica (para tiles sin sprite) ---
private val WaterShallow = Color(0xFF5AC8E8)
private val WaterDeep    = Color(0xFF2060C0)
private val CliffTop     = Color(0xFFC09058)
private val CliffFace    = Color(0xFFA07040)
private val CliffBase    = Color(0xFF704828)
private val WoodColor    = Color(0xFF885830)
private val GrassColor   = Color(0xFF68C85A)
private val GrassDark    = Color(0xFF4BAA3D)
private val PathStone    = Color(0xFFC0D0D8)
private val PathDirt     = Color(0xFFDCA868)

/**
 * Mapa de imágenes para el WorldRenderer.
 * Agrupa todos los sprites de ambiente en un solo objeto para pasar fácilmente entre funciones.
 */
data class WorldAssets(
    val grassSheet: ImageBitmap,
    val waterImg: ImageBitmap,
    val water5Img: ImageBitmap,     // Nuevo agua seleccionada (water5_pipo)
    val earthImg: ImageBitmap,
    val baseImg: ImageBitmap,
    val cornerImg: ImageBitmap,
    val crossroadsImg: ImageBitmap,
    val lairImg: ImageBitmap,
    val bonesImg: ImageBitmap,
    val bushImg: ImageBitmap,
    val cartelImg: ImageBitmap,
    val graveImg: ImageBitmap,
    val lukeImg: ImageBitmap,
    val mushroomImg: ImageBitmap,
    val skullImg: ImageBitmap
)

fun DrawScope.drawWorldMap(
    cameraX: Float,
    cameraY: Float,
    assets: WorldAssets,
    mapId: String = MapIds.EXTERIOR
) {
    val tileSizePx = GameWorld.TILE_SIZE
    val tilesFitX = size.width / tileSizePx
    val tilesFitY = size.height / tileSizePx
    val startCol = (cameraX - tilesFitX / 2).toInt() - 1
    val endCol   = (cameraX + tilesFitX / 2).toInt() + 2
    val startRow = (cameraY - tilesFitY / 2).toInt() - 1
    val endRow   = (cameraY + tilesFitY / 2).toInt() + 2

    val offsetX = (size.width / 2f)  - (cameraX * tileSizePx)
    val offsetY = (size.height / 2f) - (cameraY * tileSizePx)

    // --- Bosque Escondido: render propio ---
    if (mapId == MapIds.FOREST) {
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                val tileType = GameWorld.getTile(mapId, col, row)
                if (tileType == TileType.VOID) continue
                drawForestTile(tileType, col * tileSizePx + offsetX, row * tileSizePx + offsetY, tileSizePx, col, row)
            }
        }
        return
    }

    // --- Interior de una casa: render propio (calido/frio según la casa) ---
    if (mapId != MapIds.EXTERIOR) {
        val palette = if (mapId.contains("red")) WarmRoom else CoolRoom
        for (row in startRow..endRow) {
            for (col in startCol..endCol) {
                val tileType = GameWorld.getTile(mapId, col, row)
                if (tileType == TileType.VOID) continue
                drawInteriorTile(
                    tileType,
                    col * tileSizePx + offsetX,
                    row * tileSizePx + offsetY,
                    tileSizePx,
                    palette
                )
            }
        }
        return
    }

    // --- Pass 1: Suelo base ---
    for (row in startRow..endRow) {
        for (col in startCol..endCol) {
            val tileType = GameWorld.getTile(col, row)
            val x = col * tileSizePx + offsetX
            val y = row * tileSizePx + offsetY

            drawGroundLayer(tileType, x, y, tileSizePx, assets)
        }
    }

    // --- Pass 2: Objetos / decoraciones (encima del suelo) ---
    for (row in startRow..endRow) {
        for (col in startCol..endCol) {
            val tileType = GameWorld.getTile(col, row)
            val x = col * tileSizePx + offsetX
            val y = row * tileSizePx + offsetY

            drawObjectLayer(tileType, x, y, tileSizePx, assets)
        }
    }
}

// ---------------------------------------------------------------------------
// Interiores de casas
// ---------------------------------------------------------------------------
private class RoomPalette(
    val floorA: Color,
    val wall: Color,
    val wallDark: Color,
    val rug: Color,
    val rugTrim: Color,
    val sofa: Color,
    val curtain: Color
)

private val WarmRoom = RoomPalette(
    floorA = Color(0xFFC98B54), wall = Color(0xFFEBD3AE), wallDark = Color(0xFF8B5A3C),
    rug = Color(0xFFB03A48), rugTrim = Color(0xFFE9B949), sofa = Color(0xFFC0503A),
    curtain = Color(0xFFD9534F)
)
private val CoolRoom = RoomPalette(
    floorA = Color(0xFF9FB2C8), wall = Color(0xFFC3DAEE), wallDark = Color(0xFF3E5C8A),
    rug = Color(0xFF2E6FBF), rugTrim = Color(0xFFF2C94C), sofa = Color(0xFF3B6EC4),
    curtain = Color(0xFF5B8DEF)
)

private val BookColors = listOf(
    Color(0xFFD64545), Color(0xFF3F7CD8), Color(0xFF4CAF50), Color(0xFFF2C94C), Color(0xFF9B59B6)
)

private fun DrawScope.drawInteriorFloor(x: Float, y: Float, s: Float, p: RoomPalette) {
    drawRect(color = p.floorA, topLeft = Offset(x, y), size = Size(s, s))
    // Tablones de madera
    for (i in 0..3) {
        drawRect(
            color = Color.Black.copy(alpha = 0.13f),
            topLeft = Offset(x, y + i * s / 4f),
            size = Size(s, 1.5f)
        )
    }
    drawRect(
        color = Color.White.copy(alpha = 0.06f),
        topLeft = Offset(x, y), size = Size(s, s / 4f)
    )
}

private fun DrawScope.drawInteriorWall(x: Float, y: Float, s: Float, p: RoomPalette) {
    drawRect(color = p.wall, topLeft = Offset(x, y), size = Size(s, s))
    // Franja decorativa y zócalo
    drawRect(color = p.wallDark.copy(alpha = 0.18f), topLeft = Offset(x, y + s * 0.45f), size = Size(s, s * 0.08f))
    drawRect(color = p.wallDark, topLeft = Offset(x, y + s * 0.84f), size = Size(s, s * 0.16f))
}

private fun DrawScope.drawInteriorTile(type: TileType, x: Float, y: Float, s: Float, p: RoomPalette) {
    when (type) {
        TileType.WALL -> drawInteriorWall(x, y, s, p)

        TileType.WALL_WINDOW -> {
            drawInteriorWall(x, y, s, p)
            // Marco + cristal con cielo
            drawRect(Color(0xFF6B4226), Offset(x + s * 0.14f, y + s * 0.1f), Size(s * 0.72f, s * 0.62f))
            drawRect(Color(0xFF9AD8FF), Offset(x + s * 0.2f, y + s * 0.16f), Size(s * 0.6f, s * 0.5f))
            drawRect(Color.White.copy(alpha = 0.5f), Offset(x + s * 0.22f, y + s * 0.18f), Size(s * 0.18f, s * 0.14f))
            drawRect(Color(0xFF6B4226), Offset(x + s * 0.48f, y + s * 0.16f), Size(s * 0.04f, s * 0.5f))
            drawRect(Color(0xFF6B4226), Offset(x + s * 0.2f, y + s * 0.39f), Size(s * 0.6f, s * 0.04f))
            // Cortinas
            drawRect(p.curtain, Offset(x + s * 0.08f, y + s * 0.08f), Size(s * 0.14f, s * 0.68f))
            drawRect(p.curtain, Offset(x + s * 0.78f, y + s * 0.08f), Size(s * 0.14f, s * 0.68f))
        }

        TileType.WALL_PICTURE -> {
            drawInteriorWall(x, y, s, p)
            drawRect(Color(0xFFD9A441), Offset(x + s * 0.18f, y + s * 0.14f), Size(s * 0.64f, s * 0.5f))
            drawRect(Color(0xFF7EC8A3), Offset(x + s * 0.24f, y + s * 0.2f), Size(s * 0.52f, s * 0.38f))
            drawRect(Color(0xFF4F9D7A), Offset(x + s * 0.24f, y + s * 0.42f), Size(s * 0.52f, s * 0.16f))
            drawCircle(Color(0xFFFFE066), radius = s * 0.07f, center = Offset(x + s * 0.62f, y + s * 0.3f))
        }

        TileType.EXIT_DOOR -> {
            drawInteriorWall(x, y, s, p)
            // Puerta de salida con luz de la calle
            drawRect(Color(0xFF6B4226), Offset(x + s * 0.08f, y), Size(s * 0.84f, s))
            drawRect(Color(0xFFFFE9A8).copy(alpha = 0.85f), Offset(x + s * 0.16f, y + s * 0.06f), Size(s * 0.68f, s * 0.94f))
            drawRect(Color.White.copy(alpha = 0.35f), Offset(x + s * 0.2f, y + s * 0.1f), Size(s * 0.22f, s * 0.8f))
            // Flecha verde de salida
            val arrow = Path().apply {
                moveTo(x + s * 0.5f, y + s * 0.85f)
                lineTo(x + s * 0.28f, y + s * 0.5f)
                lineTo(x + s * 0.72f, y + s * 0.5f)
                close()
            }
            drawPath(arrow, Color(0xFF2ECC71))
            drawRect(Color(0xFF2ECC71), Offset(x + s * 0.43f, y + s * 0.25f), Size(s * 0.14f, s * 0.27f))
        }

        else -> {
            // Todo lo demás se apoya sobre el piso de madera
            drawInteriorFloor(x, y, s, p)
            when (type) {
                TileType.RUG -> {
                    drawRect(p.rug, Offset(x, y), Size(s, s))
                    drawRect(
                        p.rugTrim, Offset(x + s * 0.1f, y + s * 0.1f), Size(s * 0.8f, s * 0.8f),
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(p.rugTrim.copy(alpha = 0.7f), radius = s * 0.09f, center = Offset(x + s / 2f, y + s / 2f))
                }

                TileType.BOOKSHELF -> {
                    drawRect(Color(0xFF5A3A22), Offset(x + s * 0.05f, y + s * 0.04f), Size(s * 0.9f, s * 0.92f))
                    for (shelf in 0..2) {
                        val sy = y + s * (0.09f + shelf * 0.29f)
                        drawRect(Color(0xFF3B2514), Offset(x + s * 0.08f, sy + s * 0.22f), Size(s * 0.84f, s * 0.05f))
                        for (b in 0..4) {
                            drawRect(
                                BookColors[(b + shelf * 2) % BookColors.size],
                                Offset(x + s * (0.1f + b * 0.16f), sy + s * (if (b % 2 == 0) 0.02f else 0.06f)),
                                Size(s * 0.13f, s * (if (b % 2 == 0) 0.2f else 0.16f))
                            )
                        }
                    }
                }

                TileType.PLANT -> {
                    drawOval(Color.Black.copy(alpha = 0.2f), Offset(x + s * 0.22f, y + s * 0.8f), Size(s * 0.56f, s * 0.14f))
                    drawCircle(Color(0xFF2E8B57), s * 0.2f, Offset(x + s * 0.5f, y + s * 0.38f))
                    drawCircle(Color(0xFF3FAE6A), s * 0.15f, Offset(x + s * 0.34f, y + s * 0.48f))
                    drawCircle(Color(0xFF3FAE6A), s * 0.15f, Offset(x + s * 0.66f, y + s * 0.48f))
                    drawRect(Color(0xFFC0623B), Offset(x + s * 0.32f, y + s * 0.58f), Size(s * 0.36f, s * 0.28f))
                    drawRect(Color(0xFF9E4A28), Offset(x + s * 0.3f, y + s * 0.56f), Size(s * 0.4f, s * 0.06f))
                }

                TileType.LAMP -> {
                    drawCircle(Color(0xFFFFE066).copy(alpha = 0.18f), s * 0.9f, Offset(x + s * 0.5f, y + s * 0.3f))
                    drawOval(Color.Black.copy(alpha = 0.25f), Offset(x + s * 0.28f, y + s * 0.84f), Size(s * 0.44f, s * 0.12f))
                    drawRect(Color(0xFF444444), Offset(x + s * 0.47f, y + s * 0.34f), Size(s * 0.06f, s * 0.55f))
                    drawRect(Color(0xFFFFE066), Offset(x + s * 0.3f, y + s * 0.12f), Size(s * 0.4f, s * 0.26f))
                    drawRect(Color(0xFFFFF4B0), Offset(x + s * 0.36f, y + s * 0.16f), Size(s * 0.28f, s * 0.12f))
                }

                TileType.TV -> {
                    drawRect(Color(0xFF4A3322), Offset(x + s * 0.08f, y + s * 0.58f), Size(s * 0.84f, s * 0.38f))
                    drawRect(Color(0xFF1A1A1A), Offset(x + s * 0.06f, y + s * 0.1f), Size(s * 0.88f, s * 0.52f))
                    drawRect(Color(0xFF2A9FD6), Offset(x + s * 0.12f, y + s * 0.16f), Size(s * 0.76f, s * 0.4f))
                    drawRect(Color.White.copy(alpha = 0.35f), Offset(x + s * 0.16f, y + s * 0.2f), Size(s * 0.2f, s * 0.1f))
                    drawCircle(NeonTvDot, s * 0.025f, Offset(x + s * 0.84f, y + s * 0.6f))
                }

                TileType.KITCHEN -> {
                    drawRect(Color(0xFFCFC9BF), Offset(x, y + s * 0.2f), Size(s, s * 0.78f))
                    drawRect(Color(0xFFF2EFEA), Offset(x, y + s * 0.2f), Size(s, s * 0.3f))
                    drawCircle(Color(0xFF3A3A3A), s * 0.09f, Offset(x + s * 0.3f, y + s * 0.35f))
                    drawCircle(Color(0xFF3A3A3A), s * 0.09f, Offset(x + s * 0.7f, y + s * 0.35f))
                    drawRect(Color(0xFF9A948A), Offset(x + s * 0.1f, y + s * 0.62f), Size(s * 0.8f, s * 0.03f))
                    drawRect(Color(0xFFB5AFA5), Offset(x + s * 0.1f, y + s * 0.78f), Size(s * 0.8f, s * 0.03f))
                }

                TileType.FRIDGE -> {
                    drawRect(Color(0xFFB8C6CE), Offset(x + s * 0.16f, y + s * 0.03f), Size(s * 0.68f, s * 0.94f))
                    drawRect(Color(0xFFE6EEF2), Offset(x + s * 0.19f, y + s * 0.06f), Size(s * 0.62f, s * 0.88f))
                    drawRect(Color(0xFF9AA9B2), Offset(x + s * 0.19f, y + s * 0.4f), Size(s * 0.62f, s * 0.025f))
                    drawRect(Color(0xFF7D8B94), Offset(x + s * 0.7f, y + s * 0.15f), Size(s * 0.05f, s * 0.18f))
                    drawRect(Color(0xFF7D8B94), Offset(x + s * 0.7f, y + s * 0.48f), Size(s * 0.05f, s * 0.25f))
                }

                TileType.TABLE -> {
                    drawRect(Color.Black.copy(alpha = 0.2f), Offset(x + s * 0.04f, y + s * 0.8f), Size(s * 0.96f, s * 0.14f))
                    drawRect(Color(0xFF6E4524), Offset(x, y + s * 0.2f), Size(s, s * 0.66f))
                    drawRect(Color(0xFFA8733F), Offset(x, y + s * 0.2f), Size(s, s * 0.56f))
                    drawRect(p.rugTrim.copy(alpha = 0.8f), Offset(x, y + s * 0.42f), Size(s, s * 0.08f))
                }

                TileType.SOFA -> {
                    drawRoundRect(p.sofa.copy(alpha = 0.55f), Offset(x + s * 0.04f, y + s * 0.08f), Size(s * 0.92f, s * 0.86f), CornerRadius(s * 0.18f))
                    drawRoundRect(p.sofa, Offset(x + s * 0.06f, y + s * 0.06f), Size(s * 0.88f, s * 0.82f), CornerRadius(s * 0.18f))
                    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(x + s * 0.2f, y + s * 0.2f), Size(s * 0.6f, s * 0.54f), CornerRadius(s * 0.12f))
                }

                TileType.BED -> {
                    drawRect(Color(0xFF5A3A22), Offset(x + s * 0.02f, y + s * 0.02f), Size(s * 0.96f, s * 0.96f))
                    drawRect(p.sofa, Offset(x + s * 0.06f, y + s * 0.06f), Size(s * 0.88f, s * 0.88f))
                    drawRoundRect(Color.White, Offset(x + s * 0.12f, y + s * 0.1f), Size(s * 0.46f, s * 0.28f), CornerRadius(s * 0.08f))
                    drawRect(Color.White.copy(alpha = 0.25f), Offset(x + s * 0.06f, y + s * 0.55f), Size(s * 0.88f, s * 0.06f))
                }

                else -> { /* FLOOR_WOOD: solo el piso */ }
            }
        }
    }
}

private val NeonTvDot = Color(0xFF00E676)

// ---------------------------------------------------------------------------
// Bosque Escondido
// ---------------------------------------------------------------------------
private val ForestGrass      = Color(0xFF2F6B35)
private val ForestGrassLight = Color(0xFF3E8444)
private val ForestCanopy     = Color(0xFF1E4D2B)
private val ForestCanopyMid  = Color(0xFF2B6A3A)
private val ForestCanopyTop  = Color(0xFF3F8F4E)
private val ForestTrunk      = Color(0xFF5B3A22)
private val ForestStream     = Color(0xFF2E7DB8)
private val ForestDirt       = Color(0xFF8A6A42)

/** Pasto del bosque con briznas en posiciones fijas por tile (sin parpadeo entre frames). */
private fun DrawScope.drawForestGrass(x: Float, y: Float, s: Float, col: Int, row: Int) {
    drawRect(ForestGrass, Offset(x, y), Size(s, s))
    val seed = (col * 31 + row * 17) and 0xFF
    for (i in 0..3) {
        val bx = x + s * (((seed + i * 53) % 90) / 100f + 0.05f)
        val by = y + s * (((seed * 3 + i * 37) % 80) / 100f + 0.1f)
        drawRect(ForestGrassLight, Offset(bx, by), Size(2f, s * 0.12f))
    }
}

private fun DrawScope.drawForestTile(type: TileType, x: Float, y: Float, s: Float, col: Int, row: Int) {
    drawForestGrass(x, y, s, col, row)
    when (type) {
        TileType.FOREST_FLOWERS -> {
            val petals = listOf(Color(0xFFFFE066), Color(0xFFFF8FB1), Color(0xFFB39DFF))
            listOf(0.25f to 0.3f, 0.7f to 0.45f, 0.4f to 0.75f).forEachIndexed { i, (fx, fy) ->
                drawCircle(petals[(i + col + row) % petals.size], s * 0.07f, Offset(x + s * fx, y + s * fy))
                drawCircle(Color.White.copy(alpha = 0.8f), s * 0.025f, Offset(x + s * fx, y + s * fy))
            }
        }
        TileType.FOREST_TREE -> {
            drawOval(Color.Black.copy(alpha = 0.3f), Offset(x + s * 0.12f, y + s * 0.74f), Size(s * 0.76f, s * 0.22f))
            drawRect(ForestTrunk, Offset(x + s * 0.42f, y + s * 0.55f), Size(s * 0.16f, s * 0.32f))
            drawCircle(ForestCanopy, s * 0.42f, Offset(x + s * 0.5f, y + s * 0.42f))
            drawCircle(ForestCanopyMid, s * 0.32f, Offset(x + s * 0.42f, y + s * 0.36f))
            drawCircle(ForestCanopyTop, s * 0.16f, Offset(x + s * 0.36f, y + s * 0.28f))
        }
        TileType.FOREST_STUMP -> {
            drawOval(Color.Black.copy(alpha = 0.25f), Offset(x + s * 0.18f, y + s * 0.62f), Size(s * 0.64f, s * 0.2f))
            drawRect(ForestTrunk, Offset(x + s * 0.24f, y + s * 0.42f), Size(s * 0.52f, s * 0.3f))
            drawOval(Color(0xFFC79A63), Offset(x + s * 0.24f, y + s * 0.32f), Size(s * 0.52f, s * 0.22f))
            drawOval(ForestTrunk.copy(alpha = 0.6f), Offset(x + s * 0.36f, y + s * 0.37f), Size(s * 0.28f, s * 0.12f), style = Stroke(1.5f))
        }
        TileType.FOREST_MUSHROOMS -> {
            listOf(Triple(0.32f, 0.6f, 0.17f), Triple(0.66f, 0.68f, 0.13f)).forEach { (mx, my, r) ->
                drawRect(Color(0xFFF2E8D5), Offset(x + s * (mx - 0.04f), y + s * my), Size(s * 0.08f, s * 0.16f))
                drawCircle(Color(0xFFD94A3D), s * r, Offset(x + s * mx, y + s * my))
                drawCircle(Color.White, s * 0.025f, Offset(x + s * (mx - r * 0.4f), y + s * (my - r * 0.3f)))
                drawCircle(Color.White, s * 0.02f, Offset(x + s * (mx + r * 0.35f), y + s * (my - r * 0.45f)))
            }
        }
        TileType.FOREST_STREAM -> {
            drawRect(ForestStream, Offset(x, y), Size(s, s))
            for (i in 0..2) {
                val wy = y + s * (0.22f + i * 0.28f)
                drawRect(Color.White.copy(alpha = 0.25f), Offset(x + s * (0.15f + (i % 2) * 0.3f), wy), Size(s * 0.35f, 2f))
            }
        }
        TileType.FOREST_EXIT -> {
            drawRoundRect(ForestDirt, Offset(x + s * 0.12f, y), Size(s * 0.76f, s), CornerRadius(s * 0.1f))
            // Flecha verde de salida (igual que en las casas)
            val arrow = Path().apply {
                moveTo(x + s * 0.5f, y + s * 0.85f)
                lineTo(x + s * 0.28f, y + s * 0.5f)
                lineTo(x + s * 0.72f, y + s * 0.5f)
                close()
            }
            drawPath(arrow, Color(0xFF2ECC71))
            drawRect(Color(0xFF2ECC71), Offset(x + s * 0.43f, y + s * 0.25f), Size(s * 0.14f, s * 0.27f))
        }
        else -> { /* FOREST_GRASS: solo el pasto */ }
    }
}

// ---------------------------------------------------------------------------
// Capa de suelo: pasto, agua, tierra, caminos
// ---------------------------------------------------------------------------
private fun DrawScope.drawGroundLayer(
    type: TileType, x: Float, y: Float, size: Float, assets: WorldAssets
) {
    val rectSize   = Size(size, size)
    val rectOffset = Offset(x, y)

    when (type) {
        // --- Tiles con sprite propio ---
        TileType.EARTH       -> drawSprite(assets.earthImg, x, y, size)
        TileType.CROSSROADS  -> {
            drawSprite(assets.grassSheet, x, y, size, srcGridX = 0, srcGridY = 0)
            drawSprite(assets.crossroadsImg, x, y, size)
        }
        TileType.PATH_CORNER -> {
            drawSprite(assets.grassSheet, x, y, size, srcGridX = 0, srcGridY = 0)
            drawSprite(assets.cornerImg, x, y, size)
        }
        TileType.WATER_IMG,
        TileType.WATER_DEEP,
        TileType.WATER_SHALLOW -> drawSprite(assets.water5Img, x, y, size)

        // Puente (tabla de madera sobre el nuevo agua)
        TileType.BRIDGE -> {
            drawSprite(assets.water5Img, x, y, size)
            repeat(5) { i ->
                drawRect(color = WoodColor,
                    topLeft = Offset(x + 2f, y + i * (size / 5) + 1f),
                    size    = Size(size - 4f, size / 5 - 2f))
            }
        }

        // Caminos geométricos
        TileType.PATH_DIRT -> {
            drawSprite(assets.grassSheet, x, y, size, srcGridX = 0, srcGridY = 0)
            drawRoundRect(color = PathDirt,
                topLeft = Offset(x + 2f, y + 2f), size = Size(size - 4f, size - 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.15f))
        }
        TileType.PATH_STONE -> drawSprite(assets.baseImg, x, y, size)

        // Acantilados
        TileType.CLIFF_TOP  -> drawRect(color = CliffTop, topLeft = rectOffset, size = rectSize)
        TileType.CLIFF_FACE -> {
            drawRect(color = CliffFace, topLeft = rectOffset, size = rectSize)
            drawRect(color = CliffBase, topLeft = Offset(x, y + size * 0.35f), size = Size(size, 3f))
            drawRect(color = CliffBase, topLeft = Offset(x, y + size * 0.7f),  size = Size(size, 3f))
        }
        TileType.CLIFF_BASE -> {
            drawRect(color = CliffBase, topLeft = rectOffset, size = rectSize)
            drawRect(color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(x, y + size * 0.5f), size = Size(size, size * 0.5f))
        }

        // Mazmorra: la imagen tiene fondo transparente, así que va sobre pasto
        // (antes un suelo casi negro dejaba un recuadro oscuro detrás de cada imagen)
        TileType.LAIR, TileType.LAIR_DOOR -> drawSprite(assets.grassSheet, x, y, size, srcGridX = 0, srcGridY = 0)

        // Tierra para cementerio
        TileType.GRAVE, TileType.SKULL -> drawSprite(assets.earthImg, x, y, size)

        // Suelo de pasto por defecto (usando el sprite)
        else -> drawSprite(assets.grassSheet, x, y, size, srcGridX = 0, srcGridY = 0)
    }
}


// ---------------------------------------------------------------------------
// Capa de objetos: decoraciones encima del suelo
// ---------------------------------------------------------------------------
private fun DrawScope.drawObjectLayer(
    type: TileType, x: Float, y: Float, size: Float, assets: WorldAssets
) {
    when (type) {
        // --- Sprites reales ---
        TileType.TREE       -> {
            drawShadow(x, y, size)
            drawSprite(assets.lukeImg, x, y, size)
        }
        TileType.BUSH       -> {
            drawShadow(x, y, size, alpha = 0.2f)
            drawSprite(assets.bushImg, x, y, size)
        }
        TileType.MUSHROOM   -> drawSprite(assets.mushroomImg, x, y, size)
        TileType.SKULL      -> drawSprite(assets.skullImg, x, y, size)
        TileType.GRAVE      -> {
            drawShadow(x, y, size, alpha = 0.3f)
            drawSprite(assets.graveImg, x, y, size)
        }
        TileType.CARTEL     -> {
            drawShadow(x, y, size, alpha = 0.2f)
            drawSprite(assets.cartelImg, x, y, size)
        }
        TileType.LAIR       -> drawSprite(assets.lairImg, x, y, size)
        TileType.LAIR_DOOR  -> {
            // Puerta luminosa de la mazmorra: entrada al Bosque Escondido
            drawRect(color = Color(0xFFAA55FF).copy(alpha = 0.5f),
                topLeft = Offset(x + size * 0.25f, y + size * 0.3f),
                size    = Size(size * 0.5f, size * 0.7f))
            drawRect(color = Color(0xFFFFFFFF).copy(alpha = 0.2f),
                topLeft = Offset(x + size * 0.43f, y + size * 0.3f),
                size    = Size(size * 0.14f, size * 0.7f))
        }

        // --- Objetos geométricos ---
        TileType.GRASS_TALL -> {
            drawRect(color = GrassDark, topLeft = Offset(x, y + size * 0.2f), size = Size(size, 3f))
            drawRect(color = GrassDark, topLeft = Offset(x, y + size * 0.5f), size = Size(size, 3f))
        }
        TileType.FENCE -> {
            drawRect(color = WoodColor, topLeft = Offset(x + size * 0.4f, y + size * 0.2f), size = Size(size * 0.2f, size * 0.8f))
            drawRect(color = WoodColor, topLeft = Offset(x, y + size * 0.4f), size = Size(size, size * 0.15f))
            drawRect(color = WoodColor, topLeft = Offset(x, y + size * 0.7f), size = Size(size, size * 0.15f))
        }
        TileType.ROCK -> {
            drawShadow(x, y, size, alpha = 0.25f)
            drawCircle(color = Color(0xFF888888), radius = size * 0.3f, center = Offset(x + size / 2, y + size * 0.6f))
            drawCircle(color = Color(0xFFAAAAAA), radius = size * 0.2f, center = Offset(x + size * 0.4f, y + size * 0.5f))
        }
        TileType.FLOWER -> {
            drawCircle(color = Color.White,          radius = 5f,  center = Offset(x + size * 0.3f, y + size * 0.35f))
            drawCircle(color = Color(0xFFFF5555),    radius = 2f,  center = Offset(x + size * 0.3f, y + size * 0.35f))
            drawCircle(color = Color.White,          radius = 5f,  center = Offset(x + size * 0.7f, y + size * 0.65f))
            drawCircle(color = Color(0xFF5599FF),    radius = 2f,  center = Offset(x + size * 0.7f, y + size * 0.65f))
        }
        TileType.HOUSE_RED -> {
            drawRect(color = Color(0xFFF0F0F0), topLeft = Offset(x, y), size = Size(size, size))
            drawRect(color = Color(0xFFE05040), topLeft = Offset(x, y), size = Size(size, size * 0.6f))
            drawRect(color = Color(0xFFC04030), topLeft = Offset(x, y + size * 0.6f), size = Size(size, 4f))
        }
        TileType.HOUSE_BLUE -> {
            drawRect(color = Color(0xFFF0F0F0), topLeft = Offset(x, y), size = Size(size, size))
            drawRect(color = Color(0xFF4080E0), topLeft = Offset(x, y), size = Size(size, size * 0.6f))
            drawRect(color = Color(0xFF3060C0), topLeft = Offset(x, y + size * 0.6f), size = Size(size, 4f))
        }
        TileType.HOUSE_DOOR -> {
            drawRect(color = Color(0xFFF0F0F0), topLeft = Offset(x, y), size = Size(size, size))
            drawRect(color = WoodColor, topLeft = Offset(x + size * 0.25f, y + size * 0.3f), size = Size(size * 0.5f, size * 0.7f))
            drawCircle(color = Color.Yellow, radius = 2f, center = Offset(x + size * 0.65f, y + size * 0.65f))
        }
        TileType.ACADEMY -> {
            drawRect(color = Color(0xFFE8E8E8), topLeft = Offset(x, y), size = Size(size, size))
            drawRect(color = Color(0xFF88AABB), topLeft = Offset(x + 4f, y + 4f), size = Size(size - 8f, size * 0.4f))
        }
        TileType.ACADEMY_DOOR -> {
            drawRect(color = Color(0xFFE8E8E8), topLeft = Offset(x, y), size = Size(size, size))
            drawRect(color = Color(0xFF66AACC), topLeft = Offset(x + size * 0.1f, y + size * 0.3f), size = Size(size * 0.8f, size * 0.7f))
            drawRect(color = Color.White.copy(alpha = 0.5f), topLeft = Offset(x + size * 0.45f, y + size * 0.3f), size = Size(size * 0.1f, size * 0.7f))
        }

        else -> { /* Sin decoración: GRASS, PATH_STONE, etc. */ }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Dibuja un sprite PNG escalado al tamaño del tile. */
private fun DrawScope.drawSprite(
    bitmap: ImageBitmap,
    x: Float, y: Float, dstSize: Float,
    srcGridX: Int = 0, srcGridY: Int = 0, srcPxSize: Int = 0
) {
    val useSrcRegion = srcPxSize > 0
    if (useSrcRegion) {
        drawImage(
            image        = bitmap,
            srcOffset    = IntOffset(srcGridX * srcPxSize, srcGridY * srcPxSize),
            srcSize      = IntSize(srcPxSize, srcPxSize),
            dstOffset    = IntOffset(x.toInt(), y.toInt()),
            dstSize      = IntSize(dstSize.toInt(), dstSize.toInt()),
            filterQuality = FilterQuality.None
        )
    } else {
        drawImage(
            image        = bitmap,
            dstOffset    = IntOffset(x.toInt(), y.toInt()),
            dstSize      = IntSize(dstSize.toInt(), dstSize.toInt()),
            filterQuality = FilterQuality.None
        )
    }
}

/** Sombra ovalada genérica debajo de un objeto. */
private fun DrawScope.drawShadow(x: Float, y: Float, size: Float, alpha: Float = 0.3f) {
    drawOval(
        color    = Color.Black.copy(alpha = alpha),
        topLeft  = Offset(x + size * 0.15f, y + size * 0.72f),
        size     = Size(size * 0.7f, size * 0.28f)
    )
}
