package com.example.data.ai

import android.util.Log
import com.example.data.model.ExercisePrescription
import com.example.data.model.MealSuggestion
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

data class GeneratedPlan(
    val dailyCalorieTarget: Int,
    val dailyProteinTargetG: Int,
    val dailyCarbsTargetG: Int,
    val dailyFatTargetG: Int,
    val days: List<WorkoutDay>
)

class SpotMeAiService {

    private val planSchema = Schema.obj(
        properties = mapOf(
            "dailyCalorieTarget" to Schema.integer(description = "Recommended daily calories (1200-4500)"),
            "dailyProteinTargetG" to Schema.integer(description = "Daily protein target in grams"),
            "dailyCarbsTargetG" to Schema.integer(description = "Daily carbohydrate target in grams"),
            "dailyFatTargetG" to Schema.integer(description = "Daily fat target in grams"),
            "days" to Schema.array(
                description = "Weekly schedule with 7 days (workout days and recovery/rest days)",
                items = Schema.obj(
                    properties = mapOf(
                        "dayNumber" to Schema.integer(description = "1 to 7"),
                        "title" to Schema.string(description = "Short approachable day title like 'Upper Push & Core' or 'Active Recovery Walk'"),
                        "focusArea" to Schema.string(description = "Muscles or sport stamina focus"),
                        "estimatedMinutes" to Schema.integer(description = "Duration in minutes (15-90)"),
                        "isRestDay" to Schema.boolean(description = "True if rest or light mobility day"),
                        "exercises" to Schema.array(
                            description = "Exercises for this day (4-6 on training days, 0-2 light stretches on rest days)",
                            items = Schema.obj(
                                properties = mapOf(
                                    "name" to Schema.string(description = "Clear exercise name"),
                                    "muscleGroup" to Schema.string(description = "Target muscle group or cardio"),
                                    "sets" to Schema.integer(description = "Recommended number of sets (2-5)"),
                                    "reps" to Schema.string(description = "Repetition or duration target like '10-12' or '30 sec'"),
                                    "restSeconds" to Schema.integer(description = "Rest timer in seconds (45-120)"),
                                    "suggestedWeightKg" to Schema.double(description = "Friendly beginner starting weight in kg (0 for bodyweight)"),
                                    "setupCue" to Schema.string(description = "Plain-English setup & execution step for a first-timer"),
                                    "formMistakeToAvoid" to Schema.string(description = "One common beginner mistake to avoid"),
                                    "alternativeExercise" to Schema.string(description = "Equivalent exercise if machine/space is taken"),
                                    "alternativeCue" to Schema.string(description = "Short cue for the alternative exercise")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private val mealSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Practical, appetizing everyday meal name"),
            "reason" to Schema.string(description = "Why this fits their remaining macros and diet style"),
            "quickPrepNote" to Schema.string(description = "Simple 1-2 sentence prep or ordering guide"),
            "calories" to Schema.integer(description = "Estimated calories"),
            "proteinG" to Schema.integer(description = "Estimated protein grams"),
            "carbsG" to Schema.integer(description = "Estimated carbs grams"),
            "fatG" to Schema.integer(description = "Estimated fat grams")
        )
    )

    private val swapSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Replacement exercise name"),
            "muscleGroup" to Schema.string(description = "Target muscle group"),
            "sets" to Schema.integer(description = "Number of sets"),
            "reps" to Schema.string(description = "Rep range"),
            "restSeconds" to Schema.integer(description = "Rest in seconds"),
            "suggestedWeightKg" to Schema.double(description = "Friendly starting weight in kg"),
            "setupCue" to Schema.string(description = "Plain-English beginner setup & execution cue"),
            "formMistakeToAvoid" to Schema.string(description = "Beginner mistake to avoid"),
            "alternativeExercise" to Schema.string(description = "Another backup option"),
            "alternativeCue" to Schema.string(description = "Short cue for backup")
        )
    )

    private val planModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planSchema
            }
        )
    }

    private val mealModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealSchema
            }
        )
    }

    private val swapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = swapSchema
            }
        )
    }

    private val buddyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are SpotMe, a warm, encouraging, judgment-free gym buddy and sports coach " +
                        "specifically designed to help nervous beginners and everyday athletes. " +
                        "Explain exercises, machine setups, reps, and nutrition in plain everyday language " +
                        "without intimidating bro-science. Keep answers practical, concise (3-6 short bullets or 2 short paragraphs), " +
                        "and include a quick form safety tip when relevant."
                )
            }
        )
    }

    suspend fun generatePersonalizedPlan(profile: UserProfile): GeneratedPlan {
        val prompt = """
            Build a beginner-friendly 7-day personalized workout & nutrition plan for ${profile.displayName}.
            Profile:
            - Primary Goal: ${profile.goal}
            - Experience Level: ${profile.experienceLevel}
            - Equipment Access: ${profile.equipmentAccess}
            - Sport / Focus: ${profile.sportFocus.ifBlank { "General Gym" }}
            - Training Days Per Week: ${profile.daysPerWeek} (include ${profile.daysPerWeek} active training days and ${7 - profile.daysPerWeek} rest/light mobility days so total days = 7)
            - Dietary Style: ${profile.dietaryStyle}
            
            Make every exercise approachable for someone who might feel awkward in the gym:
            - Include crystal-clear setup cues (seat height, grip width, posture)
            - Include 1 common beginner mistake to avoid
            - Include a practical 1-tap alternative exercise if a machine or bench is taken!
        """.trimIndent()

        try {
            val response = withTimeout(TIMEOUT_MS) {
                planModel.generateContent(prompt)
            }
            val jsonString = response.text ?: throw IllegalStateException("AI returned empty routine.")
            val root = JSONObject(jsonString)
            val cal = root.optInt("dailyCalorieTarget", 2200).coerceIn(1200, 4800)
            val pro = root.optInt("dailyProteinTargetG", 130).coerceIn(40, 350)
            val carbs = root.optInt("dailyCarbsTargetG", 240).coerceIn(50, 650)
            val fat = root.optInt("dailyFatTargetG", 70).coerceIn(25, 200)

            val daysArr = root.getJSONArray("days")
            val parsedDays = (0 until daysArr.length().coerceAtMost(7)).map { i ->
                val d = daysArr.getJSONObject(i)
                val exArr = d.optJSONArray("exercises")
                val exercises = if (exArr != null) {
                    (0 until exArr.length().coerceAtMost(12)).map { j ->
                        val ex = exArr.getJSONObject(j)
                        ExercisePrescription(
                            id = UUID.randomUUID().toString().take(8),
                            name = ex.optString("name", "Bodyweight Squat").take(80),
                            muscleGroup = ex.optString("muscleGroup", "Full Body").take(40),
                            sets = ex.optInt("sets", 3).coerceIn(1, 8),
                            reps = ex.optString("reps", "10-12").take(24),
                            restSeconds = ex.optInt("restSeconds", 75).coerceIn(20, 240),
                            suggestedWeightKg = ex.optDouble("suggestedWeightKg", 0.0).coerceIn(0.0, 250.0),
                            setupCue = ex.optString("setupCue", "Move smoothly with controlled breathing."),
                            formMistakeToAvoid = ex.optString("formMistakeToAvoid", "Avoid rushing the reps."),
                            alternativeExercise = ex.optString("alternativeExercise", "Resistance Band Variation"),
                            alternativeCue = ex.optString("alternativeCue", "Use light tension and keep posture tall.")
                        )
                    }
                } else {
                    emptyList()
                }
                WorkoutDay(
                    dayNumber = d.optInt("dayNumber", i + 1).coerceIn(1, 7),
                    title = d.optString("title", "Day ${i + 1}").take(80),
                    focusArea = d.optString("focusArea", "Full Body").take(80),
                    estimatedMinutes = d.optInt("estimatedMinutes", 40).coerceIn(10, 150),
                    isRestDay = d.optBoolean("isRestDay", false),
                    exercises = exercises
                )
            }

            return GeneratedPlan(
                dailyCalorieTarget = cal,
                dailyProteinTargetG = pro,
                dailyCarbsTargetG = carbs,
                dailyFatTargetG = fat,
                days = parsedDays.sortedBy { it.dayNumber }
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Plan generation timed out", e)
            throw IllegalStateException("Building your routine took too long. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Plan generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate personalized routine.")
        }
    }

    suspend fun suggestNextMeal(
        profile: UserProfile,
        remainingCalories: Int,
        remainingProteinG: Int,
        mealSlot: String
    ): MealSuggestion {
        val targetCal = remainingCalories.coerceIn(180, 1200)
        val targetPro = remainingProteinG.coerceIn(10, 80)
        val prompt = """
            Suggest one realistic, everyday, easy-to-make or budget-friendly $mealSlot idea.
            User dietary style: ${profile.dietaryStyle}
            Goal: ${profile.goal}
            Remaining targets for today: ~$targetCal kcal and ~$targetPro g protein.
            Avoid overly complicated gourmet ingredients; make it relatable for a student or working gym beginner.
        """.trimIndent()

        try {
            val response = withTimeout(TIMEOUT_MS) {
                mealModel.generateContent(prompt)
            }
            val jsonString = response.text ?: throw IllegalStateException("AI returned empty meal suggestion.")
            val json = JSONObject(jsonString)
            return MealSuggestion(
                mealSlot = mealSlot,
                name = json.optString("name", "High-Protein Bowl").take(100),
                reason = json.optString("reason", "Fits your protein and calorie goals for today."),
                quickPrepNote = json.optString("quickPrepNote", "Combine protein, complex carbs, and veggies."),
                calories = json.optInt("calories", targetCal).coerceIn(50, 2000),
                proteinG = json.optInt("proteinG", targetPro).coerceIn(1, 150),
                carbsG = json.optInt("carbsG", 45).coerceIn(0, 250),
                fatG = json.optInt("fatG", 12).coerceIn(0, 120)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Meal suggestion timed out", e)
            throw IllegalStateException("Meal suggestion timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Meal suggestion failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to suggest meal.")
        }
    }

    suspend fun swapExerciseWithAi(
        profile: UserProfile,
        current: ExercisePrescription
    ): ExercisePrescription {
        val prompt = """
            The user is at the gym and needs a fresh alternative for "${current.name}" (Muscle group: ${current.muscleGroup}).
            Equipment access: ${profile.equipmentAccess}
            Experience level: ${profile.experienceLevel}
            Suggest a different beginner-friendly exercise hitting the same muscle group, with setup cue and mistake to avoid.
        """.trimIndent()

        try {
            val response = withTimeout(TIMEOUT_MS) {
                swapModel.generateContent(prompt)
            }
            val jsonString = response.text ?: throw IllegalStateException("AI returned empty swap.")
            val ex = JSONObject(jsonString)
            return ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = ex.optString("name", current.alternativeExercise.ifBlank { "Dumbbell Alternative" }).take(80),
                muscleGroup = ex.optString("muscleGroup", current.muscleGroup).take(40),
                sets = ex.optInt("sets", current.sets).coerceIn(1, 8),
                reps = ex.optString("reps", current.reps).take(24),
                restSeconds = ex.optInt("restSeconds", current.restSeconds).coerceIn(20, 240),
                suggestedWeightKg = ex.optDouble("suggestedWeightKg", current.suggestedWeightKg).coerceIn(0.0, 250.0),
                setupCue = ex.optString("setupCue", "Keep posture steady and control each rep."),
                formMistakeToAvoid = ex.optString("formMistakeToAvoid", "Avoid swinging your torso."),
                alternativeExercise = ex.optString("alternativeExercise", current.name),
                alternativeCue = ex.optString("alternativeCue", current.setupCue)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Exercise swap timed out", e)
            throw IllegalStateException("Exercise swap timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exercise swap failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to swap exercise.")
        }
    }

    fun askGymBuddyStream(
        profile: UserProfile?,
        question: String,
        contextTag: String
    ): Flow<String> = flow {
        val contextSummary = if (profile != null) {
            "User Profile: Goal=${profile.goal}, Level=${profile.experienceLevel}, Equipment=${profile.equipmentAccess}, Diet=${profile.dietaryStyle}, Sport=${profile.sportFocus}. "
        } else {
            ""
        }
        val tagInfo = if (contextTag.isNotBlank()) "Current context: $contextTag. " else ""
        val prompt = "$contextSummary$tagInfo\nQuestion from gym beginner: $question"

        val accumulated = StringBuilder()
        try {
            withTimeout(TIMEOUT_MS) {
                buddyModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("SpotMe coach returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gym buddy stream timed out", e)
            throw IllegalStateException("Coach timed out. Please ask again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gym buddy stream failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to reach SpotMe coach.")
        }
    }

    private companion object {
        const val TAG = "SpotMeAiService"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
    }
}
