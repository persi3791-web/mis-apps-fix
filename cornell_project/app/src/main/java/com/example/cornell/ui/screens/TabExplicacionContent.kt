package com.example.cornell.ui.screens

import android.Manifest
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.cornell.ui.voice.VoskSpeechRecognizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cornell.ui.theme.CornellPrimary
import com.example.cornell.ui.theme.CornellText
import com.example.cornell.ui.theme.CornellTextSecondary
import com.example.cornell.ui.theme.QuizWrong

// Colores del círculo de score (spec: rojo → naranja → amarillo → verde)
private val ScoreRed = Color(0xFFED4444)
private val ScoreOrange = Color(0xFFFA9A1A)
private val ScoreYellow = Color(0xFFEBB61A)
private val ScoreGreen = Color(0xFF21C451)
private val ScoreGray = Color(0xFF595959)
private val FeedbackGreenBg = Color(0xFF174D1C)
private val FeedbackRedBg = Color(0xFF381717)
private val FeedbackYellowBg = Color(0xFF2E260F)

@Composable
fun TabExplicacionContent(
    explanationText: String,
    explanationPartial: String,
    onExplanationChange: (String) -> Unit,
    score: Int,
    scoreLabelText: String,
    feedbackFound: String,
    feedbackMissing: String,
    feedbackTip: String,
    isEvaluating: Boolean,
    voiceStatus: String,
    onVoiceStatusChange: (String) -> Unit,
    micLevel: Float,
    onMicLevelChange: (Float) -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit,
    onVoicePartial: (String) -> Unit,
    onVoiceResult: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(false) }
    var voiceCancelled by remember { mutableStateOf(false) }
    var voskRef by remember { mutableStateOf<VoskSpeechRecognizer?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    fun runOnMain(block: () -> Unit) = mainHandler.post(block)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onVoiceStatusChange("❌ Permiso de micrófono denegado")
            return@rememberLauncherForActivityResult
        }
        voiceCancelled = false
        isInitializing = true
        isRecording = true
        onVoiceStatusChange("⏳ Preparando reconocimiento de voz...")
        scope.launch {
            val vosk = VoskSpeechRecognizer(
                context = context.applicationContext,
                onPartialResult = { text -> runOnMain { onVoicePartial(text) } },
                onFinalResult = { text -> runOnMain { onVoiceResult(text) } },
                onStatus = { msg -> runOnMain { onVoiceStatusChange(msg) } },
                onError = { msg -> runOnMain { onVoiceStatusChange("❌ $msg") } }
            )
            val ok = vosk.initialize { pct ->
                runOnMain {
                    onVoiceStatusChange(
                        if (pct in 1..99) "⏳ Descargando modelo... $pct%" else "⏳ Extrayendo y cargando..."
                    )
                }
            }
            withContext(Dispatchers.Main) {
                isInitializing = false
                if (voiceCancelled) {
                    voskRef = null
                    isRecording = false
                    onVoiceStatusChange("⏹ Cancelado")
                    return@withContext
                }
                if (ok) {
                    voskRef = vosk
                    if (vosk.startListening()) {
                        onVoiceStatusChange("🔴 Escuchando — habla y verás las palabras en tiempo real")
                    } else {
                        onVoiceStatusChange("❌ Error al iniciar el micrófono")
                        voskRef = null
                        isRecording = false
                    }
                } else {
                    onVoiceStatusChange("❌ No se pudo cargar el modelo de voz")
                    voskRef = null
                    isRecording = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                voskRef?.stop()
            } catch (_: Exception) {}
            voskRef = null
        }
    }

    fun stopVoice() {
        voiceCancelled = true
        try {
            voskRef?.stop()
        } catch (_: Exception) {}
        voskRef = null
        isRecording = false
        isInitializing = false
        onMicLevelChange(0f)
        onVoicePartial("")
        onVoiceStatusChange("⏹ Grabación detenida")
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212328)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "MODO ESTUDIO — Explica con tus palabras",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = CornellPrimary
                )
                Spacer(Modifier.height(10.dp))

                val displayText = explanationText + explanationPartial
                OutlinedTextField(
                    value = displayText,
                    onValueChange = { if (!isRecording) onExplanationChange(it) },
                    readOnly = isRecording,
                    placeholder = {
                        Text(
                            "Tu voz aparece aquí en tiempo real...\nO escribe directamente.",
                            color = CornellTextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CornellText,
                        unfocusedTextColor = CornellText,
                        focusedBorderColor = CornellPrimary,
                        unfocusedBorderColor = CornellTextSecondary.copy(alpha = 0.5f),
                        cursorColor = CornellPrimary
                    )
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRecording || isInitializing) {
                                stopVoice()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.weight(0.73f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording || isInitializing) QuizWrong else CornellPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording || isInitializing) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (isRecording || isInitializing) "DETENER" else "HABLAR")
                    }
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(0.27f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CornellTextSecondary)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }

                Text(
                    voiceStatus,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = if (voiceStatus.contains("❌") || voiceStatus.contains("🔴")) QuizWrong else ScoreGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                if (micLevel > 0) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF2E3035), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(micLevel.coerceIn(0f, 1f))
                                .fillMaxSize()
                                .background(
                                    if (micLevel > 0.7f) QuizWrong else ScoreGreen,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onEvaluate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEvaluating && displayText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEvaluating) Color(0xFF38383A) else CornellPrimary
                    )
                ) {
                    if (isEvaluating) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = CornellText)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(if (isEvaluating) "Evaluando con IA..." else "EVALUAR MI EXPLICACIÓN")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212328)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scoreColor = when {
                    score < 0 -> ScoreGray
                    score < 40 -> ScoreRed
                    score < 65 -> ScoreOrange
                    score < 80 -> ScoreYellow
                    else -> ScoreGreen
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidth = 11.dp.toPx()
                        val radius = 56.dp.toPx()
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            color = Color(0xFF2E3338),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        if (score >= 0) {
                            val sweepAngle = (score / 100f) * 360f
                            drawArc(
                                color = scoreColor,
                                startAngle = 90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = center - Offset(radius, radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = if (score < 0) "--" else "$score%",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                        color = scoreColor
                    )
                }
                Text(
                    text = scoreLabelText.ifBlank {
                        if (score < 0) "Graba y evalúa para ver tu calificación" else ""
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = scoreColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (feedbackFound.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FeedbackGreenBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    feedbackFound,
                    modifier = Modifier.padding(14.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = ScoreGreen
                )
            }
        }

        if (feedbackMissing.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FeedbackRedBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    feedbackMissing,
                    modifier = Modifier.padding(14.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFA7A7A)
                )
            }
        }

        if (feedbackTip.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FeedbackYellowBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    feedbackTip,
                    modifier = Modifier.padding(14.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = ScoreYellow
                )
            }
        }

        if (score >= 0) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CornellPrimary)
            ) {
                Text("Intentar de nuevo")
            }
        }
    }
}

