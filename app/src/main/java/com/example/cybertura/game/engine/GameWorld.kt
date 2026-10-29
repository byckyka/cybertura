package com.example.cybertura.game.engine

/**
 * Tipos de tile del mundo RPG.
 */
enum class TileType(val walkable: Boolean) {
    GRASS(true),
    GRASS_TALL(true),
    PATH_DIRT(true),
    PATH_STONE(true),
    CROSSROADS(true),
    PATH_CORNER(true),
    EARTH(true),

    WATER_SHALLOW(false),
    WATER_DEEP(false),
    WATER_IMG(false),

    BRIDGE(true),

    CLIFF_TOP(false),
    CLIFF_FACE(false),
    CLIFF_BASE(false),

    TREE(false),
    BUSH(false),
    MUSHROOM(true),
    FENCE(false),
    ROCK(false),
    FLOWER(true),
    SKULL(false),
    GRAVE(false),
    CARTEL(false),

    HOUSE_RED(false),
    HOUSE_BLUE(false),
    HOUSE_DOOR(true),
    ACADEMY(false),
    ACADEMY_DOOR(true),
    LAIR(false),
    LAIR_DOOR(true),

    // ── Interiores de casas ──
    VOID(false),
    FLOOR_WOOD(true),
    RUG(true),
    WALL(false),
    WALL_WINDOW(false),
    WALL_PICTURE(false),
    BOOKSHELF(false),
    PLANT(false),
    LAMP(false),
    TV(false),
    KITCHEN(false),
    FRIDGE(false),
    TABLE(false),
    SOFA(false),
    BED(false),
    EXIT_DOOR(true),

    // ── Bosque Escondido (detrás de la puerta de la cueva) ──
    FOREST_GRASS(true),
    FOREST_FLOWERS(true),
    FOREST_TREE(false),
    FOREST_STUMP(false),
    FOREST_MUSHROOMS(false),
    FOREST_STREAM(false),
    FOREST_EXIT(true)
}

/** Identificadores de los mapas del juego (exterior + interiores de casas). */
object MapIds {
    const val EXTERIOR = "exterior"
    const val HOUSE_RED_N = "house_red_n"
    const val HOUSE_BLUE_N = "house_blue_n"
    const val HOUSE_RED_S = "house_red_s"
    const val HOUSE_BLUE_S = "house_blue_s"
    /** Bosque al que se entra por la puerta de la cueva (no es una casa, pero funciona como interior). */
    const val FOREST = "forest"
}

/**
 * Puerta que conecta dos mapas. El jugador la activa al pisar el tile (tileX, tileY) de [fromMap]
 * y aparece en (toX, toY) de [toMap].
 */
data class Portal(
    val fromMap: String,
    val tileX: Int,
    val tileY: Int,
    val toMap: String,
    val toX: Float,
    val toY: Float
)

object GameWorld {

    const val MAP_WIDTH  = 50
    const val MAP_HEIGHT = 50
    const val TILE_SIZE  = 56f

    // ── Shorthands ──────────────────────────────────────────────────────────
    private const val G  = 0   // GRASS
    private const val GT = 1   // GRASS_TALL
    private const val PD = 2   // PATH_DIRT
    private const val PS = 3   // PATH_STONE
    private const val CR = 4   // CROSSROADS
    private const val PC = 5   // PATH_CORNER
    private const val ER = 6   // EARTH

    private const val W  = 7   // WATER_SHALLOW
    private const val WD = 8   // WATER_DEEP
    private const val WI = 9   // WATER_IMG

    private const val BR = 10  // BRIDGE

    private const val CT = 11  // CLIFF_TOP
    private const val CF = 12  // CLIFF_FACE
    private const val CB = 13  // CLIFF_BASE

    private const val T  = 14  // TREE
    private const val BS = 15  // BUSH
    private const val MU = 16  // MUSHROOM
    private const val FN = 17  // FENCE
    private const val RK = 18  // ROCK
    private const val F  = 19  // FLOWER
    private const val SK = 20  // SKULL
    private const val GV = 21  // GRAVE
    private const val CA = 22  // CARTEL

