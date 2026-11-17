@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import com.example.data.model.ExercisePrescription
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.example.ui.theme.CyberLime
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmber
import com.example.ui.theme.HyperPurple
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkGlassCard
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.getMuscleAccentColor
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.viewmodel.SessionUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayWorkoutScreen(
    profile: UserProfile?,
    workoutDays: List<WorkoutDay>,
    session: SessionUiState,
    onSelectDay: (Int) -> Unit = {},
    onToggleSet: (ExercisePrescription) -> Unit = {},
    onAdjustWeight: (WorkoutDay, ExercisePrescription, Double) -> Unit = { _, _, _ -> },
    onStartTimer: (Int) -> Unit = {},
    onToggleTimerPause: () -> Unit = {},
    onToggleTimerSound: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    onQuickSwap: (WorkoutDay, ExercisePrescription) -> Unit = { _, _ -> },
    onAiSwap: (WorkoutDay, ExercisePrescription) -> Unit = { _, _ -> },
    onAskBuddyAboutExercise: (String) -> Unit = {},
    onLogWorkout: (WorkoutDay, String, String) -> Unit = { _, _, _ -> }
) {
    val activeDay = workoutDays.find { it.dayNumber == session.selectedDayNumber }
        ?: workoutDays.firstOrNull()

    val haptic = LocalHapticFeedback.current
    var showFinishDialog by remember { mutableStateOf(false) }
    var demoExercise by remember { mutableStateOf<ExercisePrescription?>(null) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var timerSoundEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(session.restTimerRemainingSec) {
        if (session.restTimerRemainingSec == 1 && session.isTimerRunning) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (timerSoundEnabled) {
                runCatching {
                    val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 260)
                    delay(320L)
                    tone.release()
                }
            }
        }
    }

    val totalSets = remember(activeDay) {
        activeDay?.exercises?.sumOf { it.sets } ?: 0
    }
    val doneSets = remember(activeDay, session.completedSetsByExerciseId) {
        activeDay?.exercises?.sumOf { ex ->
            (session.completedSetsByExerciseId[ex.id] ?: 0).coerceAtMost(ex.sets)
        } ?: 0
    }
    val sessionProgress by animateFloatAsState(
        targetValue = if (totalSets > 0) (doneSets.toFloat() / totalSets.toFloat()).coerceIn(0f, 1f) else 0f,
        label = "session_progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Hey ${profile?.displayName ?: "Athlete"}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap sets as you lift, dial weights in real time, and let the rest timer cue you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (workoutDays.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workoutDays, key = { it.dayNumber }) { day ->
                            val selected = day.dayNumber == activeDay?.dayNumber
                            Surface(
                                onClick = { onSelectDay(day.dayNumber) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) DarkGlassCard else MaterialTheme.colorScheme.surface,
                                border = if (selected) BorderStroke(1.5.dp, CyberLime) else BorderStroke(1.dp, DarkGlassBorder),
                                tonalElevation = if (selected) 2.dp else 0.dp,
                                modifier = Modifier.testTag("day_pill_${day.dayNumber}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "DAY ${day.dayNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) CyberLime else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (day.isRestDay) "Rest" else "Train",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (session.isTimerRunning || isTimerPaused || session.restTimerRemainingSec > 0) {
                item {
                    RestTimerBanner(
                        remainingSec = session.restTimerRemainingSec,
                        totalSec = session.restTimerTotalSec.coerceAtLeast(1),
                        isPaused = isTimerPaused,
                        soundEnabled = timerSoundEnabled,
                        onTogglePause = {
                            isTimerPaused = !isTimerPaused
                            onToggleTimerPause()
                        },
                        onToggleSound = {
                            timerSoundEnabled = !timerSoundEnabled
                            onToggleTimerSound()
                        },
                        onSubtractSeconds = {
                            onStartTimer((session.restTimerRemainingSec - 15).coerceAtLeast(5))
                        },
                        onAddSeconds = { onStartTimer(session.restTimerRemainingSec + 15) },
                        onStop = {
                            isTimerPaused = false
                            onStopTimer()
                        }
                    )
                }
            }

            if (activeDay == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No routine loaded yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Head to the Plan tab to generate your personalized routine.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeDay.isRestDay) {
                                Color(0xFF131D28)
                            } else {
                                Color(0xFF141F30)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (activeDay.isRestDay) NeonCyan.copy(alpha = 0.35f) else CyberLime.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (activeDay.isRestDay) NeonCyan.copy(alpha = 0.15f) else CyberLime.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(
                                        1.dp,
                                        if (activeDay.isRestDay) NeonCyan.copy(alpha = 0.4f) else CyberLime.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = if (activeDay.isRestDay) "⚡ RECOVERY & GROWTH" else "~${activeDay.estimatedMinutes} MIN SESSION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeDay.isRestDay) NeonCyan else CyberLime,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                if (totalSets > 0) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                    ) {
                                        Text(
                                            text = "$doneSets / $totalSets sets done",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (doneSets == totalSets && totalSets > 0) SuccessGreen else Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (activeDay.isRestDay) Icons.Filled.SelfImprovement else Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        tint = if (activeDay.isRestDay) NeonCyan else CyberLime
                                    )
                                }
                            }
                            Text(
                                text = activeDay.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = activeDay.focusArea,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1)
                            )
                            if (totalSets > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { sessionProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = CyberLime,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(sessionProgress * 100).toInt()}% COMPLETED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberLime,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${activeDay.exercises.size} MOVEMENTS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (activeDay.exercises.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = NeonCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SelfImprovement,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "HYPERTROPHY & RECOVERY ZONE",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Muscles don't grow in the gym — they grow during rest.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(text = "🚶", style = MaterialTheme.typography.titleMedium)
                                            Column {
                                                Text(
                                                    text = "Active Walk: 6,000 - 8,000 steps",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Flushes lactic acid and promotes blood circulation to sore joints.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(text = "🥩", style = MaterialTheme.typography.titleMedium)
                                            Column {
                                                Text(
                                                    text = "Hit 100% Daily Protein Target",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Check the Fuel tab for high-yield Desi protein choices today.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(text = "💧", style = MaterialTheme.typography.titleMedium)
                                            Column {
                                                Text(
                                                    text = "Hydrate with 3.5+ Liters",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Maintains intracellular hydration for muscle protein synthesis.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(activeDay.exercises, key = { it.id }) { exercise ->
                        ExerciseCoachCard(
                            exercise = exercise,
                            completedSets = session.completedSetsByExerciseId[exercise.id] ?: 0,
                            isSwapping = session.isSwappingExerciseId == exercise.id,
                            onToggleSet = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleSet(exercise)
                            },
                            onAdjustWeight = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAdjustWeight(activeDay, exercise, delta)
                            },
                            onStartRest = { onStartTimer(exercise.restSeconds) },
                            onQuickSwap = { onQuickSwap(activeDay, exercise) },
                            onAiSwap = { onAiSwap(activeDay, exercise) },
                            onAskBuddy = { onAskBuddyAboutExercise(exercise.name) },
                            onOpenDemo = { demoExercise = exercise }
                        )
                    }

                    item {
                        val allDone = totalSets > 0 && doneSets >= totalSets
                        Button(
                            onClick = { showFinishDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("finish_workout_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allDone) SuccessGreen else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (allDone) "All Sets Complete! Log Session" else "Finish & Log Today's Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showFinishDialog && activeDay != null) {
        FinishWorkoutDialog(
            dayTitle = activeDay.title,
            onDismiss = { showFinishDialog = false },
            onConfirm = { feeling, notes ->
                showFinishDialog = false
                onLogWorkout(activeDay, feeling, notes)
            }
        )
    }

    if (demoExercise != null) {
        ExerciseDemoDialog(
            exercise = demoExercise!!,
            onDismiss = { demoExercise = null },
            onAskBuddy = {
                val exName = demoExercise?.name.orEmpty()
                demoExercise = null
                onAskBuddyAboutExercise(exName)
            }
        )
    }
}

@Composable
private fun RestTimerBanner(
    remainingSec: Int,
    totalSec: Int,
    isPaused: Boolean,
    soundEnabled: Boolean,
    onTogglePause: () -> Unit,
    onToggleSound: () -> Unit,
    onSubtractSeconds: () -> Unit,
    onAddSeconds: () -> Unit,
    onStop: () -> Unit
) {
    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val formatted = String.format("%02d:%02d", mins, secs)
    val progress by animateFloatAsState(
        targetValue = (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f),
        label = "timer_progress"
    )
    val isAlmostDone = remainingSec in 1..10 && !isPaused

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGlassCard),
        border = BorderStroke(
            1.5.dp,
            if (isAlmostDone) CyberLime else if (isPaused) AmberGold else NeonCyan
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = (if (isAlmostDone) CyberLime else if (isPaused) AmberGold else NeonCyan).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Rest timer",
                                tint = if (isAlmostDone) CyberLime else if (isPaused) AmberGold else NeonCyan
                            )
                        }
                    }
                    Column {
                        Text(
                            text = when {
                                isPaused -> "REST PAUSED"
                                isAlmostDone -> "GET READY • 10s LEFT"
                                else -> "REST & HYDRATE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPaused -> AmberGold
                                isAlmostDone -> CyberLime
                                else -> NeonCyan
                            }
                        )
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.testTag("timer_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute timer chime" else "Enable timer chime",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("timer_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Resume timer" else "Pause timer",
                            tint = if (isPaused) CyberLime else Color.White
                        )
                    }
                    Surface(
                        onClick = onSubtractSeconds,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = "-15s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Surface(
                        onClick = onAddSeconds,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.testTag("timer_add_15_button")
                    ) {
                        Text(
                            text = "+15s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Surface(
                        onClick = onStop,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Red.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("timer_skip_button")
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isAlmostDone) CyberLime else if (isPaused) AmberGold else NeonCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCoachCard(
    exercise: ExercisePrescription,
    completedSets: Int,
    isSwapping: Boolean,
    onToggleSet: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onStartRest: () -> Unit,
    onQuickSwap: () -> Unit,
    onAiSwap: () -> Unit,
    onAskBuddy: () -> Unit,
    onOpenDemo: () -> Unit
) {
    var expandedCue by remember(exercise.id) { mutableStateOf(true) }
    val allSetsDone = completedSets >= exercise.sets
    val muscleAccent = getMuscleAccentColor(exercise.muscleGroup)
    var showPlateCalculator by remember { mutableStateOf(false) }
    var selectedRir by remember(exercise.id) { mutableStateOf(2) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allSetsDone) Color(0xFF0F231D) else DarkGlassCard
        ),
        border = BorderStroke(
            1.dp,
            if (allSetsDone) SuccessGreen.copy(alpha = 0.6f) else muscleAccent.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = muscleAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, muscleAccent.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = exercise.muscleGroup.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = muscleAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (allSetsDone) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SuccessGreen)
                            ) {
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${exercise.sets} sets × ${exercise.reps} reps • ${exercise.restSeconds}s rest",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { expandedCue = !expandedCue }
                        .testTag("toggle_cue_${exercise.id}"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (expandedCue) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle form guide",
                            tint = Color.White
                        )
                    }
                }
            }

            // Interactive Weight Stepper styled like barbell plates
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WORKING LOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (exercise.suggestedWeightKg > 0.0) {
                                "${formatWeight(exercise.suggestedWeightKg)} kg"
                            } else {
                                "Bodyweight"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberLime
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (exercise.suggestedWeightKg >= 20.0) {
                            Surface(
                                onClick = { showPlateCalculator = true },
                                shape = RoundedCornerShape(10.dp),
                                color = AmberGold.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("open_plates_${exercise.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "🏷️", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "Plates",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = { onAdjustWeight(-2.5) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("weight_minus_${exercise.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "-2.5",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Surface(
                            onClick = { onAdjustWeight(2.5) },
                            shape = RoundedCornerShape(10.dp),
                            color = CyberLime,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("weight_plus_${exercise.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+2.5",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // High-Dopamine Set Completion Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..exercise.sets).forEach { setIndex ->
                    val done = setIndex <= completedSets
                    Surface(
                        onClick = onToggleSet,
                        shape = RoundedCornerShape(12.dp),
                        color = if (done) SuccessGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(
                            1.dp,
                            if (done) SuccessGreen else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("set_pill_${exercise.id}_$setIndex")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (done) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Completed",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Set $setIndex",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (done) SuccessGreen else Color.White
                            )
                        }
                    }
                }
                Surface(
                    onClick = onStartRest,
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Start rest timer",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${exercise.restSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Scientific Hypertrophy RIR (Reps In Reserve) Intensity Selector
            Surface(
                color = Color.Black.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HYPERTROPHY RIR INTENSITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = when (selectedRir) {
                                0 -> "0 RIR • True Failure"
                                1 -> "1 RIR • Extreme Hard"
                                2 -> "2 RIR • Sweetspot (RP)"
                                else -> "3+ RIR • Warmup / Submax"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedRir) {
                                0 -> NeonEmber
                                1 -> AmberGold
                                2 -> CyberLime
                                else -> NeonCyan
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(0, "0 RIR", NeonEmber),
                            Triple(1, "1 RIR", AmberGold),
                            Triple(2, "2 RIR", CyberLime),
                            Triple(3, "3 RIR", NeonCyan)
                        ).forEach { (rirVal, rirLabel, rirColor) ->
                            val isSelected = selectedRir == rirVal
                            Surface(
                                onClick = { selectedRir = rirVal },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) rirColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) rirColor else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = rirLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isSelected) rirColor else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expandedCue) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Biomechanical Cue",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = exercise.setupCue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFF2A1515),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Form Mistake to Avoid",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80)
                                )
                                Text(
                                    text = exercise.formMistakeToAvoid,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFCDD2)
                                )
                            }
                        }
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDemo,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyberLime.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, CyberLime.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("open_demo_${exercise.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Watch Exercise Demo GIF",
                        tint = CyberLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "▶ 3D Path & GIF",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberLime,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (exercise.alternativeExercise.isNotBlank()) {
                    OutlinedButton(
                        onClick = onQuickSwap,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("quick_swap_${exercise.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Swap: ${exercise.alternativeExercise}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAiSwap,
                    enabled = !isSwapping,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("ai_swap_${exercise.id}")
                ) {
                    if (isSwapping) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("AI Swap", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onAskBuddy,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("ask_buddy_${exercise.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Ask Buddy", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showPlateCalculator) {
        PlateCalculatorDialog(
            totalWeightKg = exercise.suggestedWeightKg,
            onDismiss = { showPlateCalculator = false }
        )
    }
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.1f", weight)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlateCalculatorDialog(
    totalWeightKg: Double,
    onDismiss: () -> Unit
) {
    var currentWeight by remember { mutableStateOf(totalWeightKg.coerceAtLeast(20.0)) }
    val barWeight = 20.0
    val netPlatesWeight = (currentWeight - barWeight).coerceAtLeast(0.0)
    val weightPerSide = netPlatesWeight / 2.0

    // Standard Olympic bumper plates: 25kg (Red), 20kg (Blue), 15kg (Yellow), 10kg (Green), 5kg (White), 2.5kg (Dark), 1.25kg (Silver)
    data class PlateInfo(val weight: Double, val color: Color, val textColor: Color, val heightFraction: Float)
    val availablePlates = remember {
        listOf(
            PlateInfo(25.0, Color(0xFFE53935), Color.White, 1.0f),
            PlateInfo(20.0, Color(0xFF1E88E5), Color.White, 0.92f),
            PlateInfo(15.0, Color(0xFFFDD835), Color.Black, 0.84f),
            PlateInfo(10.0, Color(0xFF43A047), Color.White, 0.76f),
            PlateInfo(5.0, Color(0xFFEEEEEE), Color.Black, 0.65f),
            PlateInfo(2.5, Color(0xFF424242), Color.White, 0.52f),
            PlateInfo(1.25, Color(0xFF9E9E9E), Color.Black, 0.42f)
        )
    }

    val sidePlates = remember(weightPerSide) {
        val result = mutableListOf<PlateInfo>()
        var rem = weightPerSide
        for (p in availablePlates) {
            val count = (rem / p.weight).toInt()
            repeat(count) {
                result.add(p)
            }
            rem -= count * p.weight
            rem = ((rem * 100).toInt()) / 100.0
        }
        result
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkGlassCard,
            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = AmberGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "OLYMPIC BAR 20KG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Plate Math",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Total weight & per side readouts
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL LOAD", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text(
                                text = "${formatWeight(currentWeight)} kg",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberLime
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EACH SIDE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text(
                                text = "${formatWeight(weightPerSide)} kg",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AmberGold
                            )
                        }
                    }
                }

                // Visual Barbell Graphic with colored plates stacked
                Surface(
                    color = Color(0xFF090D16),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Bar shaft
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFF888888), Color(0xFF444444))))
                        )
                        // Collar stopper & Plates
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(60.dp)
                                    .background(Color(0xFFCCCCCC), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            if (sidePlates.isEmpty()) {
                                Text(
                                    text = "Empty 20 kg Olympic Barbell",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    sidePlates.take(8).forEach { p ->
                                        Surface(
                                            color = p.color,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .width(22.dp)
                                                .height((80 * p.heightFraction).dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = formatWeight(p.weight),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = p.textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Plate breakdown list chips
                if (sidePlates.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sidePlates.groupBy { it.weight }.forEach { (weight, list) ->
                            val plate = list.first()
                            Surface(
                                color = plate.color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, plate.color)
                            ) {
                                Text(
                                    text = "${list.size}× ${formatWeight(weight)}kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Weight adjust stepper row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { currentWeight = (currentWeight - 5.0).coerceAtLeast(20.0) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-5 kg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Surface(
                        onClick = { currentWeight = (currentWeight - 2.5).coerceAtLeast(20.0) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-2.5 kg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Surface(
                        onClick = { currentWeight += 2.5 },
                        shape = RoundedCornerShape(10.dp),
                        color = CyberLime.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CyberLime),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+2.5 kg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CyberLime)
                        }
                    }
                    Surface(
                        onClick = { currentWeight += 5.0 },
                        shape = RoundedCornerShape(10.dp),
                        color = CyberLime.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CyberLime),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+5 kg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CyberLime)
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It • Load The Bar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FinishWorkoutDialog(
    dayTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (feeling: String, notes: String) -> Unit
) {
    var feeling by remember { mutableStateOf("great") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nice work on $dayTitle!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How did today's workout feel?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("great" to "Strong", "good" to "Good", "tough" to "Tough").forEach { (key, label) ->
                        FilterChip(
                            selected = feeling == key,
                            onClick = { feeling = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Quick note (weights, how form felt...)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(feeling, notes) },
                modifier = Modifier.testTag("confirm_log_workout_button")
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExerciseDemoDialog(
    exercise: ExercisePrescription,
    onDismiss: () -> Unit,
    onAskBuddy: () -> Unit
) {
    var isSlowMo by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("bio") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${exercise.muscleGroup.uppercase()} • 60 FPS BIOMECHANICS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close demo")
                    }
                }

                // Controls: Mode & Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = viewMode == "bio",
                            onClick = { viewMode = "bio" },
                            leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("⚡ 60fps Bio Engine", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = viewMode == "kinematics",
                            onClick = { viewMode = "kinematics" },
                            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("🧬 Vector Path", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    FilterChip(
                        selected = isSlowMo,
                        onClick = { isSlowMo = !isSlowMo },
                        leadingIcon = { Icon(Icons.Filled.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(if (isSlowMo) "0.5x Slow" else "1.0x Normal", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Screen Visual Area
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF080C14),
                    border = BorderStroke(1.dp, CyberLime.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                ) {
                    if (viewMode == "bio") {
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    setBackgroundColor(0)
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.javaScriptEnabled = true
                                    loadDataWithBaseURL(null, getBiomechanicalMotionHtml(exercise.name, isSlowMo), "text/html", "utf-8", null)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(null, getBiomechanicalMotionHtml(exercise.name, isSlowMo), "text/html", "utf-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ExerciseKinematicCanvas(
                            exerciseName = exercise.name,
                            isSlowMo = isSlowMo
                        )
                    }
                }

                // Setup Cues
                val cueDetails = getExerciseSetupDetails(exercise.name, exercise.setupCue)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Biomechanical Execution",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        cueDetails.forEachIndexed { i, cue ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${i + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = cue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Red Flag Mistake Alert
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Mistake to Avoid",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = exercise.formMistakeToAvoid.ifBlank { "Avoid using momentum or compromised posture." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Ask Buddy CTA
                Button(
                    onClick = onAskBuddy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Ask Buddy About ${exercise.name}")
                }
            }
        }
    }
}

@Composable
private fun ExerciseKinematicCanvas(
    exerciseName: String,
    isSlowMo: Boolean
) {
    val duration = if (isSlowMo) 4800 else 2400
    val transition = rememberInfiniteTransition(label = "kinematic_loop")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing)
        ),
        label = "rep_cycle"
    )

    val (phaseText, phaseColor) = when {
        progress < 0.18f -> "1. TOP LOCKOUT • BRACE CORE" to Color(0xFF00E5FF)
        progress < 0.60f -> "2. ECCENTRIC DESCENT (2.4s)" to Color(0xFFD7F957)
        progress < 0.75f -> "3. DEEP STRETCH PAUSE (1.0s)" to Color(0xFFFF9800)
        else -> "4. POWER CONCENTRIC DRIVE" to Color(0xFF10B981)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val w = size.width
            val h = size.height

            val isLower = exerciseName.contains("squat", ignoreCase = true) || exerciseName.contains("leg", ignoreCase = true)
            val isPull = exerciseName.contains("pull", ignoreCase = true) || exerciseName.contains("row", ignoreCase = true)

            val cycle = if (progress < 0.60f) {
                ((progress - 0.18f) / 0.42f).coerceIn(0f, 1f)
            } else if (progress < 0.75f) {
                1f
            } else {
                (1f - ((progress - 0.75f) / 0.25f)).coerceIn(0f, 1f)
            }

            // Grid lines
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h),
                strokeWidth = 1.dp.toPx()
            )

            // Bench Incline Guide (for presses)
            if (!isLower && !isPull) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(w * 0.25f, h * 0.75f),
                    end = Offset(w * 0.75f, h * 0.45f),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val startY = if (isPull) h * 0.18f else h * 0.28f
            val endY = if (isPull) h * 0.72f else h * 0.72f
            val curY = startY + (endY - startY) * cycle

            // Motion Trajectory Guide Path
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                start = Offset(w * 0.5f, startY),
                end = Offset(w * 0.5f, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Dynamic Joint Humerus & Forearm Lines
            val shoulderOffset = Offset(w * 0.42f, h * 0.55f)
            val elbowOffset = Offset(w * 0.38f + (w * 0.06f * cycle), curY + 20.dp.toPx() * (1f - cycle))
            val loadOffset = Offset(w * 0.5f, curY)

            drawLine(
                color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                start = shoulderOffset,
                end = elbowOffset,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFCBD5E1),
                start = elbowOffset,
                end = loadOffset,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Muscle Heatmap Glow
            val glowAlpha = (0.25f + 0.65f * cycle).coerceIn(0f, 1f)
            drawCircle(
                color = phaseColor.copy(alpha = glowAlpha * 0.5f),
                radius = 24.dp.toPx(),
                center = elbowOffset
            )

            // Barbell / Dumbbell
            val barbellWidth = w * 0.52f
            val leftX = (w - barbellWidth) / 2
            val rightX = (w + barbellWidth) / 2

            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(leftX, curY),
                end = Offset(rightX, curY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Weight Plates
            drawCircle(
                color = CyberLime,
                radius = 11.dp.toPx(),
                center = Offset(leftX, curY)
            )
            drawCircle(
                color = CyberLime,
                radius = 11.dp.toPx(),
                center = Offset(rightX, curY)
            )

            // Center glow core
            drawCircle(
                color = phaseColor,
                radius = 5.dp.toPx(),
                center = loadOffset
            )
        }

        // Phase Overlay Pill at bottom
        Surface(
            color = Color.Black.copy(alpha = 0.8f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(phaseColor, RoundedCornerShape(50))
                )
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getBiomechanicalMotionHtml(exerciseName: String, isSlowMo: Boolean): String {
    val name = exerciseName.lowercase()
    val isPress = name.contains("press") || name.contains("bench") || name.contains("push")
    val isLateral = name.contains("lateral") || name.contains("raise")
    val isTricep = name.contains("tricep") || name.contains("overhead") || name.contains("extension")
    val isPull = name.contains("pulldown") || name.contains("pull") || name.contains("row") || name.contains("chin")
    val isSquat = name.contains("squat") || name.contains("lunge") || name.contains("leg")

    val duration = if (isSlowMo) "6.4s" else "3.6s"
    val muscleName = when {
        isPress -> "Clavicular Pectoralis Major & Front Delt"
        isLateral -> "Lateral Deltoid (Scapular Plane)"
        isTricep -> "Long Head Triceps Brachii"
        isPull -> "Latissimus Dorsi & Teres Major"
        isSquat -> "Quadriceps Femoris & Gluteus"
        else -> "Target Muscle Group"
    }

    val primaryHex = when {
        isPress -> "#00E5FF"
        isLateral -> "#D7F957"
        isTricep -> "#FF7043"
        isPull -> "#38BDF8"
        isSquat -> "#A78BFA"
        else -> "#D7F957"
    }

    val angleReadout = when {
        isPress -> "30° Bench • 45° Elbow Tuck"
        isLateral -> "10° Torso • 30° Scapular Plane"
        isTricep -> "180° Upper Arm • 90° Flexion"
        isPull -> "120° Grip • Clavicle Drive"
        isSquat -> "90° Parallel • Hips Back"
        else -> "Optimal Joint Articulation"
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    background: #080C14;
                    color: #F8FAFC;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: space-between;
                    height: 100vh;
                    overflow: hidden;
                    padding: 8px 10px;
                }
                .hud-header {
                    width: 100%;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    font-size: 10px;
                    font-weight: 700;
                    letter-spacing: 0.5px;
                }
                .badge-muscle {
                    background: rgba(0, 229, 255, 0.15);
                    border: 1px solid $primaryHex;
                    color: $primaryHex;
                    padding: 2px 8px;
                    border-radius: 6px;
                    text-transform: uppercase;
                }
                .badge-angle {
                    color: #94A3B8;
                    font-size: 10px;
                }
                .canvas-wrap {
                    flex: 1;
                    width: 100%;
                    max-height: 155px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    position: relative;
                }
                svg {
                    width: 100%;
                    height: 100%;
                }
                /* Rep Motion Keyframe */
                @keyframes repPacer {
                    0% { transform: translateY(0px); }
                    15% { transform: translateY(0px); }
                    55% { transform: translateY(38px); }
                    70% { transform: translateY(38px); }
                    92% { transform: translateY(0px); }
                    100% { transform: translateY(0px); }
                }
                @keyframes musclePulsing {
                    0% { opacity: 0.35; filter: drop-shadow(0 0 2px $primaryHex); }
                    50% { opacity: 0.95; filter: drop-shadow(0 0 8px $primaryHex); }
                    75% { opacity: 1.0; filter: drop-shadow(0 0 12px $primaryHex); }
                    100% { opacity: 0.35; filter: drop-shadow(0 0 2px $primaryHex); }
                }
                @keyframes pacerBar {
                    0% { width: 0%; }
                    100% { width: 100%; }
                }
                .anim-load {
                    animation: repPacer $duration cubic-bezier(0.4, 0.0, 0.2, 1) infinite;
                }
                .anim-muscle {
                    animation: musclePulsing $duration ease-in-out infinite;
                }
                .pacer-track {
                    width: 100%;
                    background: rgba(255, 255, 255, 0.08);
                    height: 4px;
                    border-radius: 2px;
                    overflow: hidden;
                    margin-top: 4px;
                }
                .pacer-fill {
                    height: 100%;
                    background: linear-gradient(90deg, #00E5FF, #D7F957, #FF7043, #10B981);
                    animation: pacerBar $duration linear infinite;
                }
                .phase-ticker {
                    width: 100%;
                    display: flex;
                    justify-content: space-between;
                    font-size: 9px;
                    color: #64748B;
                    font-weight: 600;
                    margin-top: 2px;
                }
                .active-phase {
                    color: #D7F957;
                    font-weight: 800;
                }
            </style>
        </head>
        <body>
            <div class="hud-header">
                <span class="badge-muscle">$muscleName</span>
                <span class="badge-angle">$angleReadout</span>
            </div>

            <div class="canvas-wrap">
                <svg viewBox="0 0 320 150">
                    <defs>
                        <radialGradient id="neonCore" cx="50%" cy="50%" r="50%">
                            <stop offset="0%" stop-color="#FFFFFF" />
                            <stop offset="60%" stop-color="$primaryHex" />
                            <stop offset="100%" stop-color="transparent" />
                        </radialGradient>
                        <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                            <feGaussianBlur stdDeviation="3" result="blur" />
                            <feComposite in="SourceGraphic" in2="blur" operator="over" />
                        </filter>
                    </defs>

                    <!-- Technical Grid -->
                    <line x1="20" y1="75" x2="300" y2="75" stroke="rgba(255,255,255,0.04)" stroke-dasharray="3,3" />
                    <line x1="160" y1="10" x2="160" y2="140" stroke="rgba(255,255,255,0.04)" stroke-dasharray="3,3" />

                    <!-- Bench & Skeletal Platform -->
                    <line x1="70" y1="125" x2="250" y2="65" stroke="#1E293B" stroke-width="8" stroke-linecap="round" />
                    <line x1="150" y1="98" x2="150" y2="135" stroke="#0F172A" stroke-width="6" />

                    <!-- Mannequin Torso & Head -->
                    <line x1="95" y1="117" x2="215" y2="77" stroke="#334155" stroke-width="12" stroke-linecap="round" />
                    <circle cx="232" cy="71" r="12" fill="#1E293B" stroke="#475569" stroke-width="2" />

                    <!-- Active Muscle Belly Heatmap -->
                    <polygon points="175,85 140,97 155,108 190,92" fill="$primaryHex" class="anim-muscle" />

                    <!-- Shoulder Pivot -->
                    <circle cx="180" cy="88" r="5" fill="#00E5FF" />

                    <!-- Articulated Moving Arm & Dumbbell Load -->
                    <g class="anim-load">
                        <!-- Upper Arm -->
                        <line x1="180" y1="88" x2="155" y2="52" stroke="#94A3B8" stroke-width="5" stroke-linecap="round" />
                        <!-- Forearm -->
                        <line x1="155" y1="52" x2="165" y2="18" stroke="#CBD5E1" stroke-width="4" stroke-linecap="round" />
                        <!-- Elbow Joint Badge -->
                        <circle cx="155" cy="52" r="4" fill="#D7F957" />

                        <!-- Dumbbell Shaft -->
                        <line x1="140" y1="18" x2="190" y2="18" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round" />
                        <!-- Dumbbell Plates -->
                        <rect x="135" y="8" width="6" height="20" rx="2" fill="#D7F957" filter="url(#glow)" />
                        <rect x="189" y="8" width="6" height="20" rx="2" fill="#D7F957" filter="url(#glow)" />
                        <!-- Core Center Glow -->
                        <circle cx="165" cy="18" r="4" fill="url(#neonCore)" />
                    </g>

                    <!-- Protractor Joint Guide -->
                    <path d="M 170 88 A 20 20 0 0 0 160 70" fill="none" stroke="$primaryHex" stroke-width="1.5" stroke-dasharray="2,2" />
                    <text x="185" y="65" fill="$primaryHex" font-size="8" font-weight="bold">45° TUCK</text>
                </svg>
            </div>

            <!-- Dynamic 4-Phase Rep Pacer -->
            <div style="width: 100%;">
                <div class="pacer-track">
                    <div class="pacer-fill"></div>
                </div>
                <div class="phase-ticker">
                    <span>1. Lockout</span>
                    <span class="active-phase">2. Eccentric (2.4s)</span>
                    <span>3. Stretch</span>
                    <span>4. Drive</span>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun getExerciseSetupDetails(exerciseName: String, setupCue: String): List<String> {
    val name = exerciseName.lowercase()
    return when {
        name.contains("incline") -> listOf(
            "Set bench to 30° angle (2nd or 3rd notch from bottom).",
            "Pinch shoulder blades into the bench and maintain a slight natural arch.",
            "Tuck elbows at 45° to protect rotator cuffs; lower for 2-3 seconds.",
            "Press straight up over upper chest without banging dumbbells together."
        )
        name.contains("lateral") -> listOf(
            "Lean torso forward slightly (5-10°) to match side delt fibers.",
            "Raise dumbbells 30° in front of your body (scapular plane).",
            "Lead with your elbows and keep pinkies slightly tilted.",
            "Stop at shoulder height — going higher engages upper traps."
        )
        name.contains("tricep") || name.contains("overhead") -> listOf(
            "Lock upper arms vertically beside your ears; don't flare elbows wide.",
            "Lower weight behind head until you feel a deep long-head stretch.",
            "Extend elbows and flare rope outwards at the peak contraction.",
            "Brace abs tightly to prevent excessive lower back arching."
        )
        name.contains("pulldown") -> listOf(
            "Set thigh pad snug so knees cannot lift during the set.",
            "Depress shoulder blades first before pulling with your elbows.",
            "Pull bar down to upper collarbone with your chest held high.",
            "Avoid swinging your torso backwards 45° for momentum."
        )
        name.contains("squat") -> listOf(
            "Shoulder-width stance with toes angled outward 15-30°.",
            "Screw your feet into the floor to activate your hips & glutes.",
            "Push knees out over your 2nd toe as you sink hips below parallel.",
            "Drive through mid-foot with chest proud on the ascent."
        )
        else -> listOf(
            setupCue.ifBlank { "Keep your core braced and posture tall." },
            "Control the eccentric phase for 2 seconds.",
            "Pause briefly at peak contraction without locking joints aggressively."
        )
    }
}
