package com.example.cybertura.game.engine

/**
 * Datos estáticos de los NPCs que habitan la Ciudad CyberTura.
 */
data class NpcData(
    val id: String,
    val name: String,
    val title: String,
    val moduleId: String?,   // Si es null, es un Aldeano (solo habla)
    val emoji: String,
    val tileX: Float,
    val tileY: Float,
    val tutorialDialog: String = "",
    val greetingDialog: String,
    val completedDialog: String = "",
    /** Orden en la cadena de misiones principales (null = aldeano, sin misión) */
    val questOrder: Int? = null,
    /** Mapa donde vive el NPC (calle o el interior de una casa) */
    val mapId: String = MapIds.EXTERIOR,
    /** Pista corta de dónde encontrarlo (se muestra en el objetivo actual) */
    val locationHint: String = "",
    /** Radio máximo (en tiles) que se aleja de su punto de origen al pasear */
    val wanderRadius: Float = 4f
)

object NpcRegistry {

    /**
     * Orden canónico de la misión principal. Intercala los tipos de reto para dar
     * variedad (preguntas → deslizar → preguntas → detective → …):
     * 1. Líder Heart       → Ciberacoso / Empatía  (plaza central)                  · chat de grupo
     * 2. Escudera Shield   → Privacidad            (frente a la Casa Azul norte)    · deslizar
     * 3. Guardián Key      → Contraseñas           (frente a la Casa Roja norte)    · forja de llaves
     * 4. Capitán Joystick  → Juegos en línea       (DENTRO de la Casa Azul del sur) · detective
     * 5. Técnica Wifi      → Wi-Fi y dispositivos  (DENTRO de la Casa Roja del sur) · chat de grupo
     * 6. Druida Raíz       → Descargas y virus     (Bosque Escondido, tras la cueva) · defiende el bosque
     * 7. Capitán Anzuelo   → Phishing              (muelle)                         · deslizar
     * 8. Maestro Cyber     → Redes Sociales        (academia)                       · detective
     */
    val npcs = listOf(

        // --- MAESTROS DE MÓDULOS (en orden de progresión) ---

        NpcData(
            id = "npc_bullying",
            name = "Líder Heart",
            title = "Defensora de la Empatía",
            moduleId = "module_cyberbullying",
            emoji = "❤️",
            tileX = 25f,
            tileY = 28f, // Plaza central — primer encuentro
            questOrder = 1,
            locationHint = "en la plaza central, donde empiezas",
            tutorialDialog = "Soy Líder Heart. Mi especialidad es la convivencia digital y frenar el ciberacoso. ¡Yo seré tu primera maestra en este viaje!",
            greetingDialog = "¡Hola, aventurero! Bienvenido a CyberTura. El ciberacoso es un problema real que lastima a muchas personas. ¡Únete a mí para aprender a combatirlo con respeto!",
            completedDialog = "¡Qué gran trabajo en mi misión! Has demostrado tener un corazón enorme. Ve con Escudera Shield, al norte-este, y sigue brillando."
        ),
        NpcData(
            id = "npc_passwords",
            name = "Guardián Key",
            title = "Maestro de Contraseñas",
            moduleId = "module_passwords",
            emoji = "🔑",
            tileX = 14f,
            tileY = 22f, // Camino que sube a la Casa Roja del norte
            questOrder = 3,
            locationHint = "al norte-oeste, en el sendero frente a la Casa Roja",
            tutorialDialog = "Soy Guardián Key. Conmigo aprenderás a crear contraseñas fuertes como fortalezas. Me encontrarás después de superar a Escudera Shield.",
            greetingDialog = "¡Alto ahí, cadete! Escudera Shield te enseñó a cuidar tus datos. Ahora toca proteger tus accesos. ¿Listo para forjar una llave inquebrantable?",
            completedDialog = "Tus cuentas están ahora completamente blindadas. ¡No olvides usar claves largas y variadas! El Capitán Joystick te espera en su cuarto gamer, dentro de la Casa Azul del sur."
        ),
        NpcData(
            id = "npc_wifi",
            name = "Técnica Wifi",
            title = "Guardiana del Hogar Digital",
            moduleId = "module_wifi",
            emoji = "📶",
            tileX = 5f,
            tileY = 4f, // Dentro del taller (Casa Roja del sur)
            mapId = MapIds.HOUSE_RED_S,
            wanderRadius = 1.2f,
            questOrder = 5,
            locationHint = "dentro de la Casa Roja del sur (la de la puerta marrón al sur de la ciudad)",
            tutorialDialog = "Soy Técnica Wifi y vivo en la Casa Roja del sur. Entra a mi taller para aprender a cuidar el Wi-Fi, el celular y la tablet de tu hogar.",
            greetingDialog = "¡Pasa, pasa, no te quedes en la puerta! Este taller está lleno de routers y tablets. Ya sabes cerrar tus cuentas y jugar seguro; ahora te enseñaré a cuidar las puertas digitales de tu casa.",
            completedDialog = "¡Tu casa digital quedó con todas las ventanas cerradas! Ahora busca la cueva del sur-oeste: tras su puerta morada está el Bosque Escondido de la Druida Raíz."
        ),
        NpcData(
            id = "npc_forest",
            name = "Druida Raíz",
            title = "Guardiana del Bosque Antivirus",
            moduleId = "module_malware",
            emoji = "🌳",
            tileX = 5f,
            tileY = 3f, // En el claro del Bosque Escondido
            mapId = MapIds.FOREST,
            wanderRadius = 1.5f,
            questOrder = 6,
            locationHint = "en el Bosque Escondido (entra por la puerta morada de la cueva, al sur-oeste)",
            tutorialDialog = "Soy la Druida Raíz y cuido el Bosque Escondido, detrás de la cueva. Conmigo aprenderás a descargar con seguridad y a mantener lejos a los virus.",
            greetingDialog = "Shhh… escucha el bosque. Ya sabes cuidar las puertas de tu casa; ahora te enseñaré a cuidar lo que dejas entrar. Los virus llegan disfrazados de juegos gratis, premios y archivos raros. ¿Me ayudas a defender el bosque?",
            completedDialog = "¡El bosque está a salvo gracias a ti! Ningún virus pasará. Tu siguiente maestro es el Capitán Anzuelo: está muy cerca, en el muelle de madera al oeste."
        ),
        NpcData(
            id = "npc_privacy",
            name = "Escudera Shield",
            title = "Protectora de Privacidad",
            moduleId = "module_privacy",
            emoji = "🛡️",
            tileX = 34f,
            tileY = 22f, // Camino que sube a la Casa Azul del norte
            questOrder = 2,
            locationHint = "al norte-este, en el sendero frente a la Casa Azul",
            tutorialDialog = "Soy la Escudera Shield. Mi misión es enseñarte a proteger tus datos y tu identidad para que navegues siempre con seguridad.",
            greetingDialog = "Saludos, agente. Líder Heart me contó que tienes un gran corazón. Ahora te mostraré algo muy valioso: tu información personal. ¿Quieres aprender a usar el escudo de la privacidad?",
            completedDialog = "¡Tu privacidad es ahora un escudo de titanio! Tu siguiente maestro es Guardián Key, en el sendero frente a la Casa Roja del norte."
        ),
        NpcData(
            id = "npc_phishing",
            name = "Capitán Anzuelo",
            title = "Cazador de Phishing",
            moduleId = "module_phishing",
            emoji = "🎣",
            tileX = 9f,
            tileY = 26f, // En el muelle de madera
            questOrder = 7,
            locationHint = "al oeste, en el muelle de madera junto al mar",
            tutorialDialog = "¡Ahoy! Soy el Capitán Anzuelo. Conmigo descubrirás las peores trampas y estafas de internet, y aprenderás a no morder el cebo.",
            greetingDialog = "¡Ahoy, marinero digital! Hay piratas en este mar de datos que quieren engañarte. Ya conoces de claves, dispositivos y privacidad... ahora aprende a detectar los anzuelos antes de morder el cebo.",
            completedDialog = "¡Arrr! Ningún pirata engañoso podrá robar tus datos ahora. Ve a la gran Academia del norte: el Maestro Cyber te espera para la prueba final."
        ),
        NpcData(
            id = "npc_gaming",
            name = "Capitán Joystick",
            title = "Protector de Juegos en Línea",
            moduleId = "module_gaming",
            emoji = "🎮",
            tileX = 5f,
            tileY = 4f, // Dentro del cuarto gamer (Casa Azul del sur)
            mapId = MapIds.HOUSE_BLUE_S,
            wanderRadius = 1.2f,
            questOrder = 4,
            locationHint = "dentro de la Casa Azul del sur (cerca del cementerio, al sur-este)",
            tutorialDialog = "Soy el Capitán Joystick y mi cuarto gamer está en la Casa Azul del sur. Allí aprenderás a jugar en línea sin caer en trampas ni hablar con extraños.",
            greetingDialog = "¡Player uno detectado! Bienvenido a mi cuarto gamer. Guardián Key te enseñó a proteger tus claves; ahora te enseñaré a defender tu cuenta y tus datos mientras juegas con gente de todo el mundo.",
            completedDialog = "¡Nivel superado, jugador inteligente! Ahora visita a Técnica Wifi en su taller, dentro de la Casa Roja del sur."
        ),
        NpcData(
            id = "npc_social",
            name = "Maestro Cyber",
            title = "Guía de Redes Sociales",
            moduleId = "module_social_networks",
            emoji = "🌐",
            tileX = 25f,
            tileY = 16f, // Frente a la gran Academia del Norte
            questOrder = 8,
            locationHint = "al norte, frente a la gran Academia",
            tutorialDialog = "Soy el Maestro Cyber. Yo seré tu prueba final en la gran Academia, donde aprenderás a dejar una huella digital positiva.",
            greetingDialog = "Bienvenido a la Academia, Agente Élite. Has superado siete grandes pruebas, en las calles, dentro de las casas y hasta en el bosque. Solo queda una: las redes sociales. Lo que publicas deja una huella permanente. Domina este último reto y conviértete en Campeón Legendario.",
            completedDialog = "¡Eres un verdadero Campeón Legendario de CyberTura! Tu huella digital es un ejemplo a seguir. Estoy inmensamente orgulloso de tu progreso."
        ),

        // --- ALDEANOS DE LA CALLE (Ambientación) ---

        NpcData(
            id = "npc_villager_1",
            name = "Turista Tech",
            title = "Explorador",
            moduleId = null,
            emoji = "🎒",
            tileX = 20f,
            tileY = 38f,
            greetingDialog = "¡Qué bonita ciudad! Dicen que si hablas con los 8 Maestros en el orden correcto te conviertes en un Campeón Legendario. Y ojo: ¡dos te esperan dentro de sus casas y una en un bosque escondido tras la cueva! Busca el signo ❗ dorado sobre la puerta."
        ),
        NpcData(
            id = "npc_villager_2",
            name = "Pescador Local",
            title = "Aldeano",
            moduleId = null,
            emoji = "👨‍🌾",
            tileX = 18f,
            tileY = 30f, // Cerca del lago interior
            greetingDialog = "El agua está muy tranquila hoy. Recuerda que no todo lo que brilla en internet es oro... a veces es un anzuelo del Capitán Anzuelo. ¡Habla con él en el muelle!"
        ),
        NpcData(
            id = "npc_villager_3",
            name = "Niño Corredor",
            title = "Aldeano",
            moduleId = null,
            emoji = "👦",
            tileX = 30f,
            tileY = 25f,
            greetingDialog = "¡Oye! Las casas de la ciudad se pueden visitar: solo camina hasta la puerta. Si ves un ❗ dorado flotando sobre una puerta, ¡ahí te espera un maestro!"
        ),

        // --- HABITANTES DE LAS CASAS (Ambientación interior) ---

        NpcData(
            id = "npc_abuela",
            name = "Abuela Teje",
            title = "Vecina de la Casa Roja",
            moduleId = null,
            emoji = "🧶",
            tileX = 7f,
            tileY = 2f,
            mapId = MapIds.HOUSE_RED_N,
            wanderRadius = 1.2f,
            greetingDialog = "Bienvenido, mi cielo. Estoy tejiendo una bufanda mientras hablo con mi nieto por videollamada. ¡Ah! Y siempre le digo: la clave del correo no se le presta ni a la abuela."
        ),
        NpcData(
            id = "npc_tio_bits",
            name = "Tío Bits",
            title = "Vecino de la Casa Azul",
            moduleId = null,
            emoji = "💾",
            tileX = 4f,
            tileY = 4f,
            mapId = MapIds.HOUSE_BLUE_N,
            wanderRadius = 1.2f,
            greetingDialog = "Hola, joven. Guardo copias de las fotos de la familia en dos lugares distintos, por si se daña la computadora. Si algún día pierdes algo importante, ¡cuéntaselo a un adulto!"
        ),
        NpcData(
            id = "npc_mama_cora",
            name = "Mamá Cora",
            title = "Dueña del Taller",
            moduleId = null,
            emoji = "☕",
            tileX = 3f,
            tileY = 6f,
            mapId = MapIds.HOUSE_RED_S,
            wanderRadius = 1.0f,
            greetingDialog = "Preparé chocolate caliente para todos los visitantes. Mi hija Técnica Wifi pasa el día entre cables, pero siempre me recuerda apagar la tablet un rato para descansar los ojos."
        ),
        NpcData(
            id = "npc_hermana_luna",
            name = "Hermana Luna",
            title = "Gamer Novata",
            moduleId = null,
            emoji = "🌙",
            tileX = 3f,
            tileY = 4f,
            mapId = MapIds.HOUSE_BLUE_S,
            wanderRadius = 1.0f,
            greetingDialog = "Mi hermano el Capitán Joystick dice que un buen jugador sabe cuándo pausar, pedir ayuda y no hablar con extraños. ¡Hasta los campeones descansan entre partidas!"
        )
    )

    fun getNpcById(id: String): NpcData? = npcs.find { it.id == id }

    /** NPCs que viven en el mapa indicado */
    fun npcsInMap(mapId: String): List<NpcData> = npcs.filter { it.mapId == mapId }

    /** Lista de NPCs de misión ordenados por questOrder */
    val questNpcs: List<NpcData>
        get() = npcs.filter { it.questOrder != null }.sortedBy { it.questOrder }

    /**
     * Devuelve el ID del siguiente NPC de misión que el jugador debe visitar,
     * dado el conjunto de módulos ya completados.
     * Retorna null si ya completó todos los módulos.
     */
    fun getNextQuestNpcId(completedModuleIds: Set<String>): String? {
        return questNpcs.firstOrNull { npc ->
            npc.moduleId != null && !completedModuleIds.contains(npc.moduleId)
        }?.id
    }
}
