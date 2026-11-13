package com.example.cybertura.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.game.engine.PlayerCharacter
import com.example.cybertura.game.engine.PlayerCharacters
import com.example.cybertura.game.renderer.GameSprites
import com.example.cybertura.ui.theme.DungeonBlack
import com.example.cybertura.ui.theme.DungeonBorder
import com.example.cybertura.ui.theme.DungeonCard
import com.example.cybertura.ui.theme.DungeonDark
import com.example.cybertura.ui.theme.NeonCyan
import com.example.cybertura.ui.theme.NeonGold
import com.example.cybertura.ui.theme.TextMuted
import com.example.cybertura.ui.theme.TextPrimary
import com.example.cybertura.ui.theme.TextSecondary

/**
 * Último paso antes de entrar al mundo: el jugador elige un alias y su personaje, para que la
 * aventura sea suya. El alias se queda en el teléfono y no aparece en los datos del piloto.
 */
@Composable
fun ProfileSetupScreen(onDone: (name: String, characterId: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var characterId by rememberSaveable { mutableStateOf(PlayerCharacters.default.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DungeonDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(text = "🕵️", fontSize = 56.sp)
        Text(
            text = "Crea tu agente",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        // ── Alias ──
        Text(
            text = "1. ¿CÓMO TE LLAMARÁN EN CYBERTURA?",
            color = NeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = PlayerCharacters.sanitizeName(it) },
            singleLine = true,
            placeholder = { Text("Tu alias (ej. Luna, Rayo7)", color = TextMuted) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DungeonBorder,
                cursorColor = NeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Surface(
            color = NeonGold.copy(alpha = 0.10f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.4f))
        ) {
            Text(
                text = "🛡️ Primer consejo de agente: usa un apodo inventado, nunca tu nombre completo. " +
                    "En internet tu nombre real es un dato privado.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(12.dp)
            )
        }

        // ── Personaje ──
        Text(
            text = "2. ELIGE TU PERSONAJE",
            color = NeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayerCharacters.all.forEach { character ->
                CharacterCard(
                    character = character,
                    selected = character.id == characterId,
                    onClick = { characterId = character.id },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Button(
            onClick = { onDone(PlayerCharacters.finalName(name), characterId) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("¡A la aventura! 🚀", color = DungeonBlack, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        if (name.isEmpty()) {
            Text(
                text = "Si no escribes un alias, serás «${PlayerCharacters.DEFAULT_NAME}».",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CharacterCard(
    character: PlayerCharacter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sprite = remember(character.id) { GameSprites.get(context.resources).playerFor(character.id) }
    Surface(
        onClick = onClick,
        color = if (selected) NeonCyan.copy(alpha = 0.15f) else DungeonCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.5.dp else 1.dp,
            if (selected) NeonCyan else DungeonBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pixel art: se amplía sin suavizar para que no se vea borroso
            Image(
                bitmap = sprite,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = character.name,
                color = if (selected) NeonCyan else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(text = if (selected) "✓ Elegido" else " ", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}
