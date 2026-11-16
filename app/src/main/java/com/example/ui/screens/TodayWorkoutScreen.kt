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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExercisePrescription
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
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
    onSelectDay: (Int) -> Unit,
    onToggleSet: (ExercisePrescription) -> Unit,
    onAdjustWeight: (WorkoutDay, ExercisePrescription, Double) -> Unit,
    onStartTimer: (Int) -> Unit,
    onToggleTimerPause: () -> Unit,
    onToggleTimerSound: () -> Unit,
    onStopTimer: () -> Unit,
    onQuickSwap: (WorkoutDay, ExercisePrescription) -> Unit,
    onAiSwap: (WorkoutDay, ExercisePrescription) -> Unit,
    onAskBuddyAboutExercise: (String) -> Unit,
    onLogWorkout: (WorkoutDay, String, String) -> Unit
) {
    val activeDay = workoutDays.find { it.dayNumber == session.selectedDayNumber }
        ?: workoutDays.firstOrNull()

    val haptic = LocalHapticFeedback.current
    var showFinishDialog by remember { mutableStateOf(false) }
    var demoExercise by remember { mutableStateOf<ExercisePrescription?>(null) }

    LaunchedEffect(session.timerCompletedCount) {
        if (session.timerCompletedCount > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (session.timerSoundEnabled) {
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
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                tonalElevation = if (selected) 0.dp else 1.dp,
                                modifier = Modifier.testTag("day_pill_${day.dayNumber}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "DAY ${day.dayNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = if (day.isRestDay) "Rest" else "Train",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (session.isTimerRunning || session.isTimerPaused || session.restTimerRemainingSec > 0) {
                item {
                    RestTimerBanner(
                        remainingSec = session.restTimerRemainingSec,
                        totalSec = session.restTimerTotalSec.coerceAtLeast(1),
                        isPaused = session.isTimerPaused,
                        soundEnabled = session.timerSoundEnabled,
                        onTogglePause = onToggleTimerPause,
                        onToggleSound = onToggleTimerSound,
                        onSubtractSeconds = {
                            onStartTimer((session.restTimerRemainingSec - 15).coerceAtLeast(5))
                        },
                        onAddSeconds = { onStartTimer(session.restTimerRemainingSec + 15) },
                        onStop = onStopTimer
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
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeDay.isRestDay) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (activeDay.isRestDay) "RECOVERY DAY" else "~${activeDay.estimatedMinutes} MIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                if (totalSets > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "$doneSets / $totalSets sets",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (activeDay.isRestDay) Icons.Filled.SelfImprovement else Icons.Filled.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Text(
                                text = activeDay.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (activeDay.isRestDay) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Text(
                                text = activeDay.focusArea,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (activeDay.isRestDay) {
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                }
                            )
                            if (totalSets > 0) {
                                LinearProgressIndicator(
                                    progress = { sessionProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }

                if (activeDay.exercises.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Muscles grow while you recover!",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Aim for a 20-minute brisk walk, hit your protein target in the Fuel tab, and hydrate well.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Rest timer",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = if (isPaused) "REST PAUSED" else "REST TIMER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.testTag("timer_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) "Mute timer chime" else "Enable timer chime",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("timer_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Resume timer" else "Pause timer",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    TextButton(onClick = onSubtractSeconds) {
                        Text("-15s")
                    }
                    TextButton(
                        onClick = onAddSeconds,
                        modifier = Modifier.testTag("timer_add_15_button")
                    ) {
                        Text("+15s")
                    }
                    TextButton(
                        onClick = onStop,
                        modifier = Modifier.testTag("timer_skip_button")
                    ) {
                        Text("Skip")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.secondary
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

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allSetsDone) {
                SuccessGreenSoft.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = exercise.muscleGroup.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${exercise.sets} sets × ${exercise.reps} reps • ${exercise.restSeconds}s rest",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { expandedCue = !expandedCue }
                        .testTag("toggle_cue_${exercise.id}"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (expandedCue) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle form guide"
                        )
                    }
                }
            }

            // Interactive Weight Stepper saved to Firestore
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (exercise.suggestedWeightKg > 0.0) {
                            "Working Weight: ${formatWeight(exercise.suggestedWeightKg)} kg"
                        } else {
                            "Bodyweight (tap + to add load)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onAdjustWeight(-2.5) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("weight_minus_${exercise.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Decrease weight by 2.5 kg",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { onAdjustWeight(2.5) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("weight_plus_${exercise.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Increase weight by 2.5 kg",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Set Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..exercise.sets).forEach { setIndex ->
                    val done = setIndex <= completedSets
                    Surface(
                        onClick = onToggleSet,
                        shape = RoundedCornerShape(12.dp),
                        color = if (done) SuccessGreenSoft else MaterialTheme.colorScheme.surfaceVariant,
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
                                color = if (done) SuccessGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Surface(
                    onClick = onStartRest,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
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
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${exercise.restSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expandedCue) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
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
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "How to set up & do it",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = exercise.setupCue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
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
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Mistake to avoid",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = exercise.formMistakeToAvoid,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
                            text = "Taken? Swap: ${exercise.alternativeExercise}",
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
                    onClick = onOpenDemo,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.testTag("open_demo_${exercise.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Watch Exercise Demo GIF",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Demo & GIF",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format("%.1f", weight)
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
    var viewMode by remember { mutableStateOf("kinematics") }

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
                                text = "${exercise.muscleGroup.uppercase()} • 3D FORM SPOTTER",
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
                            selected = viewMode == "kinematics",
                            onClick = { viewMode = "kinematics" },
                            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("3D Path", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = viewMode == "gif",
                            onClick = { viewMode = "gif" },
                            leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("GIF Demo", style = MaterialTheme.typography.labelSmall) }
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
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    if (viewMode == "gif") {
                        val gifUrl = getExerciseGifUrl(exercise.name)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        setBackgroundColor(0)
                                        settings.loadWithOverviewMode = true
                                        settings.useWideViewPort = true
                                        val html = """
                                            <html>
                                            <head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                                            <body style="margin:0;padding:0;background:#0E131B;display:flex;align-items:center;justify-content:center;height:100vh;">
                                                <img src="$gifUrl" style="max-width:100%;max-height:100%;border-radius:14px;object-fit:contain;" />
                                            </body>
                                            </html>
                                        """.trimIndent()
                                        loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "GIF Motion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
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
        progress < 0.20f -> "1. TOP LOCKOUT • BRACE CORE" to Color(0xFF00E5FF)
        progress < 0.60f -> "2. ECCENTRIC DESCENT (2-3s)" to Color(0xFFD7F957)
        progress < 0.75f -> "3. DEEP STRETCH PAUSE (1s)" to Color(0xFFFF9800)
        else -> "4. POWER CONCENTRIC DRIVE" to Color(0xFF10B981)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val w = size.width
            val h = size.height

            val isLower = exerciseName.contains("squat", ignoreCase = true) || exerciseName.contains("leg", ignoreCase = true)
            val isPull = exerciseName.contains("pull", ignoreCase = true) || exerciseName.contains("row", ignoreCase = true)

            val cycle = if (progress < 0.60f) {
                ((progress - 0.20f) / 0.40f).coerceIn(0f, 1f)
            } else if (progress < 0.75f) {
                1f
            } else {
                (1f - ((progress - 0.75f) / 0.25f)).coerceIn(0f, 1f)
            }

            // Grid lines
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h),
                strokeWidth = 1.dp.toPx()
            )

            val startY = if (isPull) h * 0.2f else h * 0.3f
            val endY = if (isPull) h * 0.75f else h * 0.78f
            val curY = startY + (endY - startY) * cycle

            // Motion Trajectory Guide Path
            drawLine(
                color = Color(0xFF2E6BFF).copy(alpha = 0.45f),
                start = Offset(w * 0.5f, startY),
                end = Offset(w * 0.5f, endY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Dumbbell or Barbell Width
            val barbellWidth = w * 0.55f
            val leftX = (w - barbellWidth) / 2
            val rightX = (w + barbellWidth) / 2

            // Barbell rod
            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = Offset(leftX, curY),
                end = Offset(rightX, curY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Weight Plates
            drawCircle(
                color = Color(0xFFD7F957),
                radius = 12.dp.toPx(),
                center = Offset(leftX, curY)
            )
            drawCircle(
                color = Color(0xFFD7F957),
                radius = 12.dp.toPx(),
                center = Offset(rightX, curY)
            )

            // Center glow core
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 5.dp.toPx(),
                center = Offset(w * 0.5f, curY)
            )
        }

        // Phase Overlay Pill at bottom
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
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

private fun getExerciseGifUrl(exerciseName: String): String {
    val name = exerciseName.lowercase()
    return when {
        name.contains("incline") && name.contains("bench") ->
            "https://raw.githubusercontent.com/vvishalkumar7070-lang/Vishal-Gupta/main/gifs/incline_db_press.gif"
        name.contains("lateral") ->
            "https://raw.githubusercontent.com/vvishalkumar7070-lang/Vishal-Gupta/main/gifs/lateral_raise.gif"
        name.contains("tricep") || name.contains("overhead") ->
            "https://raw.githubusercontent.com/vvishalkumar7070-lang/Vishal-Gupta/main/gifs/triceps_extension.gif"
        name.contains("lat pulldown") || name.contains("pulldown") ->
            "https://raw.githubusercontent.com/vvishalkumar7070-lang/Vishal-Gupta/main/gifs/lat_pulldown.gif"
        name.contains("squat") ->
            "https://raw.githubusercontent.com/vvishalkumar7070-lang/Vishal-Gupta/main/gifs/goblet_squat.gif"
        else ->
            "https://raw.githubusercontent.com/vvishalkumar7070-lang/Vishal-Gupta/main/gifs/general_lift.gif"
    }
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
