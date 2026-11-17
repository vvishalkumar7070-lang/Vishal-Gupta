package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BuddyRole
import com.example.data.ai.SpotMeAiService
import com.example.data.model.BuddyMessage
import com.example.data.model.ExercisePrescription
import com.example.data.model.MealLog
import com.example.data.model.MealSuggestion
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.example.data.model.WorkoutLog
import com.example.data.repository.SpotMeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class SessionUiState(
    val selectedDayNumber: Int = 1,
    val completedSetsByExerciseId: Map<String, Int> = emptyMap(),
    val restTimerRemainingSec: Int = 0,
    val restTimerTotalSec: Int = 0,
    val isTimerRunning: Boolean = false,
    val isTimerPaused: Boolean = false,
    val timerSoundEnabled: Boolean = true,
    val timerCompletedCount: Int = 0,
    val isGeneratingPlan: Boolean = false,
    val isSwappingExerciseId: String? = null,
    val isGeneratingMeal: Boolean = false,
    val mealSuggestion: MealSuggestion? = null,
    val waterGlassesToday: Int = 0,
    val selectedBuddyRole: BuddyRole = BuddyRole.FORM_SPOTTER,
    val streamingCoachReply: String? = null,
    val isAskingBuddy: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null
)

class SpotMeViewModel(
    private val repository: SpotMeRepository,
    private val aiService: SpotMeAiService,
    private val currentUserId: String,
    val defaultDisplayName: String
) : ViewModel() {

    val todayDateKey: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val workoutDaysState: StateFlow<UiState<List<WorkoutDay>>> = repository.observeWorkoutDays(currentUserId)
        .map<List<WorkoutDay>, UiState<List<WorkoutDay>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing workout days", root)
            emit(UiState.Error(root.message ?: "Failed to load workout plan"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val workoutLogsState: StateFlow<UiState<List<WorkoutLog>>> = repository.observeWorkoutLogs(currentUserId)
        .map<List<WorkoutLog>, UiState<List<WorkoutLog>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing workout logs", root)
            emit(UiState.Error(root.message ?: "Failed to load workout history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val mealLogsState: StateFlow<UiState<List<MealLog>>> = repository.observeMealLogs(currentUserId)
        .map<List<MealLog>, UiState<List<MealLog>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing meal logs", root)
            emit(UiState.Error(root.message ?: "Failed to load meals"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val buddyMessagesState: StateFlow<UiState<List<BuddyMessage>>> = repository.observeBuddyMessages(currentUserId)
        .map<List<BuddyMessage>, UiState<List<BuddyMessage>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing buddy messages", root)
            emit(UiState.Error(root.message ?: "Failed to load gym buddy messages"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    private val _session = MutableStateFlow(SessionUiState())
    val session: StateFlow<SessionUiState> = _session.asStateFlow()

    private var timerJob: Job? = null

    fun selectDay(dayNumber: Int) {
        _session.update { it.copy(selectedDayNumber = dayNumber) }
    }

    fun selectBuddyRole(role: BuddyRole) {
        _session.update { it.copy(selectedBuddyRole = role) }
    }

    fun adjustWaterGlasses(delta: Int) {
        _session.update {
            it.copy(waterGlassesToday = (it.waterGlassesToday + delta).coerceIn(0, 16))
        }
    }

    fun clearMessages() {
        _session.update { it.copy(bannerMessage = null, errorMessage = null) }
    }

    fun generateAndSavePlan(
        displayName: String,
        goal: String,
        experienceLevel: String,
        equipmentAccess: String,
        sportFocus: String,
        dietaryStyle: String,
        daysPerWeek: Int
    ) {
        viewModelScope.launch {
            _session.update { it.copy(isGeneratingPlan = true, errorMessage = null, bannerMessage = null) }
            try {
                val draftProfile = UserProfile(
                    userId = currentUserId,
                    displayName = displayName.trim().ifEmpty { defaultDisplayName },
                    goal = goal,
                    experienceLevel = experienceLevel,
                    equipmentAccess = equipmentAccess,
                    sportFocus = sportFocus.trim().ifEmpty { "General Gym" },
                    dietaryStyle = dietaryStyle,
                    daysPerWeek = daysPerWeek.coerceIn(2, 7),
                    onboardingCompleted = true
                )
                val generated = aiService.generatePersonalizedPlan(draftProfile)
                val finalProfile = draftProfile.copy(
                    dailyCalorieTarget = generated.dailyCalorieTarget,
                    dailyProteinTargetG = generated.dailyProteinTargetG,
                    dailyCarbsTargetG = generated.dailyCarbsTargetG,
                    dailyFatTargetG = generated.dailyFatTargetG
                )
                repository.saveProfile(finalProfile).getOrThrow()
                repository.replaceWeeklyRoutine(generated.days).getOrThrow()
                val firstActiveDay = generated.days.firstOrNull { !it.isRestDay }?.dayNumber ?: 1
                _session.update {
                    it.copy(
                        isGeneratingPlan = false,
                        selectedDayNumber = firstActiveDay,
                        completedSetsByExerciseId = emptyMap(),
                        bannerMessage = "Your personalized routine & macro plan is ready!"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate and save plan", e)
                _session.update {
                    it.copy(
                        isGeneratingPlan = false,
                        errorMessage = e.localizedMessage ?: "Couldn't build your routine right now."
                    )
                }
            }
        }
    }

    fun updateMacroTargets(
        profile: UserProfile,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int
    ) {
        viewModelScope.launch {
            val updated = profile.copy(
                dailyCalorieTarget = calories.coerceIn(1200, 5000),
                dailyProteinTargetG = proteinG.coerceIn(40, 350),
                dailyCarbsTargetG = carbsG.coerceIn(40, 700),
                dailyFatTargetG = fatG.coerceIn(20, 220)
            )
            repository.saveProfile(updated)
                .onSuccess {
                    _session.update { it.copy(bannerMessage = "Daily macro targets updated!") }
                }
                .onFailure { err ->
                    _session.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Could not save targets.")
                    }
                }
        }
    }

    fun toggleSetComplete(exercise: ExercisePrescription) {
        val current = _session.value.completedSetsByExerciseId[exercise.id] ?: 0
        val next = if (current >= exercise.sets) 0 else current + 1
        _session.update {
            it.copy(
                completedSetsByExerciseId = it.completedSetsByExerciseId + (exercise.id to next)
            )
        }
        if (next > current && next <= exercise.sets) {
            startRestTimer(exercise.restSeconds)
        }
    }

    fun adjustExerciseWeight(
        day: WorkoutDay,
        exercise: ExercisePrescription,
        deltaKg: Double
    ) {
        viewModelScope.launch {
            val updatedWeight = (exercise.suggestedWeightKg + deltaKg).coerceIn(0.0, 250.0)
            val updatedExercise = exercise.copy(suggestedWeightKg = updatedWeight)
            val updatedList = day.exercises.map { if (it.id == exercise.id) updatedExercise else it }
            repository.updateWorkoutDayExercises(day, updatedList)
                .onFailure { err ->
                    _session.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Could not save weight")
                    }
                }
        }
    }

    fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        val duration = seconds.coerceIn(5, 300)
        _session.update {
            it.copy(
                restTimerRemainingSec = duration,
                restTimerTotalSec = duration,
                isTimerRunning = true,
                isTimerPaused = false
            )
        }
        launchTimerCountdown()
    }

    fun toggleTimerPause() {
        val current = _session.value
        if (current.restTimerRemainingSec <= 0) return
        if (current.isTimerPaused) {
            _session.update { it.copy(isTimerPaused = false, isTimerRunning = true) }
            launchTimerCountdown()
        } else {
            timerJob?.cancel()
            _session.update { it.copy(isTimerPaused = true, isTimerRunning = false) }
        }
    }

    fun toggleTimerSound() {
        _session.update { it.copy(timerSoundEnabled = !it.timerSoundEnabled) }
    }

    private fun launchTimerCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_session.value.restTimerRemainingSec > 0) {
                delay(1000L)
                val next = (_session.value.restTimerRemainingSec - 1).coerceAtLeast(0)
                _session.update {
                    it.copy(
                        restTimerRemainingSec = next,
                        isTimerRunning = next > 0,
                        isTimerPaused = false,
                        timerCompletedCount = if (next == 0) it.timerCompletedCount + 1 else it.timerCompletedCount
                    )
                }
            }
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _session.update {
            it.copy(
                restTimerRemainingSec = 0,
                restTimerTotalSec = 0,
                isTimerRunning = false,
                isTimerPaused = false
            )
        }
    }

    fun swapExerciseQuick(day: WorkoutDay, exercise: ExercisePrescription) {
        if (exercise.alternativeExercise.isBlank()) return
        viewModelScope.launch {
            val swapped = exercise.copy(
                name = exercise.alternativeExercise,
                setupCue = exercise.alternativeCue.ifBlank { "Control the movement and keep steady posture." },
                alternativeExercise = exercise.name,
                alternativeCue = exercise.setupCue
            )
            val updatedList = day.exercises.map { if (it.id == exercise.id) swapped else it }
            repository.updateWorkoutDayExercises(day, updatedList)
                .onSuccess {
                    _session.update { it.copy(bannerMessage = "Swapped to ${swapped.name}") }
                }
                .onFailure { err ->
                    _session.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to swap exercise") }
                }
        }
    }

    fun swapExerciseWithAi(profile: UserProfile?, day: WorkoutDay, exercise: ExercisePrescription) {
        val activeProfile = profile ?: return
        viewModelScope.launch {
            _session.update { it.copy(isSwappingExerciseId = exercise.id, errorMessage = null) }
            try {
                val newEx = aiService.swapExerciseWithAi(activeProfile, exercise).copy(id = exercise.id)
                val updatedList = day.exercises.map { if (it.id == exercise.id) newEx else it }
                repository.updateWorkoutDayExercises(day, updatedList).getOrThrow()
                _session.update {
                    it.copy(
                        isSwappingExerciseId = null,
                        bannerMessage = "SpotMe swapped in ${newEx.name}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI swap failed", e)
                _session.update {
                    it.copy(
                        isSwappingExerciseId = null,
                        errorMessage = e.localizedMessage ?: "Couldn't generate swap right now."
                    )
                }
            }
        }
    }

    fun logCompletedWorkout(
        day: WorkoutDay,
        feeling: String,
        notes: String
    ) {
        viewModelScope.launch {
            val setsDone = day.exercises.sumOf { ex ->
                _session.value.completedSetsByExerciseId[ex.id] ?: ex.sets
            }
            val exDone = day.exercises.count { ex ->
                (_session.value.completedSetsByExerciseId[ex.id] ?: 0) > 0
            }.let { count -> if (count == 0) day.exercises.size else count }

            val log = WorkoutLog(
                dateKey = todayDateKey,
                dayTitle = day.title,
                durationMinutes = day.estimatedMinutes,
                completedExercises = exDone,
                totalSetsCompleted = setsDone,
                feeling = feeling,
                notes = notes
            )
            repository.addWorkoutLog(log)
                .onSuccess {
                    stopRestTimer()
                    _session.update {
                        it.copy(
                            completedSetsByExerciseId = emptyMap(),
                            bannerMessage = "Session logged! Great consistency today."
                        )
                    }
                }
                .onFailure { err ->
                    _session.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Could not save workout log")
                    }
                }
        }
    }

    fun addMeal(
        mealSlot: String,
        name: String,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        isAiSuggested: Boolean = false
    ) {
        viewModelScope.launch {
            val meal = MealLog(
                dateKey = todayDateKey,
                mealSlot = mealSlot,
                name = name,
                calories = calories,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                isAiSuggested = isAiSuggested
            )
            repository.addMealLog(meal)
                .onSuccess {
                    _session.update {
                        it.copy(
                            mealSuggestion = if (isAiSuggested) null else it.mealSuggestion,
                            bannerMessage = "Logged ${meal.name}"
                        )
                    }
                }
                .onFailure { err ->
                    _session.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Could not log meal")
                    }
                }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            repository.deleteMealLog(mealId)
                .onFailure { err ->
                    _session.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Could not delete meal")
                    }
                }
        }
    }

    fun requestMealSuggestion(
        profile: UserProfile?,
        remainingCalories: Int,
        remainingProteinG: Int,
        mealSlot: String
    ) {
        val activeProfile = profile ?: return
        viewModelScope.launch {
            _session.update { it.copy(isGeneratingMeal = true, errorMessage = null) }
            try {
                val suggestion = aiService.suggestNextMeal(
                    profile = activeProfile,
                    remainingCalories = remainingCalories,
                    remainingProteinG = remainingProteinG,
                    mealSlot = mealSlot
                )
                _session.update {
                    it.copy(isGeneratingMeal = false, mealSuggestion = suggestion)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Meal suggestion failed", e)
                _session.update {
                    it.copy(
                        isGeneratingMeal = false,
                        errorMessage = e.localizedMessage ?: "Could not generate meal idea."
                    )
                }
            }
        }
    }

    fun estimateMealFromDescription(
        profile: UserProfile?,
        description: String,
        mealSlot: String
    ) {
        val trimmed = description.trim()
        if (trimmed.isEmpty()) return
        val dietaryStyle = profile?.dietaryStyle ?: "balanced"
        viewModelScope.launch {
            _session.update { it.copy(isGeneratingMeal = true, errorMessage = null) }
            try {
                val estimated = aiService.estimateMealMacros(
                    description = trimmed,
                    mealSlot = mealSlot,
                    dietaryStyle = dietaryStyle
                )
                _session.update {
                    it.copy(isGeneratingMeal = false, mealSuggestion = estimated)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Meal estimation failed", e)
                _session.update {
                    it.copy(
                        isGeneratingMeal = false,
                        errorMessage = e.localizedMessage ?: "Could not estimate macros right now."
                    )
                }
            }
        }
    }

    fun dismissMealSuggestion() {
        _session.update { it.copy(mealSuggestion = null) }
    }

    fun askBuddy(
        profile: UserProfile?,
        question: String,
        contextTag: String = "",
        history: List<BuddyMessage> = emptyList(),
        workoutContextSummary: String = ""
    ) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _session.value.isAskingBuddy) return
        val activeRole = _session.value.selectedBuddyRole
        viewModelScope.launch {
            _session.update {
                it.copy(isAskingBuddy = true, streamingCoachReply = "", errorMessage = null)
            }
            try {
                repository.addBuddyMessage("user", trimmed, contextTag).getOrThrow()
                var finalAnswer = ""
                aiService.askGymBuddyStream(
                    profile = profile,
                    question = trimmed,
                    contextTag = contextTag,
                    history = history,
                    role = activeRole,
                    workoutContextSummary = workoutContextSummary
                ).collect { partial ->
                    finalAnswer = partial
                    _session.update { it.copy(streamingCoachReply = partial) }
                }
                if (finalAnswer.isNotBlank()) {
                    repository.addBuddyMessage("coach", finalAnswer, contextTag).getOrThrow()
                }
                _session.update { it.copy(isAskingBuddy = false, streamingCoachReply = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ask buddy failed", e)
                _session.update {
                    it.copy(
                        isAskingBuddy = false,
                        streamingCoachReply = null,
                        errorMessage = e.localizedMessage ?: "Couldn't reach SpotMe Coach."
                    )
                }
            }
        }
    }

    fun clearBuddyHistory() {
        viewModelScope.launch {
            repository.clearBuddyChat()
                .onSuccess {
                    _session.update { it.copy(bannerMessage = "Chat cleared") }
                }
                .onFailure { err ->
                    _session.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to clear chat") }
                }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "SpotMeVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
