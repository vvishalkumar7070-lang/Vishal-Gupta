package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BuddyMessage
import com.example.data.model.ExercisePrescription
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.example.data.model.WorkoutLog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SpotMeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                }
        )
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }

        val clampedDays = profile.daysPerWeek.coerceIn(2, 7)
        val clampedCalories = profile.dailyCalorieTarget.coerceIn(1000, 6000)
        val clampedProtein = profile.dailyProteinTargetG.coerceIn(30, 400)
        val clampedCarbs = profile.dailyCarbsTargetG.coerceIn(30, 800)
        val clampedFat = profile.dailyFatTargetG.coerceIn(20, 250)
        val safeName = profile.displayName.trim().ifEmpty { "Athlete" }.take(80)
        val safeSport = profile.sportFocus.trim().take(60)

        try {
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "displayName" to safeName,
                    "goal" to profile.goal,
                    "experienceLevel" to profile.experienceLevel,
                    "equipmentAccess" to profile.equipmentAccess,
                    "sportFocus" to safeSport,
                    "dietaryStyle" to profile.dietaryStyle,
                    "daysPerWeek" to clampedDays,
                    "dailyCalorieTarget" to clampedCalories,
                    "dailyProteinTargetG" to clampedProtein,
                    "dailyCarbsTargetG" to clampedCarbs,
                    "dailyFatTargetG" to clampedFat,
                    "onboardingCompleted" to profile.onboardingCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "goal" to profile.goal,
                    "experienceLevel" to profile.experienceLevel,
                    "equipmentAccess" to profile.equipmentAccess,
                    "sportFocus" to safeSport,
                    "dietaryStyle" to profile.dietaryStyle,
                    "daysPerWeek" to clampedDays,
                    "dailyCalorieTarget" to clampedCalories,
                    "dailyProteinTargetG" to clampedProtein,
                    "dailyCarbsTargetG" to clampedCarbs,
                    "dailyFatTargetG" to clampedFat,
                    "onboardingCompleted" to profile.onboardingCompleted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeWorkoutDays(userId: String): Flow<List<WorkoutDay>> = flow {
        val colRef = db.collection("users").document(userId).collection("workoutDays")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutDay::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.dayNumber }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                }
        )
    }

    suspend fun replaceWeeklyRoutine(days: List<WorkoutDay>): Result<Unit> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("workoutDays")
        try {
            val existingDocs = colRef.get().await()
            val batch = db.batch()
            existingDocs.documents.forEach { doc -> batch.delete(doc.reference) }
            days.take(7).forEachIndexed { idx, day ->
                val dayNum = day.dayNumber.coerceIn(1, 7)
                val docRef = colRef.document("day_$dayNum")
                val payload = mapOf(
                    "userId" to uid,
                    "dayNumber" to dayNum,
                    "title" to day.title.trim().ifEmpty { "Day $dayNum" }.take(80),
                    "focusArea" to day.focusArea.trim().ifEmpty { "Full Body" }.take(80),
                    "estimatedMinutes" to day.estimatedMinutes.coerceIn(5, 180),
                    "isRestDay" to day.isRestDay,
                    "exercises" to day.exercises.take(15).map { it.toMap() },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            throw e
        }
    }

    suspend fun updateWorkoutDayExercises(
        day: WorkoutDay,
        updatedExercises: List<ExercisePrescription>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(day.id.isNotBlank()) { "WorkoutDay id must not be blank" }
        val docRef = db.collection("users").document(uid)
            .collection("workoutDays").document(day.id)
        try {
            docRef.update(
                mapOf(
                    "exercises" to updatedExercises.take(15).map { it.toMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeWorkoutLogs(userId: String): Flow<List<WorkoutLog>> = flow {
        val colRef = db.collection("users").document(userId).collection("workoutLogs")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WorkoutLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                }
        )
    }

    suspend fun addWorkoutLog(log: WorkoutLog): Result<String> = runCatching {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("workoutLogs").document(id)
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to log.dateKey,
            "dayTitle" to log.dayTitle.trim().ifEmpty { "Workout Session" }.take(80),
            "durationMinutes" to log.durationMinutes.coerceIn(1, 300),
            "completedExercises" to log.completedExercises.coerceIn(0, 30),
            "totalSetsCompleted" to log.totalSetsCompleted.coerceIn(0, 150),
            "feeling" to log.feeling,
            "notes" to log.notes.trim().take(400),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeMealLogs(userId: String): Flow<List<MealLog>> = flow {
        val colRef = db.collection("users").document(userId).collection("mealLogs")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MealLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                }
        )
    }

    suspend fun addMealLog(meal: MealLog): Result<String> = runCatching {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("mealLogs").document(id)
        val payload = mapOf(
            "userId" to uid,
            "dateKey" to meal.dateKey,
            "mealSlot" to meal.mealSlot,
            "name" to meal.name.trim().ifEmpty { "Meal" }.take(100),
            "calories" to meal.calories.coerceIn(0, 4000),
            "proteinG" to meal.proteinG.coerceIn(0, 300),
            "carbsG" to meal.carbsG.coerceIn(0, 500),
            "fatG" to meal.fatG.coerceIn(0, 250),
            "isAiSuggested" to meal.isAiSuggested,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMealLog(mealId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(mealId.isNotBlank()) { "Meal id must not be blank" }
        val docRef = db.collection("users").document(uid).collection("mealLogs").document(mealId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeBuddyMessages(userId: String): Flow<List<BuddyMessage>> = flow {
        val colRef = db.collection("users").document(userId).collection("buddyChats")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BuddyMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, userId)
                }
        )
    }

    suspend fun addBuddyMessage(
        role: String,
        text: String,
        contextTag: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("buddyChats").document(id)
        val payload = mapOf(
            "userId" to uid,
            "role" to role,
            "text" to text.trim().ifEmpty { "..." }.take(4000),
            "contextTag" to contextTag.trim().take(80),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun clearBuddyChat(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("buddyChats")
        try {
            val docs = colRef.get().await()
            val batch = db.batch()
            docs.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            throw e
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        userId: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    private fun ExercisePrescription.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "muscleGroup" to muscleGroup,
        "sets" to sets,
        "reps" to reps,
        "restSeconds" to restSeconds,
        "suggestedWeightKg" to suggestedWeightKg,
        "setupCue" to setupCue,
        "formMistakeToAvoid" to formMistakeToAvoid,
        "alternativeExercise" to alternativeExercise,
        "alternativeCue" to alternativeCue
    )
}