    private const val HR = 23  // HOUSE_RED
    private const val HB = 24  // HOUSE_BLUE
    private const val HD = 25  // HOUSE_DOOR
    private const val AC = 26  // ACADEMY
    private const val AD = 27  // ACADEMY_DOOR
    private const val LR = 28  // LAIR
    private const val LD = 29  // LAIR_DOOR

    // ── Hash determinístico para scatter natural (sin cuadrículas visibles) ──
    private fun hash(c: Int, r: Int): Int {
        val h = (c * 7919 + r * 6271 + c * r * 31) and 0x7FFFFFFF
        return h % 100
    }

    // ── Mapa ─────────────────────────────────────────────────────────────────
    private val mapData: Array<IntArray> = Array(MAP_HEIGHT) { row ->
        IntArray(MAP_WIDTH) { col -> generateMapTile(col, row) }
    }

    private fun generateMapTile(c: Int, r: Int): Int {
        val h = hash(c, r)

        // ── Bordes: agua profunda ────────────────────────────────────────────
        if (c < 2 || c > MAP_WIDTH - 3 || r < 2 || r > MAP_HEIGHT - 3) return WD

        // ── Océano y costa oeste ─────────────────────────────────────────────
        if (c < 6) return WI
        if (c in 6..7) {
            // Muelles de madera sobre el agua
            if (r in 24..26) return BR
            return PD
        }

        // ── Montaña Norte (fila 2-9) ─────────────────────────────────────────
        if (r < 10) {
            return when (r) {
                9    -> CB
                8    -> CF
                else -> CT
            }
        }

        // ── Montaña Este (col 42-47) ─────────────────────────────────────────
        if (c > 41) {
            return if (c == 42) CF else CT
        }

        // ── Bosque Sur (filas 43-47) ─────────────────────────────────────────
        // Sin árboles del hash — los árboles van en posiciones fijas
        if (r > 42) {
            if (h < 15) return BS        // solo arbustos sueltos
            if (h < 18) return MU        // algún hongo
            return G
        }


        // ════════════════════════════════════════════════════════════════════
        //  CIUDAD CENTRAL
        // ════════════════════════════════════════════════════════════════════

        // ── Caminos principales (cruz) ────────────────────────────────────
        // ── Caminos de acceso a las casas (puertas al sur de cada casa) ───
        // Casas del norte: tramo abierto en la cerca + sendero hasta el camino principal
        if ((c == 14 || c == 34) && r in 17..23) return PD
        // Casas del sur: sendero bajo el lago/plaza hasta el camino vertical
        if (c == 13 && r in 36..37) return PD
        if (c == 35 && r in 36..37) return PD
        if (r == 37 && c in 13..23) return PD
        if (r == 37 && c in 27..35) return PD

        val onHPath = r in 24..26 && c in 8..41
        val onVPath = c in 24..26 && r in 10..43

        if (onHPath && onVPath) return CR          // intersección real
        if (onHPath || onVPath)  return PS

        // ── Academia (Norte-Centro, 7×4 tiles) ───────────────────────────
        if (c in 22..28 && r in 11..14) {
            return if (r == 14 && c == 25) AD else AC
        }
        // Camino de acceso a la academia
        if (c in 24..26 && r in 15..17) return PS

        // ── Mazmorra / Cueva (Suroeste, 4×3 tiles — más pequeña) ─────────
        if (c in 9..12 && r in 29..31) {
            return if (r == 31 && c == 10) LD else LR
        }

        // ── Casa Roja (Centro-Oeste) ──────────────────────────────────────
        if (c in 12..16 && r in 13..16) {
            return if (r == 16 && c == 14) HD else HR
        }

        // ── Casa Azul (Centro-Este) ───────────────────────────────────────
        if (c in 32..36 && r in 13..16) {
            return if (r == 16 && c == 34) HD else HB
        }

        // ── Segunda Casa Roja (Sur-Oeste) ────────────────────────────────
        if (c in 12..15 && r in 33..35) {
            return if (r == 35 && c == 13) HD else HR
        }

        // ── Segunda Casa Azul (Sur-Este) ─────────────────────────────────
        if (c in 33..36 && r in 33..35) {
            return if (r == 35 && c == 35) HD else HB
        }

        // ── Cercas alrededor de las casas del norte ───────────────────────
        if (r == 17 && c in 11..17) return FN
        if (r == 17 && c in 31..37) return FN
        if (c == 11 && r in 13..17) return FN
        if (c == 37 && r in 13..17) return FN

        // ── Lago interior (fila 28-36, col 16-21) ───────────────────────
        if (c in 16..21 && r in 28..36) {
            if (r == 32 && c == 18) return BR  // Puente sobre el lago
            return WI
        }

        // ── Cementerio pequeño y ordenado (Solo 5×4 tiles, sureste) ──────
        if (c in 37..41 && r in 34..37) {
            // Valla perimetral
            if (r == 34 || r == 37) return FN
            if (c == 37 || c == 41) return FN
            // Interior: solo 4 lápidas en fila ordenada
            return when {
                c == 38 && r == 35 -> GV
                c == 39 && r == 35 -> SK
                c == 40 && r == 35 -> GV
                c == 38 && r == 36 -> GV
                c == 40 && r == 36 -> GV
                else               -> G   // pasto limpio entre las lápidas
            }
        }

        // ── Plaza sur con fuente (zona abierta y limpia) ─────────────────
        // Sin árboles ni arbustos en la plaza — zona abierta

        // ── Carteles de dirección (solo 2, bien ubicados) ────────────────
        if (c == 23 && r == 23) return CA
        if (c == 27 && r == 23) return CA

        // ── Árboles: exactamente 6 en todo el mapa, posiciones fijas ───────
        // 4 junto a las casas del norte
        if (c == 11 && r == 12) return T
        if (c == 17 && r == 12) return T
        if (c == 31 && r == 12) return T
        if (c == 37 && r == 12) return T
        // 2 en el borde sur del mapa (zona boscosa)
        if (c == 15 && r == 43) return T
        if (c == 32 && r == 44) return T

        // ── Flores solo en bordes de caminos (posiciones exactas) ─────────
        if (r == 23 && (c == 15 || c == 20 || c == 30 || c == 35)) return F
        if (r == 27 && (c == 15 || c == 20 || c == 30 || c == 35)) return F

        // ── Arbustos: SOLO en la franja exterior de la ciudad (borde) ────
        // Ningún arbusto dentro del área central de la ciudad
        val isOuterArea = c in 8..11 || c in 38..41 || r in 10..13 || r in 38..42
        if (isOuterArea && h < 18) return BS  // 18% solo en bordes exteriores

        // ── Sin rocas en la ciudad — ciudad limpia ────────────────────────
        return G
    }

