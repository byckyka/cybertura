package com.example.cybertura.data.model

import com.google.gson.annotations.SerializedName

data class GameData(
    @SerializedName("version") val version: String = "1.0",
    @SerializedName("modules") val modules: List<CyberModule> = emptyList()
)

data class CyberModule(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("icon") val icon: String = "shield",
    @SerializedName("accentColor") val accentColor: String = "#00D2B4",
    @SerializedName("badgeId") val badgeId: String = "",
    @SerializedName("badgeTitle") val badgeTitle: String = "",
    @SerializedName("badgeDescription") val badgeDescription: String = "",
    @SerializedName("challenges") val challenges: List<Challenge> = emptyList()
)

data class Challenge(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String = "MULTIPLE_CHOICE",
    @SerializedName("storyContext") val storyContext: String = "",
    @SerializedName("question") val question: String,
    @SerializedName("points") val points: Int = 100,
    @SerializedName("educationalTip") val educationalTip: String = "",
    @SerializedName("options") val options: List<Option> = emptyList(),
    // ── Solo para type = SWIPE_SORT (minijuego de deslizar) ──
    // Nullable: Gson no usa los valores por defecto de Kotlin, y los retos de opción
    // múltiple no traen estos campos en el JSON.
    @SerializedName("cards") val cards: List<SwipeCard>? = null,
    /** Etiqueta del lado derecho (ej. "Seguro"). */
    @SerializedName("safeLabel") val safeLabel: String? = null,
    /** Etiqueta del lado izquierdo (ej. "Trampa"). */
    @SerializedName("trapLabel") val trapLabel: String? = null,
    // ── Solo para type = SPOT_THE_CLUES (minijuego del detective) ──
    @SerializedName("scene") val scene: ClueScene? = null,
    @SerializedName("clues") val clues: List<Clue>? = null,
    // ── Solo para type = CHAT_STORY (minijuego del chat de grupo) ──
    @SerializedName("chat") val chat: ChatStory? = null,
    // ── Solo para type = FORGE_KEY (minijuego de la forja de llaves) ──
    @SerializedName("forge") val forge: ForgeChallenge? = null,
    // ── Solo para type = TAP_DEFENSE (arcade "Defiende el bosque") ──
    @SerializedName("arcade") val arcade: ArcadeChallenge? = null
) {
    companion object {
        const val TYPE_SWIPE_SORT = "SWIPE_SORT"
        const val TYPE_SPOT_THE_CLUES = "SPOT_THE_CLUES"
        const val TYPE_CHAT_STORY = "CHAT_STORY"
        const val TYPE_FORGE_KEY = "FORGE_KEY"
        const val TYPE_TAP_DEFENSE = "TAP_DEFENSE"
    }
}

/**
 * Arcade de reacción: caen objetos hacia el bosque; hay que tocar las amenazas para
 * destruirlas y dejar pasar lo seguro. Se juega en oleadas cada vez más rápidas.
 */
data class ArcadeChallenge(
    /** Escudos por oleada; si se acaban, la oleada se repite. */
    @SerializedName("shields") val shields: Int? = null,
    @SerializedName("items") val items: List<ArcadeItem> = emptyList(),
    @SerializedName("waves") val waves: List<ArcadeWave> = emptyList()
)

data class ArcadeItem(
    @SerializedName("id") val id: String,
    @SerializedName("emoji") val emoji: String,
    /** Texto corto (cabe en 2 líneas de una tarjeta pequeña). */
    @SerializedName("text") val text: String,
    @SerializedName("isThreat") val isThreat: Boolean,
    /** Explicación si el niño se equivoca con este objeto. */
    @SerializedName("feedback") val feedback: String
)

data class ArcadeWave(
    /** Cuántos objetos caen en la oleada. */
    @SerializedName("count") val count: Int,
    /** Milisegundos que tarda un objeto en caer hasta el bosque. */
    @SerializedName("fallMs") val fallMs: Long? = null,
    /** Milisegundos entre la aparición de un objeto y el siguiente. */
    @SerializedName("spawnMs") val spawnMs: Long? = null,
    /** Consejo que se muestra al superar la oleada. */
    @SerializedName("tip") val tip: String? = null
)

/** Forja de llaves: rondas de construir contraseñas y de defenderlas de un ladrón. */
data class ForgeChallenge(
    @SerializedName("rounds") val rounds: List<ForgeRound> = emptyList()
)

/**
 * Ronda de la forja. Si tiene [options] es de DEFENDER (el niño decide qué hacer con su
 * llave ante una situación); si no, es de CONSTRUIR (arma la llave con [blocks]).
 */
