package com.example.cybertura.game.engine

/**
 * Dureza de una contraseña para la Forja de llaves, en 4 materiales.
 * Combina el largo y la variedad de caracteres (mayúsculas, minúsculas, números, símbolos).
 */
object KeyStrength {

    data class Material(val emoji: String, val name: String)

    val MATERIALS = listOf(
        Material("🪵", "Madera"),
        Material("🪨", "Piedra"),
        Material("🔩", "Hierro"),
        Material("💎", "Diamante")
    )
    val MAX_LEVEL = MATERIALS.size - 1

    private fun lengthPoints(key: String) = when {
        key.length >= 16 -> 3
        key.length >= 12 -> 2
        key.length >= 8 -> 1
        else -> 0
    }

    private fun categories(key: String) = listOf(
        key.any { it.isLowerCase() },
        key.any { it.isUpperCase() },
        key.any { it.isDigit() },
        key.any { !it.isLetterOrDigit() }
    ).count { it }

    /**
     * 0 Madera … 3 Diamante. Ejemplos: "LunaSol" Madera, "Luna2026" Piedra,
     * "Luna2026*Sol" Hierro, "GatoAzulSalta2026!" Diamante.
     */
    fun level(key: String): Int {
        if (key.isEmpty()) return 0
        val score = lengthPoints(key) + (categories(key) - 1).coerceAtLeast(0)
        return when {
            score <= 1 -> 0
            score <= 3 -> 1
            score <= 5 -> 2
            else -> 3
        }
    }

    /** Consejos para endurecer la llave, del más importante al menos. */
    fun tips(key: String): List<String> = buildList {
        if (key.length < 16) add("Hazla más larga: suma otra palabra 📏")
        if (key.none { it.isDigit() }) add("Agrega un número 🔢")
        if (key.all { it.isLetterOrDigit() }) add("Agrega un símbolo como ! o # ✳️")
        if (key.none { it.isUpperCase() } || key.none { it.isLowerCase() }) add("Mezcla mayúsculas y minúsculas 🔠")
    }
}
