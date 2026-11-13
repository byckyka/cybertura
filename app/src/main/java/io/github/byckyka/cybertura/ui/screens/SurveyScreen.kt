package io.github.byckyka.cybertura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.byckyka.cybertura.research.ResearchSurvey
import io.github.byckyka.cybertura.ui.theme.DungeonBlack
import io.github.byckyka.cybertura.ui.theme.DungeonBorder
import io.github.byckyka.cybertura.ui.theme.DungeonCard
import io.github.byckyka.cybertura.ui.theme.DungeonDark
import io.github.byckyka.cybertura.ui.theme.NeonCyan
import io.github.byckyka.cybertura.ui.theme.NeonGold
import io.github.byckyka.cybertura.ui.theme.TextMuted
import io.github.byckyka.cybertura.ui.theme.TextPrimary
import io.github.byckyka.cybertura.ui.theme.TextSecondary

/**
 * Encuesta de percepción del piloto. Solo aparece con una sesión de investigación abierta.
 * Ninguna respuesta viene marcada y se puede enviar con preguntas en blanco.
 */
@Composable
fun SurveyScreen(onSubmit: (answers: List<Int>) -> Unit) {
    val answers = remember {
        mutableStateListOf<Int>().apply { repeat(ResearchSurvey.QUESTIONS.size) { add(ResearchSurvey.NO_ANSWER) } }
    }
    val missing = answers.count { it == ResearchSurvey.NO_ANSWER }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DungeonDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "💬 ¡Cuéntanos qué te pareció!",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Toca la carita que mejor diga lo que piensas. No hay respuestas buenas ni malas.",
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )

        ResearchSurvey.QUESTIONS.forEachIndexed { index, question ->
            Surface(
                color = DungeonCard,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${index + 1}. $question",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FaceScale(selected = answers[index], onSelect = { answers[index] = it })
                }
            }
        }

        Button(
            onClick = { onSubmit(answers.toList()) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Enviar mis respuestas", color = DungeonBlack, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        if (missing > 0) {
            Text(
                text = if (missing == 1) "Te falta 1 pregunta. Puedes enviarla así si no quieres responderla."
                else "Te faltan $missing preguntas. Puedes enviar así si no quieres responderlas.",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Cinco caritas, de "nada" a "muchísimo". Tocar la elegida otra vez la desmarca. */
@Composable
private fun FaceScale(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ResearchSurvey.FACES.forEachIndexed { i, face ->
                val value = i + 1
                val isSelected = selected == value
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (isSelected) NeonGold.copy(alpha = 0.25f) else DungeonBlack, CircleShape)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) NeonGold else DungeonBorder, CircleShape)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(if (isSelected) ResearchSurvey.NO_ANSWER else value) }
                        )
                        .semantics { contentDescription = "$value de ${ResearchSurvey.MAX_ANSWER}" },
                    contentAlignment = Alignment.Center
                ) {
                    // Sin elegir todas se ven igual de apagadas: ninguna parece la "sugerida"
                    Text(
                        text = face,
                        fontSize = 28.sp,
                        modifier = Modifier.alpha(if (selected == ResearchSurvey.NO_ANSWER || isSelected) 1f else 0.45f)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Nada", color = TextMuted, fontSize = 12.sp)
            Text("Muchísimo", color = TextMuted, fontSize = 12.sp)
        }
    }
}