    // ── Lookup table TileType ─────────────────────────────────────────────
    private val tileTypes = arrayOf(
        TileType.GRASS, TileType.GRASS_TALL, TileType.PATH_DIRT, TileType.PATH_STONE,
        TileType.CROSSROADS, TileType.PATH_CORNER, TileType.EARTH,
        TileType.WATER_SHALLOW, TileType.WATER_DEEP, TileType.WATER_IMG,
        TileType.BRIDGE,
        TileType.CLIFF_TOP, TileType.CLIFF_FACE, TileType.CLIFF_BASE,
        TileType.TREE, TileType.BUSH, TileType.MUSHROOM, TileType.FENCE,
        TileType.ROCK, TileType.FLOWER, TileType.SKULL, TileType.GRAVE, TileType.CARTEL,
        TileType.HOUSE_RED, TileType.HOUSE_BLUE, TileType.HOUSE_DOOR,
        TileType.ACADEMY, TileType.ACADEMY_DOOR,
        TileType.LAIR, TileType.LAIR_DOOR
    )

    fun getTile(col: Int, row: Int): TileType {
        if (row < 0 || row >= MAP_HEIGHT || col < 0 || col >= MAP_WIDTH) return TileType.WATER_DEEP
        return tileTypes[mapData[row][col]]
    }