data class ForgeRound(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String? = null,
    /** Explicación de la ronda (ej. "Forja la llave de tu cuenta del juego espacial"). */
    @SerializedName("goal") val goal: String? = null,
    // Construir
    /** Dureza mínima: 0 Madera, 1 Piedra, 2 Hierro, 3 Diamante. */
    @SerializedName("minLevel") val minLevel: Int? = null,
    @SerializedName("blocks") val blocks: List<ForgeBlock>? = null,
    // Defender
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("situation") val situation: String? = null,
    @SerializedName("options") val options: List<ForgeOption>? = null
) {
    val isDefend: Boolean get() = !options.isNullOrEmpty()
}

/** Bloque para armar la llave. Los bloques trampa (nombre, cumpleaños…) hacen que el ladrón la adivine. */
data class ForgeBlock(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    /** Texto pequeño bajo el bloque (ej. "tu mascota"). */
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("isTrap") val isTrap: Boolean = false,
    /** Por qué el ladrón adivina este bloque. */
    @SerializedName("feedback") val feedback: String? = null
)

data class ForgeOption(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("isGood") val isGood: Boolean,
    @SerializedName("feedback") val feedback: String
)

/**
 * Historia en un chat de grupo. Avanza paso a paso: en cada paso llegan mensajes y el
 * jugador elige qué responder. Las malas decisiones se explican y se puede volver a elegir;
 * el último paso (sin opciones) es el final de la historia.
 */
data class ChatStory(
    /** Nombre del grupo (ej. "4° B 🏫"). */
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    /** Medidor que reacciona a las decisiones (ej. "Cómo se siente Mateo"). */
    @SerializedName("meterLabel") val meterLabel: String? = null,
    /** Iconos del medidor, de peor a mejor. */
    @SerializedName("meterIcons") val meterIcons: List<String>? = null,
    /** Posición inicial del medidor (índice en [meterIcons]). */
    @SerializedName("meterStart") val meterStart: Int? = null,
    @SerializedName("steps") val steps: List<ChatStep> = emptyList()
)

data class ChatStep(
    @SerializedName("messages") val messages: List<ChatMessage>? = null,
    /** Pregunta que se muestra sobre las opciones (ej. "¿Qué respondes?"). */
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("choices") val choices: List<ChatChoice>? = null
)

/** Mensaje de otra persona. Sin [from] se muestra como nota del sistema (ej. "Chat privado con Mateo"). */
data class ChatMessage(
    @SerializedName("from") val from: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("text") val text: String
)

data class ChatChoice(
    @SerializedName("id") val id: String,
    /** Lo que el jugador escribe en el chat. */
    @SerializedName("text") val text: String,
    @SerializedName("isGood") val isGood: Boolean,
    /** Explicación de por qué esa decisión ayuda o no. */
    @SerializedName("feedback") val feedback: String,
    /** Cómo reaccionan los demás a la respuesta. */
    @SerializedName("reactions") val reactions: List<ChatMessage>? = null
)

/**
 * Escena que el niño inspecciona (un correo, un chat, una publicación, una ventana…).
 * En [title], [lines] y [button] las pistas se marcan así: `[[idPista|texto visible]]`.
 */
data class ClueScene(
    /** Nombre de la "app" donde ocurre (ej. "Correo", "Chat del juego"). */
    @SerializedName("app") val app: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("lines") val lines: List<String>? = null,
    /** Botón de llamada a la acción que aparece al final (ej. "RECLAMAR PREMIO"). */
    @SerializedName("button") val button: String? = null
)

/** Pista escondida en la escena. */
data class Clue(
    @SerializedName("id") val id: String,
    /** Nombre corto de la señal (ej. "Remitente raro"). */
    @SerializedName("label") val label: String,
    /** Por qué es sospechosa; se muestra al encontrarla. */
    @SerializedName("explanation") val explanation: String,
    /** Ayuda para encontrarla si el niño se atasca. */
    @SerializedName("hint") val hint: String? = null
)

/** Carta del minijuego de deslizar: derecha = segura, izquierda = trampa. */
data class SwipeCard(
    @SerializedName("id") val id: String,
    /** Quién lo envía o de dónde viene (ej. "Número desconocido"). Opcional. */
    @SerializedName("sender") val sender: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
    @SerializedName("text") val text: String,
    @SerializedName("isSafe") val isSafe: Boolean,
    /** Explicación que se muestra tras clasificarla (acierte o falle). */
    @SerializedName("feedback") val feedback: String
)

data class Option(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("feedback") val feedback: String
)
