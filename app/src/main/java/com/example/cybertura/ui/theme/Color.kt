package com.example.cybertura.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// PALETA MMORPG CYBERTURA - Fantasía Cibernética para Niños
// Estilo: Sci-Fi RPG con colores vibrantes y sin tonos alarmistas
// ═══════════════════════════════════════════════════════════════

// ─── Fondos del "Mundo Digital" ────────────────────────────────
val DungeonBlack = Color(0xFF080C18)        // Fondo más profundo (void)
val DungeonDark = Color(0xFF0D1225)         // Fondo principal oscuro
val DungeonCard = Color(0xFF141B33)         // Tarjetas principales
val DungeonCardHover = Color(0xFF1A2342)    // Tarjetas hover / elevadas
val DungeonBorder = Color(0xFF2A3355)       // Bordes sutiles

// ─── Colores de Rareza (RPG Loot System) ───────────────────────
val RarityCommon = Color(0xFF9CA3AF)        // Gris - Común
val RarityUncommon = Color(0xFF22D3EE)      // Cyan - Poco Común
val RarityRare = Color(0xFF6366F1)          // Índigo - Raro
val RarityEpic = Color(0xFFA855F7)          // Púrpura - Épico
val RarityLegendary = Color(0xFFFFB703)     // Dorado - Legendario

// ─── Colores primarios de acento ───────────────────────────────
val NeonCyan = Color(0xFF00F5D4)            // Acento principal / XP
val NeonBlue = Color(0xFF00B4D8)            // Acento secundario
val NeonPurple = Color(0xFFA855F7)          // Aura mágica
val NeonPink = Color(0xFFFF6B9D)            // Coral / Afecto
val NeonGold = Color(0xFFFFD700)            // Oro / Recompensas
val NeonAmber = Color(0xFFFFB703)           // Monedas / XP ganada
val NeonGreen = Color(0xFF34D399)           // Éxito / Salud

// ─── Colores de módulos (matching JSON content) ────────────────
val ModulePasswords = Color(0xFF00D2B4)     // Teal ciber
val ModulePhishing = Color(0xFFFFB800)      // Ámbar alerta
val ModulePrivacy = Color(0xFF8B5CF6)       // Violeta escudo
val ModuleBullying = Color(0xFFFF6B8B)      // Rosa empatía
val ModuleSocial = Color(0xFF00C2FF)        // Azul digital

// ─── Textos ────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val TextGlow = Color(0xFFE0F2FE)

// ─── Estados de feedback suaves (sin ansiedad) ─────────────────
val QuestSuccess = Color(0xFF34D399)        // ¡Victoria!
val QuestRetry = Color(0xFFFBBF24)          // Naranja cálido para reintentos
val QuestXpGlow = Color(0xFF22D3EE)         // Cyan glow al ganar XP
val ShieldGlow = Color(0xFF6366F1)          // Escudo protector

// ─── Legacy aliases (compatibilidad con código existente) ──────
val CyberTeal = NeonCyan
val CyberCyan = NeonBlue
val CyberNavyDark = DungeonDark
val CyberNavyCard = DungeonCard
val CyberNavyCardLight = DungeonCardHover
val CyberYellow = NeonGold
val CyberPurple = NeonPurple
val CyberCoral = NeonPink
val CyberGreen = NeonGreen
val CyberSubtleText = TextMuted
val GentleSuccess = QuestSuccess
val GentleRetry = QuestRetry
val SoftBackgroundGlow = Color(0xFF0E1830)
val CyberLightBg = Color(0xFFF8FAFC)
val CyberLightCard = Color(0xFFFFFFFF)
val CyberLightText = Color(0xFF0F172A)