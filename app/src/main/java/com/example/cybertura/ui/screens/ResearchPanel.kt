package com.example.cybertura.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.research.ResearchLogger
import com.example.cybertura.research.ResearchStatus
import com.example.cybertura.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════
// MODO INVESTIGADOR — se abre tocando 5 veces seguidas el título de la
// Sala de Trofeos y un PIN. Es para el adulto que dirige el piloto, no para el niño.
// ═══════════════════════════════════════════════════════════════

@Composable
fun ResearchPinDialog(onDismiss: () -> Unit, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔬 Modo investigador", fontWeight = FontWeight.Black, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Escribe el PIN para continuar.", color = TextSecondary)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); wrong = false },
                    singleLine = true,
                    isError = wrong,
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = { if (wrong) Text("PIN incorrecto") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (pin == ResearchLogger.RESEARCHER_PIN) onUnlocked() else wrong = true
            }) { Text("Entrar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) } },
        containerColor = DungeonCard,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ResearchPanelDialog(
    status: ResearchStatus,
    onDismiss: () -> Unit,
    /** Abre una sesión con el código y reinicia la aventura para el nuevo participante. */
    onStartSession: (code: String) -> Unit,
    onEndSession: () -> Unit,
    onExportToFolder: () -> Unit,
    onShare: () -> Unit,
    onClearData: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var confirmStart by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val cleanCode = ResearchLogger.sanitizeCode(code)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔬 Modo investigador", fontWeight = FontWeight.Black, color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Estado actual
                Surface(color = DungeonBlack.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (status.isRecording) {
                            Text("● Registrando: ${status.participant}", color = NeonGreen, fontWeight = FontWeight.Black)
                            Text(
                                "Desde las ${TIME.format(Date(status.sessionStartMs))} · " +
                                    "${status.attemptsInSession} intento(s) registrados",
                                color = TextSecondary, fontSize = 13.sp
                            )
                        } else {
                            Text("○ Sin sesión activa (no se registra nada)", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Text("Participantes con datos guardados: ${status.savedSessions}", color = TextMuted, fontSize = 12.sp)
                    }
                }

                if (status.isRecording) {
                    Button(
                        onClick = onEndSession,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                    ) { Text("Terminar sesión de ${status.participant}", color = DungeonBlack, fontWeight = FontWeight.Bold) }
                }

                // Nueva sesión
                HorizontalDivider(color = DungeonBorder)
                Text("Nueva sesión", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(12); confirmStart = false },
                    singleLine = true,
                    label = { Text("Código del participante") },
                    placeholder = { Text("Ej: P01") },
                    supportingText = { Text("Nunca escribas el nombre del niño.") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!confirmStart) {
                    Button(
                        onClick = { confirmStart = true },
                        enabled = cleanCode.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Iniciar sesión", fontWeight = FontWeight.Bold) }
                } else {
                    Text(
                        (if (status.isRecording) "Se cerrará la sesión de ${status.participant}. " else "") +
                            "La aventura se reiniciará desde el tutorial para $cleanCode.",
                        color = NeonAmber, fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmStart = false }) { Text("Cancelar", color = TextSecondary) }
                        Button(onClick = { onStartSession(cleanCode) }) {
                            Text("Reiniciar e iniciar", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Exportar
                HorizontalDivider(color = DungeonBorder)
                Text("Exportar resultados (un CSV para Excel)", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExportToFolder, modifier = Modifier.weight(1f)) { Text("Guardar en carpeta") }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text("Compartir") }
                }

                // Borrar
                HorizontalDivider(color = DungeonBorder)
                if (!confirmClear) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Borrar todos los datos del piloto", color = NeonPink)
                    }
                } else {
                    Text("Se borrarán todas las sesiones de este teléfono. Expórtalas antes.", color = NeonPink, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmClear = false }) { Text("Cancelar", color = TextSecondary) }
                        Button(
                            onClick = { confirmClear = false; onClearData() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) { Text("Borrar", color = DungeonBlack, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = TextSecondary) } },
        containerColor = DungeonCard,
        shape = RoundedCornerShape(24.dp)
    )
}

private val TIME = SimpleDateFormat("HH:mm", Locale.ROOT)