    /** Tile de cualquier mapa (exterior o interior). Fuera de un interior = VOID. */
    fun getTile(mapId: String, col: Int, row: Int): TileType {
        if (mapId == MapIds.EXTERIOR) return getTile(col, row)
        val grid = interiors[mapId] ?: return TileType.VOID
        if (row < 0 || row >= grid.size || col < 0 || col >= grid[0].size) return TileType.VOID
        return grid[row][col]
    }

    fun isWalkable(col: Int, row: Int): Boolean = getTile(col, row).walkable

    fun isWalkable(mapId: String, col: Int, row: Int): Boolean = getTile(mapId, col, row).walkable

    fun isInterior(mapId: String): Boolean = mapId != MapIds.EXTERIOR

    // Spawn del jugador en la plaza central
    val playerSpawnX = 25f
    val playerSpawnY = 30f

    // ── Interiores ───────────────────────────────────────────────────────
    // Leyenda: W pared · w ventana · p cuadro · K librero · P planta · L lámpara · V TV
    //          F cocina · R nevera · T mesa · S sofá · B cama · c alfombra · . piso · D puerta
    private val layoutHomeRed = listOf(
        "WpwWWpWwWpW",
        "WKKP.V.FFRW",
        "W.........W",
        "WB.cccc..SW",
        "WB.cccc..SW",
        "W..cccc...W",
        "W.TT....P.W",
        "W.........W",
        "WWWWWDWWWWW"
    )
    private val layoutHomeBlue = listOf(
        "WwWpWwWpWwW",
        "WFFRP...KKW",
        "W.........W",
        "W.TTT.cccSW",
        "W.....cccSW",
        "W.....ccc.W",
        "WBB.......W",
        "WL.......PW",
        "WWWWWDWWWWW"
    )
    // Casa Roja del sur: taller de redes y dispositivos de Técnica Wifi
    private val layoutWorkshop = listOf(
        "WpWwWWwWpWW",
        "WKKKP.VVL.W",
        "W.........W",
        "W.TTTT.cc.W",
        "W......ccPW",
        "WSS....cc.W",
        "W.........W",
        "WP.......LW",
        "WWWWWDWWWWW"
    )
    // Casa Azul del sur: cuarto gamer de Capitán Joystick
    private val layoutGamer = listOf(
        "WpWwWpWwWpW",
        "WVVLR.KKKPW",
        "W.........W",
        "W.SSS.cccBW",
        "W.....cccBW",
        "W.....ccc.W",
        "W.TT......W",
        "WP.......PW",
        "WWWWWDWWWWW"
    )

    // Bosque Escondido de la Druida Raíz.
    // Leyenda: T árbol · . pasto · f flores · M hongos · S tronco · w arroyo · E salida
    private val layoutForest = listOf(
        "TTTTTTTTTTT",
        "TMf..S..fwT",
        "Tf.......wT",
        "T...f....wT",
        "TS.......wT",
        "T..f...f.wT",
        "Tf.......fT",
        "TM.......MT",
        "TTTTTETTTTT"
    )

    private fun parseForest(rows: List<String>): Array<Array<TileType>> =
        Array(rows.size) { r ->
            Array(rows[r].length) { c ->
                when (rows[r][c]) {
                    'T' -> TileType.FOREST_TREE
                    'f' -> TileType.FOREST_FLOWERS
                    'M' -> TileType.FOREST_MUSHROOMS
                    'S' -> TileType.FOREST_STUMP
                    'w' -> TileType.FOREST_STREAM
                    'E' -> TileType.FOREST_EXIT
                    else -> TileType.FOREST_GRASS
                }
            }
        }

