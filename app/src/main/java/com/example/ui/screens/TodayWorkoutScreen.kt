package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExercisePrescription
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.viewmodel.SessionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayWorkoutScreen(
    profile: UserProfile?,
    workoutDays: List<WorkoutDay>,
    session: SessionUiState,
    onSelectDay: (Int) -> Unit,
    onToggleSet: (ExercisePrescription) -> Unit,
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    onQuickSwap: (WorkoutDay, ExercisePrescription) -> Unit,
    onAiSwap: (WorkoutDay, ExercisePrescription) -> Unit,
    onAskBuddyAboutExercise: (String) -> Unit,
    onLogWorkout: (WorkoutDay, String, String) -> Unit
) {
    val activeDay = workoutDays.find { it.dayNumber == session.selectedDayNumber }
        ?: workoutDays.firstOrNull()

    var showFinishDialog by remember { mutableStateOf(false) }

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
                        text = "Tap a set pill when you finish it — we'll handle your rest timer and guide your form.",
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

            if (session.isTimerRunning || session.restTimerRemainingSec > 0) {
                item {
                    RestTimerBanner(
                        remainingSec = session.restTimerRemainingSec,
                        totalSec = session.restTimerTotalSec.coerceAtLeast(1),
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
                                Icon(
                                    imageVector = if (activeDay.isRestDay) Icons.Filled.SelfImprovement else Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
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
                            onToggleSet = { onToggleSet(exercise) },
                            onStartRest = { onStartTimer(exercise.restSeconds) },
                            onQuickSwap = { onQuickSwap(activeDay, exercise) },
                            onAiSwap = { onAiSwap(activeDay, exercise) },
                            onAskBuddy = { onAskBuddyAboutExercise(exercise.name) }
                        )
                    }

                    item {
                        Button(
                            onClick = { showFinishDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("finish_workout_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Finish & Log Today's Session",
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
}

@Composable
private fun RestTimerBanner(
    remainingSec: Int,
    totalSec: Int,
    onAddSeconds: () -> Unit,
    onStop: () -> Unit
) {
    val mins = remainingSec / 60
    val secs = remainingSec % 60
    val formatted = String.format("%02d:%02d", mins, secs)
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
                            text = "REST TIMER",
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                progress = { (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f) },
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
    onStartRest: () -> Unit,
    onQuickSwap: () -> Unit,
    onAiSwap: () -> Unit,
    onAskBuddy: () -> Unit
) {
    var expandedCue by remember(exercise.id) { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    val weightText = if (exercise.suggestedWeightKg > 0.0) {
                        " • Start ~${exercise.suggestedWeightKg.toInt()} kg"
                    } else {
                        " • Bodyweight"
                    }
                    Text(
                        text = "${exercise.sets} sets × ${exercise.reps} reps$weightText • ${exercise.restSeconds}s rest",
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
                            .height(44.dp)
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
                    modifier = Modifier.height(44.dp)
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
