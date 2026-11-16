package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ExercisePrescription
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.example.data.model.WorkoutLog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotMeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveProfile_andObserveProfile_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SpotMeRepository(firestore, auth)

        val profile = UserProfile(
            userId = uid,
            displayName = "Alice",
            goal = "build_muscle",
            experienceLevel = "day_one",
            equipmentAccess = "full_gym",
            sportFocus = "General Gym",
            dietaryStyle = "balanced",
            daysPerWeek = 4,
            dailyCalorieTarget = 2250,
            dailyProteinTargetG = 140,
            dailyCarbsTargetG = 250,
            dailyFatTargetG = 70,
            onboardingCompleted = true
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveProfile(profile) }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null && it.displayName == "Alice" }
        }
        assertNotNull(observed)
        assertEquals(2250, observed?.dailyCalorieTarget)
    }

    @Test
    fun replaceWeeklyRoutine_andLogWorkoutAndMeal_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SpotMeRepository(firestore, auth)

        val day1 = WorkoutDay(
            dayNumber = 1,
            title = "Upper Push & Core",
            focusArea = "Chest, Shoulders & Triceps",
            estimatedMinutes = 40,
            isRestDay = false,
            exercises = listOf(
                ExercisePrescription(
                    id = "ex_1",
                    name = "Machine Chest Press",
                    muscleGroup = "Chest",
                    sets = 3,
                    reps = "10-12",
                    restSeconds = 75,
                    suggestedWeightKg = 20.0,
                    setupCue = "Adjust seat height so handles align with mid-chest.",
                    formMistakeToAvoid = "Avoid shrugging your shoulders upward.",
                    alternativeExercise = "Dumbbell Floor Press",
                    alternativeCue = "Press smoothly with elbows at 45 degrees."
                )
            )
        )

        val routineRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.replaceWeeklyRoutine(listOf(day1)) }
        assertTrue(routineRes.isSuccess)

        val observedDays = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorkoutDays(uid).first { it.isNotEmpty() }
        }
        assertEquals("Upper Push & Core", observedDays.first().title)

        val mealRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMealLog(
                MealLog(
                    dateKey = "2026-11-15",
                    mealSlot = "lunch",
                    name = "Chicken & Rice Bowl",
                    calories = 540,
                    proteinG = 42,
                    carbsG = 58,
                    fatG = 14,
                    isAiSuggested = false
                )
            )
        }
        assertTrue(mealRes.isSuccess)

        val workoutLogRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addWorkoutLog(
                WorkoutLog(
                    dateKey = "2026-11-15",
                    dayTitle = "Upper Push & Core",
                    durationMinutes = 38,
                    completedExercises = 1,
                    totalSetsCompleted = 3,
                    feeling = "great",
                    notes = "Easy to follow!"
                )
            )
        }
        assertTrue(workoutLogRes.isSuccess)

        val chatRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addBuddyMessage("user", "Should my elbows flare out?", "Machine Chest Press")
        }
        assertTrue(chatRes.isSuccess)
    }

    @Test
    fun observeMealLogs_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = SpotMeRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMealLog(
                MealLog(
                    dateKey = "2026-11-15",
                    mealSlot = "breakfast",
                    name = "Oats & Milk",
                    calories = 350,
                    proteinG = 16,
                    carbsG = 52,
                    fatG = 8
                )
            ).getOrThrow()
        }

        // Sign in as Bob and attempt to read Alice's mealLogs
        signInTestUser(BOB_EMAIL)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMealLogs(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for cross-user read, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    @Test
    fun observeWorkoutDays_unauthenticated_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = SpotMeRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWorkoutDays(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException for unauthenticated read, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_spotme@test.com"
        const val BOB_EMAIL = "bob_spotme@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