    private fun parseLayout(rows: List<String>): Array<Array<TileType>> =
        Array(rows.size) { r ->
            Array(rows[r].length) { c ->
                when (rows[r][c]) {
                    'W' -> TileType.WALL
                    'w' -> TileType.WALL_WINDOW
                    'p' -> TileType.WALL_PICTURE
                    'K' -> TileType.BOOKSHELF
                    'P' -> TileType.PLANT
                    'L' -> TileType.LAMP
                    'V' -> TileType.TV
                    'F' -> TileType.KITCHEN
                    'R' -> TileType.FRIDGE
                    'T' -> TileType.TABLE
                    'S' -> TileType.SOFA
                    'B' -> TileType.BED
                    'c' -> TileType.RUG
                    'D' -> TileType.EXIT_DOOR
                    else -> TileType.FLOOR_WOOD
                }
            }
        }

    private val interiors: Map<String, Array<Array<TileType>>> = mapOf(
        MapIds.HOUSE_RED_N to parseLayout(layoutHomeRed),
        MapIds.HOUSE_BLUE_N to parseLayout(layoutHomeBlue),
        MapIds.HOUSE_RED_S to parseLayout(layoutWorkshop),
        MapIds.HOUSE_BLUE_S to parseLayout(layoutGamer),
        MapIds.FOREST to parseForest(layoutForest)
    )

    private const val DOOR_COL = 5
    private const val DOOR_ROW = 8
    private const val ENTRY_SPAWN_ROW = 7f

    /** Puertas: cada casa tiene una entrada exterior y una salida interior. */
    val portals: List<Portal> = listOf(
        // (puerta exterior) → interior
        Triple(MapIds.HOUSE_RED_N, 14, 16),
        Triple(MapIds.HOUSE_BLUE_N, 34, 16),
        Triple(MapIds.HOUSE_RED_S, 13, 35),
        Triple(MapIds.HOUSE_BLUE_S, 35, 35),
        // Puerta luminosa de la cueva (suroeste) → Bosque Escondido
        Triple(MapIds.FOREST, 10, 31)
    ).flatMap { (house, doorX, doorY) ->
        listOf(
            Portal(MapIds.EXTERIOR, doorX, doorY, house, DOOR_COL.toFloat(), ENTRY_SPAWN_ROW),
            Portal(house, DOOR_COL, DOOR_ROW, MapIds.EXTERIOR, doorX.toFloat(), doorY + 1f)
        )
    }

    /** Portal que el jugador está pisando (según sus pies, igual que la caja de colisión), o null. */
    fun portalAt(mapId: String, playerX: Float, playerY: Float): Portal? {
        val tx = kotlin.math.floor(playerX + 0.5f).toInt()
        val ty = kotlin.math.floor(playerY + 0.75f).toInt()
        return portals.firstOrNull { it.fromMap == mapId && it.tileX == tx && it.tileY == ty }
    }

    /**
     * Portal que debe indicarse al jugador para llegar al mapa [targetMap] desde [currentMap]:
     * desde la calle, la puerta de la casa destino; desde una casa equivocada, su puerta de salida.
     */
    fun guidePortal(currentMap: String, targetMap: String): Portal? = when {
        currentMap == targetMap -> null
        currentMap == MapIds.EXTERIOR -> portals.firstOrNull { it.fromMap == MapIds.EXTERIOR && it.toMap == targetMap }
        else -> portals.firstOrNull { it.fromMap == currentMap && it.toMap == MapIds.EXTERIOR }
    }

    fun mapName(mapId: String): String = when (mapId) {
        MapIds.HOUSE_RED_N -> "🏠 Casa Roja del Norte"
        MapIds.HOUSE_BLUE_N -> "🏠 Casa Azul del Norte"
        MapIds.HOUSE_RED_S -> "🏠 Casa Roja del Sur · Taller Wi-Fi"
        MapIds.HOUSE_BLUE_S -> "🏠 Casa Azul del Sur · Cuarto Gamer"
        MapIds.FOREST -> "🌲 Bosque Escondido"
        else -> ""
    }
}
