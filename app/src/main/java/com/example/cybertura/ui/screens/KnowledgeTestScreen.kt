package com.example.cybertura.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cybertura.research.KnowledgeTest
import com.example.cybertura.research.TestMoment
import com.example.cybertura.ui.components.NarrateButton
import com.example.cybertura.ui.theme.DungeonBlack
import com.example.cybertura.ui.theme.DungeonBorder
import com.example.cybertura.ui.theme.DungeonCard
import com.example.cybertura.ui.theme.DungeonDark
import com.example.cybertura.ui.theme.NeonCyan
import com.example.cybertura.ui.theme.TextMuted
import com.example.cybertura.ui.theme.TextPrimary
import com.example.cybertura.ui.theme.TextSecondary

/**
 * Cuestionario de conocimientos del piloto, una pregunta por pantalla. Es el mismo antes y
 * después de jugar. No dice si la respuesta fue correcta y siempre ofrece "No lo sé".
 */
@Composable
fun KnowledgeTestScreen(moment: TestMoment, onSubmit: (answers: List<Int>) -> Unit) {
    val questions = KnowledgeTest.QUESTIONS
    val answers = remember {
        mutableStateListOf<Int>().apply { repeat(questions.size) { add(KnowledgeTest.NO_ANSWER) } }
    }
    var index by remember { mutableIntStateOf(0) }
    val question = questions[index]
    val isLast = index == questions.lastIndex

    // "Atrás" vuelve a la pregunta anterior; no se sale del cuestionario a medias
    BackHandler { if (index > 0) index -= 1 }

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
            text = if (moment == TestMoment.PRE) "🧠 Antes de empezar" else "🧠 Para terminar",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Responde lo que tú creas. No es un examen y no afecta tu aventura. " +
                "Si no sabes una respuesta, toca «${KnowledgeTest.DONT_KNOW_LABEL}».",
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(questions.size) { i ->
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (i == index) 28.dp else 12.dp)
                        .background(if (i <= index) NeonCyan else DungeonBorder, RoundedCornerShape(3.dp))
                )
            }
        }

        Surface(
            color = DungeonCard,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PREGUNTA ${index + 1} DE ${questions.size}",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = question.text,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                NarrateButton(
                    text = buildString {
                        append(question.text)
                        question.options.forEachIndexed { i, option -> append(" Opción ${i + 1}: $option.") }
                    }
                )
            }
        }

        val choices = question.options + KnowledgeTest.DONT_KNOW_LABEL
        choices.forEachIndexed { i, text ->
            val value = i + 1
            val selected = answers[index] == value
            val isDontKnow = value == KnowledgeTest.dontKnow(index)
            Surface(
                onClick = { answers[index] = value },
                color = if (selected) NeonCyan.copy(alpha = 0.15f) else DungeonCard,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    if (selected) 2.5.dp else 1.dp,
                    if (selected) NeonCyan else DungeonBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = (if (selected) "● " else "○ ") + text,
                    color = if (isDontKnow && !selected) TextMuted else TextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            if (index > 0) {
                TextButton(onClick = { index -= 1 }) {
                    Text("← Anterior", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { if (isLast) onSubmit(answers.toList()) else index += 1 },
                // Hay que elegir algo para seguir; "No lo sé" también vale
                enabled = answers[index] != KnowledgeTest.NO_ANSWER,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Text(
                    text = if (isLast) "Terminar" else "Siguiente →",
                    color = DungeonBlack,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
