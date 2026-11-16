package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val goal: String = "build_muscle",
    val experienceLevel: String = "day_one",
    val equipmentAccess: String = "full_gym",
    val sportFocus: String = "General Gym",
    val dietaryStyle: String = "balanced",
    val daysPerWeek: Int = 4,
    val dailyCalorieTarget: Int = 2200,
    val dailyProteinTargetG: Int = 135,
    val dailyCarbsTargetG: Int = 240,
    val dailyFatTargetG: Int = 70,
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExercisePrescription(
    val id: String = "",
    val name: String = "",
    val muscleGroup: String = "",
    val sets: Int = 3,
    val reps: String = "10-12",
    val restSeconds: Int = 75,
    val suggestedWeightKg: Double = 0.0,
    val setupCue: String = "",
    val formMistakeToAvoid: String = "",
    val alternativeExercise: String = "",
    val alternativeCue: String = ""
)

@IgnoreExtraProperties
data class WorkoutDay(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayNumber: Int = 1,
    val title: String = "",
    val focusArea: String = "",
    val estimatedMinutes: Int = 45,
    val isRestDay: Boolean = false,
    val exercises: List<ExercisePrescription> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkoutLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val dayTitle: String = "",
    val durationMinutes: Int = 30,
    val completedExercises: Int = 0,
    val totalSetsCompleted: Int = 0,
    val feeling: String = "good",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MealLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val mealSlot: String = "lunch",
    val name: String = "",
    val calories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val isAiSuggested: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BuddyMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val contextTag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MealSuggestion(
    val mealSlot: String,
    val name: String,
    val reason: String,
    val quickPrepNote: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)
