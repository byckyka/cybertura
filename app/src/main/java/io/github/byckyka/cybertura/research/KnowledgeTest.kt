package io.github.byckyka.cybertura.research

/** Pregunta del cuestionario de conocimientos; [correct] es la posición (desde 1) de la opción correcta. */
data class KnowledgeQuestion(val text: String, val options: List<String>, val correct: Int)

/** Momento en que se aplica el cuestionario dentro de la sesión de un participante. */
enum class TestMoment(val code: String, val label: String) {
    /** Antes de jugar, justo después de crear el perfil. */
    PRE("antes", "Antes de jugar"),
    /** Después de jugar, al terminar la aventura o cuando el investigador lo abre. */
    POST("despues", "Después de jugar");

    companion object {
        fun fromCode(code: String?): TestMoment? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Cuestionario de conocimientos del piloto: las mismas preguntas antes y después de jugar, para
 * comparar lo que el niño sabía con lo que aprendió.
 *
 * Hay una pregunta por módulo, con situaciones distintas a las del juego. No muestra si la
 * respuesta fue correcta (antes de jugar eso ya enseñaría) y ofrece siempre "No lo sé", para
 * que nadie tenga que adivinar.
 */
object KnowledgeTest {
    const val NO_ANSWER = 0
    const val DONT_KNOW_LABEL = "No lo sé"

    val QUESTIONS = listOf(
        KnowledgeQuestion(
            "En el grupo del curso alguien publica una foto para burlarse de un compañero. ¿Qué es lo que más ayuda?",
            listOf(
                "Reírme también, para no quedar mal con el grupo",
                "No hacer nada: no es mi problema",
                "No compartirla, apoyar a mi compañero y avisar a un adulto"
            ),
            correct = 3
        ),
        KnowledgeQuestion(
            "¿Cuál de estos datos NO deberías poner en un perfil que cualquiera puede ver?",
            listOf("Mi color favorito", "La dirección de mi casa", "Mi deporte favorito"),
            correct = 2
        ),
        KnowledgeQuestion(
            "¿Cuál de estas contraseñas es la más segura?",
            listOf(
                "Mi nombre y mi año de nacimiento",
                "123456",
                "Una frase larga que mezcla letras, números y símbolos"
            ),
            correct = 3
        ),
        KnowledgeQuestion(
            "En un juego en línea, alguien que no conoces te ofrece monedas gratis si le das tu contraseña. ¿Qué haces?",
            listOf(
                "No se la doy, lo bloqueo y se lo cuento a un adulto",
                "Se la doy y después la cambio",
                "Le pido que primero me mande las monedas"
            ),
            correct = 1
        ),
        KnowledgeQuestion(
            "Estás en un lugar con Wi-Fi gratis y sin clave. ¿Qué es lo más seguro?",
            listOf(
                "Aprovechar para entrar a todas mis cuentas",
                "No entrar a mis cuentas ahí y preguntarle antes a un adulto",
                "Pasarles la red a mis amigos"
            ),
            correct = 2
        ),
        KnowledgeQuestion(
            "¿Desde dónde es más seguro descargar un juego?",
            listOf(
                "Desde un enlace que me mandó alguien que no conozco",
                "Desde un anuncio que dice que es gratis",
                "Desde la tienda oficial y con permiso de un adulto"
            ),
            correct = 3
        ),
        KnowledgeQuestion(
            "Te llega este mensaje: «¡Ganaste un premio! Entra ya a este enlace y escribe tus datos». ¿Qué es lo más probable?",
            listOf(
                "Que sea una trampa para quedarse con mis datos",
                "Que de verdad me haya ganado un premio",
                "Que me lo haya mandado un amigo"
            ),
            correct = 1
        ),
        KnowledgeQuestion(
            "Publicas una foto y después la borras. ¿Qué puede pasar?",
            listOf(
                "Desaparece de internet para siempre",
                "Alguien pudo guardarla antes y seguir compartiéndola",
                "Queda guardada, pero solo la ven mis amigos"
            ),
            correct = 2
        )
    )

    /** Valor que se guarda cuando el niño elige "No lo sé" en la pregunta [index]. */
    fun dontKnow(index: Int): Int = QUESTIONS[index].options.size + 1

    /** Deja una respuesta por pregunta: una opción, "No lo sé" o sin respuesta. */
    fun normalize(answers: List<Int>): List<Int> = QUESTIONS.indices.map { i ->
        answers.getOrNull(i)?.takeIf { it in 1..dontKnow(i) } ?: NO_ANSWER
    }

    fun isCorrect(index: Int, answer: Int): Boolean = answer == QUESTIONS[index].correct

    /** Cantidad de respuestas correctas. "No lo sé" y las preguntas en blanco no suman. */
    fun score(answers: List<Int>): Int = normalize(answers).withIndex().count { (i, a) -> isCorrect(i, a) }
}

/** Respuestas de un participante en un momento (antes o después), tal como se guardan en el teléfono. */
data class KnowledgeRow(
    val sessionId: String,
    val participant: String,
    val sessionStartMs: Long,
    val moment: TestMoment,
    val answers: List<Int>
) {
    fun toCsvLine(): String = ResearchCsv.line(
        listOf(sessionId, participant, sessionStartMs.toString(), moment.code) + answers.map { it.toString() }
    )

    companion object {
        fun parse(line: String): KnowledgeRow? {
            val c = ResearchCsv.parseLine(line)
            if (c.size < 4) return null
            val moment = TestMoment.fromCode(c[3]) ?: return null
            return runCatching {
                KnowledgeRow(c[0], c[1], c[2].toLong(), moment, KnowledgeTest.normalize(c.drop(4).map { it.toInt() }))
            }.getOrNull()
        }
    }
}
