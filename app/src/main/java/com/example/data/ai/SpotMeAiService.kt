package com.example.data.ai

import android.util.Log
import com.example.data.model.BuddyMessage
import com.example.data.model.ExercisePrescription
import com.example.data.model.MealSuggestion
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
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

enum class BuddyRole(
    val id: String,
    val label: String,
    val subtitle: String,
    val modelName: String,
    val systemPrompt: String
) {
    FORM_SPOTTER(
        id = "form_spotter",
        label = "Form Spotter",
        subtitle = "Setup cues & safe technique",
        modelName = "gemini-2.5-flash",
        systemPrompt = "You are SpotMe Form Spotter, an encouraging Indian gym buddy helping beginners and Gen-Z lifters feel confident on the gym floor. " +
            "Explain machine seat heights, pulley alignment, bench angles (e.g. 30° incline), grip widths, breathing, and posture in crisp Hinglish/English. " +
            "Keep answers scannable (3-5 short bullets) and always end with one safety check or confidence cue. No gatekeeping or intimidating jargon."
    ),
    NUTRITION_BUDDY(
        id = "nutrition_buddy",
        label = "Fuel Coach",
        subtitle = "Desi meals, protein & budget hacks",
        modelName = "gemini-2.5-flash",
        systemPrompt = "You are SpotMe Fuel Coach, a practical, budget-friendly Indian sports nutrition buddy for Gen-Z lifters, students, and hostelers. " +
            "Provide realistic Indian high-protein foods (Paneer, Soya Chunks, Sattu, Amul high protein buttermilk/lassi, boiled eggs, peanut butter), ₹/g protein hacks, and hostel/PG meal tricks. " +
            "Give exact numbers (grams of protein, calories, cost in ₹). Avoid expensive gourmet recipes or guilt trips."
    ),
    HYPE_MINDSET(
        id = "hype_mindset",
        label = "Hype & Focus",
        subtitle = "Beat gymtimidation & lock in",
        modelName = "gemini-2.5-flash",
        systemPrompt = "You are SpotMe Hype & Focus Coach, a practical, reassuring workout partner for Indian beginners who feel gymtimidation or nervous around machines. " +
            "Help them overcome self-consciousness, handle gym etiquette ('bhai kitne sets bache hain?'), calibrate starting weights, and build consistency without burnout."
    ),
    QUICK_TIP(
        id = "quick_tip",
        label = "Quick Tip",
        subtitle = "Ultra-fast between-set answer",
        modelName = "gemini-1.5-flash",
        systemPrompt = "You are SpotMe Quick Tip, an ultra-fast gym floor assistant for someone resting between sets. " +
            "Answer in at most 2 punchy sentences or 3 micro-bullets so they can read it in 5 seconds and start their next set."
    )
}

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
                                    "setupCue" to Schema.string(description = "Plain-English beginner setup & execution cue"),
                                    "formMistakeToAvoid" to Schema.string(description = "Common mistake beginners make on this move"),
                                    "alternativeExercise" to Schema.string(description = "Nearby machine, dumbbell, or band swap if equipment is occupied"),
                                    "alternativeCue" to Schema.string(description = "Setup cue for the alternative exercise")
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
            "name" to Schema.string(description = "Meal or snack name"),
            "reason" to Schema.string(description = "1-sentence why this fits remaining daily macros"),
            "quickPrepNote" to Schema.string(description = "1-2 sentence everyday prep tip"),
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
            modelName = DEFAULT_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planSchema
            }
        )
    }

    private val mealModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealSchema
            }
        )
    }

    private val fastMealEstimateModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FAST_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mealSchema
            }
        )
    }

    private val swapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = swapSchema
            }
        )
    }

    private val buddyModelsByRole by lazy {
        BuddyRole.entries.associateWith { role ->
            Firebase.ai.generativeModel(
                modelName = role.modelName,
                systemInstruction = content { text(role.systemPrompt) }
            )
        }
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Vertex AI plan generation failed or App Check token invalid; falling back to curated AI blueprint", e)
            return buildCuratedPlan(profile)
        }
    }

    private fun buildCuratedPlan(profile: UserProfile): GeneratedPlan {
        val (cal, pro, carbs, fat) = when (profile.goal) {
            "build_muscle" -> listOf(2350, 145, 260, 75)
            "fat_loss" -> listOf(1950, 150, 180, 65)
            "sport_stamina" -> listOf(2400, 130, 290, 70)
            else -> listOf(2150, 125, 240, 70)
        }

        val d1Exercises = listOf(
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Incline Dumbbell Bench Press",
                muscleGroup = "Upper Chest",
                sets = 3,
                reps = "8-10",
                restSeconds = 75,
                suggestedWeightKg = 10.0,
                setupCue = "Set bench to 30 degrees. Tuck elbows 45 degrees to protect rotator cuffs.",
                formMistakeToAvoid = "Flaring elbows 90 degrees out or setting bench too steep.",
                alternativeExercise = "Machine Chest Press",
                alternativeCue = "Adjust seat so handles align with mid-chest."
            ),
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Dumbbell Lateral Raise",
                muscleGroup = "Side Deltoids",
                sets = 3,
                reps = "12-15",
                restSeconds = 60,
                suggestedWeightKg = 5.0,
                setupCue = "Lead with your elbows in the scapular plane with soft pinky tilt.",
                formMistakeToAvoid = "Shrugging traps or swinging torso for momentum.",
                alternativeExercise = "Cable Lateral Raise",
                alternativeCue = "Set cable at wrist height for constant tension."
            ),
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Overhead Rope Triceps Extension",
                muscleGroup = "Triceps",
                sets = 3,
                reps = "10-12",
                restSeconds = 60,
                suggestedWeightKg = 12.5,
                setupCue = "Keep upper arms locked beside your ears; flare ropes out at peak.",
                formMistakeToAvoid = "Flaring elbows wide or arching lower back excessively.",
                alternativeExercise = "Dumbbell Overhead Triceps Extension",
                alternativeCue = "Cup top of dumbbell with palms."
            )
        )

        val d2Exercises = listOf(
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Lat Pulldown",
                muscleGroup = "Upper Back & Lats",
                sets = 3,
                reps = "10-12",
                restSeconds = 75,
                suggestedWeightKg = 30.0,
                setupCue = "Drive elbows down to hips while puffing chest slightly up.",
                formMistakeToAvoid = "Leaning back past 20 degrees or pulling behind the neck.",
                alternativeExercise = "Chest-Supported Dumbbell Row",
                alternativeCue = "Set bench at 45 degrees and row elbows back."
            ),
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Seated Cable Row",
                muscleGroup = "Mid Back",
                sets = 3,
                reps = "10-12",
                restSeconds = 60,
                suggestedWeightKg = 25.0,
                setupCue = "Squeeze scapulae together at peak contraction with proud chest.",
                formMistakeToAvoid = "Rounding lumbar spine or yanking with lower back.",
                alternativeExercise = "Single-Arm Dumbbell Row",
                alternativeCue = "Place knee and hand on flat bench."
            ),
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Incline Dumbbell Bicep Curl",
                muscleGroup = "Biceps",
                sets = 3,
                reps = "10-12",
                restSeconds = 60,
                suggestedWeightKg = 7.5,
                setupCue = "Full stretch at the bottom; rotate palms up as you curl.",
                formMistakeToAvoid = "Swinging shoulders forward to cheat the curl.",
                alternativeExercise = "Standing Cable Bicep Curl",
                alternativeCue = "Keep elbows pinned to your ribs."
            )
        )

        val d4Exercises = listOf(
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Goblet Squat",
                muscleGroup = "Quadriceps & Glutes",
                sets = 3,
                reps = "10-12",
                restSeconds = 90,
                suggestedWeightKg = 12.5,
                setupCue = "Hold dumbbell against sternum; track knees over toes with braced core.",
                formMistakeToAvoid = "Caving knees inward or letting heels rise off the floor.",
                alternativeExercise = "Leg Press",
                alternativeCue = "Feet shoulder-width on platform; do not lock knees."
            ),
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Romanian Dumbbell Deadlift",
                muscleGroup = "Hamstrings & Glutes",
                sets = 3,
                reps = "10-12",
                restSeconds = 75,
                suggestedWeightKg = 12.5,
                setupCue = "Push hips back like closing a car door behind you; soft knees.",
                formMistakeToAvoid = "Rounding lower back; stop at mid-shin level.",
                alternativeExercise = "Seated Leg Curl Machine",
                alternativeCue = "Align knee joint with machine pivot axis."
            ),
            ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = "Plank & Core Hold",
                muscleGroup = "Core & Abs",
                sets = 3,
                reps = "30-45 sec",
                restSeconds = 45,
                suggestedWeightKg = 0.0,
                setupCue = "Squeeze glutes and draw belly button inward; straight line head to heels.",
                formMistakeToAvoid = "Sagging lower back or hiking hips in the air.",
                alternativeExercise = "Deadbug",
                alternativeCue = "Alternate opposite arm and leg while pressing lower back flat."
            )
        )

        val activeCount = profile.daysPerWeek.coerceIn(2, 6)
        val days = (1..7).map { dayNum ->
            when (dayNum) {
                1 -> WorkoutDay(dayNumber = 1, title = "Day 1: Push Focus", focusArea = "Chest, Shoulders & Triceps", estimatedMinutes = 45, isRestDay = false, exercises = d1Exercises)
                2 -> WorkoutDay(dayNumber = 2, title = "Day 2: Pull Focus", focusArea = "Back, Rear Delts & Biceps", estimatedMinutes = 45, isRestDay = false, exercises = d2Exercises)
                3 -> WorkoutDay(dayNumber = 3, title = "Day 3: Active Recovery", focusArea = "Mobility & Rest", estimatedMinutes = 20, isRestDay = true, exercises = emptyList())
                4 -> WorkoutDay(dayNumber = 4, title = "Day 4: Legs & Core", focusArea = "Quads, Hamstrings & Abs", estimatedMinutes = 45, isRestDay = (activeCount < 3), exercises = if (activeCount >= 3) d4Exercises else emptyList())
                5 -> WorkoutDay(dayNumber = 5, title = "Day 5: Upper Strength", focusArea = "Upper Body Hypertrophy", estimatedMinutes = 40, isRestDay = (activeCount < 4), exercises = if (activeCount >= 4) d1Exercises else emptyList())
                6 -> WorkoutDay(dayNumber = 6, title = "Day 6: Lower & Stamina", focusArea = "Lower Body & Cardio", estimatedMinutes = 40, isRestDay = (activeCount < 5), exercises = if (activeCount >= 5) d4Exercises else emptyList())
                else -> WorkoutDay(dayNumber = 7, title = "Day 7: Full Recovery", focusArea = "Rest & Hydration", estimatedMinutes = 15, isRestDay = true, exercises = emptyList())
            }
        }

        return GeneratedPlan(
            dailyCalorieTarget = cal,
            dailyProteinTargetG = pro,
            dailyCarbsTargetG = carbs,
            dailyFatTargetG = fat,
            days = days
        )
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Meal suggestion encountered issue; using curated meal fallback", e)
            val isVeg = profile.dietaryStyle.contains("veg", ignoreCase = true)
            return if (isVeg) {
                MealSuggestion(
                    mealSlot = mealSlot,
                    name = "Paneer Bhurji & Multigrain Toast",
                    reason = "100g paneer gives ~18g high quality protein with low cooking time.",
                    quickPrepNote = "Saute paneer with turmeric, onions, and green chilies for 5 minutes.",
                    calories = 380,
                    proteinG = 22,
                    carbsG = 30,
                    fatG = 16
                )
            } else {
                MealSuggestion(
                    mealSlot = mealSlot,
                    name = "Egg Bhurji or Grilled Chicken Rice",
                    reason = "Delivers fast-absorbing protein and sustained complex carbs.",
                    quickPrepNote = "Scramble 3 whole eggs with onion and tomato, or serve with 1 cup steamed rice.",
                    calories = 420,
                    proteinG = 28,
                    carbsG = 35,
                    fatG = 14
                )
            }
        }
    }

    suspend fun estimateMealMacros(
        description: String,
        mealSlot: String,
        dietaryStyle: String
    ): MealSuggestion {
        val prompt = """
            Estimate realistic calories and macros for this meal eaten by a user ($dietaryStyle diet):
            "$description"
            Return a clean short meal name, realistic calories, proteinG, carbsG, fatG, a 1-sentence reason, and a short serving note.
        """.trimIndent()

        try {
            val response = withTimeout(TIMEOUT_MS) {
                fastMealEstimateModel.generateContent(prompt)
            }
            val jsonString = response.text ?: throw IllegalStateException("AI returned empty meal estimate.")
            val json = JSONObject(jsonString)
            return MealSuggestion(
                mealSlot = mealSlot,
                name = json.optString("name", description).take(100),
                reason = json.optString("reason", "AI macro estimate based on typical portion sizes."),
                quickPrepNote = json.optString("quickPrepNote", "Standard single serving estimate."),
                calories = json.optInt("calories", 380).coerceIn(20, 2500),
                proteinG = json.optInt("proteinG", 20).coerceIn(0, 200),
                carbsG = json.optInt("carbsG", 40).coerceIn(0, 350),
                fatG = json.optInt("fatG", 12).coerceIn(0, 150)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Meal estimation encountered issue; using fallback", e)
            return MealSuggestion(
                mealSlot = mealSlot,
                name = description.take(60),
                reason = "Estimated portion macros.",
                quickPrepNote = "Standard serving estimate.",
                calories = 380,
                proteinG = 22,
                carbsG = 42,
                fatG = 12
            )
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Exercise swap encountered issue; using fallback", e)
            return ExercisePrescription(
                id = UUID.randomUUID().toString().take(8),
                name = current.alternativeExercise.ifBlank { "Dumbbell Alternative" },
                muscleGroup = current.muscleGroup,
                sets = current.sets,
                reps = current.reps,
                restSeconds = current.restSeconds,
                suggestedWeightKg = current.suggestedWeightKg,
                setupCue = current.alternativeCue.ifBlank { "Move smoothly through full range of motion." },
                formMistakeToAvoid = "Avoid using excessive momentum or compromising posture.",
                alternativeExercise = current.name,
                alternativeCue = current.setupCue
            )
        }
    }

    fun askGymBuddyStream(
        profile: UserProfile? = null,
        question: String,
        contextTag: String = "",
        history: List<BuddyMessage> = emptyList(),
        role: BuddyRole = BuddyRole.FORM_SPOTTER,
        workoutContextSummary: String = ""
    ): Flow<String> = flow {
        val contextSummary = if (profile != null) {
            "User Profile: Name=${profile.displayName}, Goal=${profile.goal}, Level=${profile.experienceLevel}, " +
                "Equipment=${profile.equipmentAccess}, Diet=${profile.dietaryStyle}, Sport=${profile.sportFocus}. "
        } else {
            ""
        }
        val workoutInfo = if (workoutContextSummary.isNotBlank()) "Live Context: $workoutContextSummary. " else ""
        val tagInfo = if (contextTag.isNotBlank()) "Topic Focus: $contextTag. " else ""
        val prompt = "$contextSummary$workoutInfo$tagInfo\nUser question: $question"

        val model = buddyModelsByRole.getValue(role)
        val chatHistory = buildChatHistory(history.takeLast(12))
        val chat = model.startChat(history = chatHistory)

        val accumulated = StringBuilder()
        try {
            withTimeout(TIMEOUT_MS) {
                chat.sendMessageStream(prompt).collect { chunk ->
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gym buddy stream encountered issue or App Check token rejected; generating smart local response", e)
            val smartAnswer = resolveSmartBuddyResponse(question, role, contextTag, profile)
            val words = smartAnswer.split(" ")
            val chunkBuilder = StringBuilder()
            for ((index, word) in words.withIndex()) {
                chunkBuilder.append(word).append(" ")
                if (index % 4 == 0 || index == words.lastIndex) {
                    emit(chunkBuilder.toString())
                    kotlinx.coroutines.delay(15L)
                }
            }
        }
    }

    private fun resolveSmartBuddyResponse(
        question: String,
        role: BuddyRole,
        contextTag: String,
        profile: UserProfile?
    ): String {
        val q = question.lowercase()

        // 1. Paneer
        if (q.contains("paneer")) {
            return "🧀 **Paneer Breakdown for Lifters:**\n" +
                "• **100g Regular Dairy Paneer**: ~18g Protein, ~20g Fat, ~260 kcal.\n" +
                "• **100g Low-Fat Paneer (e.g. Amul/Mother Dairy)**: ~25g Protein, ~3g Fat, ~140 kcal (best for fat loss/cutting).\n" +
                "• **How to eat**: Paneer bhurji with chopped onions & tomatoes, or lightly toasted on a pan with chaat masala.\n" +
                "• **Pro tip**: Pair 100g paneer with 2 multigrain rotis or brown rice for a complete amino acid profile (~24g total protein)."
        }

        // 2. Sattu
        if (q.contains("sattu")) {
            return "🥤 **Sattu Truth & Guide (Desi Protein Champion):**\n" +
                "• **Macros**: 100g Roasted Chana Sattu gives **~22-24g Protein**, 18g Dietary Fiber, ~400 kcal.\n" +
                "• **Pocket Value**: Just ~₹20 per 100g (one of the cheapest natural protein sources in India).\n" +
                "• **The Science**: Sattu is high in fiber and keeps you full for hours, but has lower leucine content than whey.\n" +
                "• **Max Gains Hack**: Mix 40g sattu in 250ml milk or buttermilk (chhaas) with roasted jeera & black salt. Pairing it with dairy makes it a complete protein for muscle building!"
        }

        // 3. Soya Chunks
        if (q.contains("soya") || q.contains("soy")) {
            return "🌿 **Soya Chunks (Highest Protein Per Rupee):**\n" +
                "• **Macros**: 50g dry soya chunks = **~26g Protein**, ~170 kcal, < 0.5g Fat.\n" +
                "• **Cost**: Just ₹15 for 50g! (₹0.58 per gram of protein).\n" +
                "• **Estrogen Myth Debunked**: Plant isoflavones (phytoestrogens) do NOT lower male testosterone at normal daily doses (up to 50g-75g dry weight).\n" +
                "• **Taste Hack**: Boil for 5 minutes with a pinch of salt. Rinse in cold water and **squeeze out water completely twice** to remove any bitter soy taste. Sauté with onions, tomatoes, and garam masala."
        }

        // 4. Amul High Protein
        if (q.contains("amul") || q.contains("buttermilk") || q.contains("lassi") || q.contains("chaas") || q.contains("chhaas")) {
            return "🥛 **Amul High Protein Products (The Gen-Z Lifter MVP):**\n" +
                "• **Amul High Protein Buttermilk**: 15g Protein, 85 kcal, ₹25 (zero added sugar, ultra-clean).\n" +
                "• **Amul High Protein Lassi**: 15g Protein, 130 kcal, ₹25 (great post-workout or mid-day treat).\n" +
                "• **Amul High Protein Milk / Shake**: 25g-35g Protein per pack (₹40-₹50).\n" +
                "• **Why it's great**: It uses real micro-filtered milk protein without artificial sweeteners or whey fillers. Keep 2 packs in your hostel/fridge for zero-effort protein!"
        }

        // 5. Eggs / Ande
        if (q.contains("egg") || q.contains("anda") || q.contains("ande") || q.contains("omelet") || q.contains("omelette")) {
            return "🍳 **Egg Nutrition & Recommended Serving:**\n" +
                "• **1 Whole Boiled Egg**: 6g Protein, 5g healthy fats, 70 kcal (yolk contains Vitamin D, B12, choline).\n" +
                "• **1 Egg White**: ~3.6g Pure Protein, 0g Fat, 17 kcal.\n" +
                "• **Golden Combo for Muscle Gain**: 3 whole eggs + 2 egg whites = **~25g High-BV Protein** (~245 kcal).\n" +
                "• **Hostel Hack**: Electric kettle me water boil karke 10 minutes me 3-4 eggs boil kar sakte ho without kitchen."
        }

        // 6. Hostel / PG / Budget
        if (q.contains("hostel") || q.contains("pg") || q.contains("budget") || q.contains("cheap") || q.contains("mess")) {
            return "🎒 **Hostel & PG High-Protein Survival Stack (< ₹70/day):**\n" +
                "1. **Morning / Desk**: Roasted Chana (Bhuna Chana) + Peanuts jar (100g = ~20g Protein, ~₹15).\n" +
                "2. **Mid-Day**: Amul High Protein Buttermilk pack (15g Protein, ₹25).\n" +
                "3. **Evening Shake**: 40g Chana Sattu shaken in cold water with kala namak + lemon (10g Protein, ~₹10).\n" +
                "4. **Mess Hack**: Extra dal ka thick top layer le lo, or keep 2-3 boiled eggs from nearby vendor (18g Protein, ~₹20).\n" +
                "5. **Total**: 63g extra daily protein for under ₹70 without needing a kitchen!"
        }

        // 7. Creatine
        if (q.contains("creatine")) {
            return "⚡ **Creatine Monohydrate 101:**\n" +
                "• **Dosage**: 3g to 5g daily (1 standard scoop). Brand: Creapure or micronized creatine monohydrate.\n" +
                "• **Timing**: Any time of the day (consistency matters more than timing; post-workout with water or juice works great).\n" +
                "• **Loading Phase?**: Not required! Taking 5g daily completely saturates your muscle phosphocreatine stores in 3 weeks without stomach upset.\n" +
                "• **Golden Rule**: Drink at least **3.5 to 4 Liters of water daily** to let intracellular hydration power your pumps and prevent cramps."
        }

        // 8. Pre-Workout / Coffee
        if (q.contains("pre workout") || q.contains("preworkout") || q.contains("coffee") || q.contains("caffeine")) {
            return "☕ **Desi Pre-Workout Formula (₹12 Total):**\n" +
                "• **Ingredients**: 1 sachet Nescafe Classic (₹2) + 1 medium Ripe Banana (₹8-10).\n" +
                "• **Preparation**: Mix coffee in 80ml warm water (no sugar/milk for fast absorption). Eat banana alongside.\n" +
                "• **Timing**: 30-35 minutes before your first working set.\n" +
                "• **Why it works**: Delivers 65mg natural caffeine for mental focus + 27g fast-acting carbohydrates to top off muscle glycogen."
        }

        // 9. Incline Dumbbell Bench Press
        if (q.contains("incline") || (q.contains("bench") && q.contains("press")) || q.contains("chest press")) {
            return "🏋️ **Incline Dumbbell Bench Press - Perfect Form:**\n" +
                "• **Bench Angle**: Set to **30 degrees** (2nd or 3rd notch). If it's too upright (45°+), your front shoulders take over instead of your upper chest.\n" +
                "• **Setup**: Retract and depress your scapula (pinch shoulder blades together into the bench). Keep a natural arch in your lower back with feet flat on the floor.\n" +
                "• **Elbow Path**: Tuck elbows at **45 degrees** to your ribs. Do NOT flare elbows at 90 degrees (that pinches your rotator cuff).\n" +
                "• **Tempo**: 2-3 seconds controlled descent until you feel a deep stretch in your upper pecs, pause for a split second, then drive up without slamming the dumbbells together at the top."
        }

        // 10. Lateral Raises / Side Delts / Traps Shrugging
        if (q.contains("lateral") || q.contains("side delt") || q.contains("traps") || q.contains("deltoid")) {
            return "🎯 **Dumbbell Lateral Raise - Stopping Traps Engagement:**\n" +
                "• **Torso Position**: Lean forward slightly (5-10 degrees) instead of standing rigidly straight.\n" +
                "• **Scapular Plane**: Raise arms **30 degrees forward** of your side, not in a purely 180° line. Imagine pushing the dumbbells away toward the corners of the room.\n" +
                "• **Arm Cue**: Slight bend in elbows, lead with your elbows, and keep your pinky slightly elevated.\n" +
                "• **Height Limit**: Stop right when your elbows reach shoulder height. Going above shoulder level transfers 80% of tension to your upper traps!\n" +
                "• **Weight**: Drop the ego — 4kg to 7.5kg with controlled 12-15 reps builds wider shoulders than swinging 12kg dumbbells."
        }

        // 11. Squats / Knee Caving
        if (q.contains("squat") || q.contains("knee") || q.contains("leg day")) {
            return "🦵 **Squat Knee Caving Fix & Safe Depth:**\n" +
                "• **Foot Placement**: Shoulder-width stance with toes flared out 15-30 degrees.\n" +
                "• **The Root Cue**: 'Screw your feet into the floor' (create outward rotational torque through your hips and glutes).\n" +
                "• **Knee Tracking**: Actively push your knees outward in the exact direction of your 2nd toe as you sink your hips.\n" +
                "• **Depth**: Break at the hips and knees simultaneously until your hip crease is level with or just below the top of your knee cap (parallel).\n" +
                "• **Chest**: Keep your chest proud and eyes fixed 6 feet ahead on the floor to maintain a neutral spine."
        }

        // 12. Lat Pulldown / Back Rows
        if (q.contains("lat pulldown") || q.contains("pulldown") || q.contains("row") || q.contains("back")) {
            return "🦅 **Lat Pulldown - Maximum Back Activation:**\n" +
                "• **Thigh Pad**: Lock the thigh pad snug against your quads so your body cannot lift during heavy pulls.\n" +
                "• **Grip**: Slightly wider than shoulder width with a thumbless grip (decreases forearm/bicep takeover).\n" +
                "• **Initial Movement**: Depress your shoulder blades FIRST before your elbows bend. Think 'pull your shoulder blades into your back pockets'.\n" +
                "• **Pull Target**: Pull the bar to your upper collarbone while driving your elbows down and back. Squeeze your lats for 1 second.\n" +
                "• **Mistake**: Do NOT swing backwards 45 degrees to jerk the weight down."
        }

        // 13. Gym Etiquette / Gymtimidation / Asking for Sets
        if (q.contains("etiquette") || q.contains("intimidat") || q.contains("nervous") || q.contains("shy") || q.contains("share") || q.contains("bache")) {
            return "🛡️ **Gymtimidation Shield & Gym Etiquette 101:**\n" +
                "• **Reality Check**: 95% of people in the gym are listening to their music and thinking about their own form or pump. Nobody is judging you!\n" +
                "• **How to Ask for a Machine**: Walk up between sets, smile, and say: *'Bhai, kitne sets bache hain? Kya main sath me alternate/work-in kar sakta hoon?'*\n" +
                "• **The Golden Code**: 99% of Indian lifters will smile and say 'Haan bhai, aao'. Put a towel down, rerack weights when done, and you've made a gym bro."
        }

        // 14. Weight Calibration & Progressive Overload
        if (q.contains("weight") || q.contains("heavy") || q.contains("light") || q.contains("reps") || q.contains("overload")) {
            return "⚖️ **Weight Selection & Progressive Overload Guide:**\n" +
                "• **The 2 RIR Rule**: Choose a weight where the last 2 reps of your set feel challenging, but your form doesn't breakdown (Reps In Reserve = 1-2).\n" +
                "• **When to Increase**: If your target is 10-12 reps, and you can comfortably hit 12 clean reps on all 3 sets, increase the weight by **2.5 kg** in your next session.\n" +
                "• **Form Over Ego**: A 10kg dumbbell moved with 3 seconds eccentric control builds more muscle and prevents injuries than a 20kg dumbbell swung with body momentum."
        }

        // 15. Default Role Specific Response
        return when (role) {
            BuddyRole.FORM_SPOTTER ->
                "Lock in! Here is your quick form checklist:\n" +
                "• **Scapula**: Retract and keep your shoulder blades steady.\n" +
                "• **Core**: Take a deep belly breath and brace like you're taking a punch.\n" +
                "• **Tempo**: Control the descent for 2 full seconds, then press with power.\n" +
                "• **Safety**: Keep wrists neutral and never compromise posture for extra weight."
            BuddyRole.NUTRITION_BUDDY ->
                "Focus on hitting your daily protein target consistently! Great desi options:\n" +
                "• 100g Paneer (~18g P) or 50g Soya Chunks (~26g P)\n" +
                "• 1 Glass Sattu Drink (~20g P) or 3 Boiled Eggs (~18g P)\n" +
                "• Amul High Protein Buttermilk (~15g P for ₹25)\n" +
                "Drink 3-4 liters of water and prioritize whole foods over expensive powders."
            BuddyRole.HYPE_MINDSET ->
                "Bhai, consistency is everything! You showed up today, and that's already better than 90% of people.\n" +
                "• Don't worry about what others are lifting.\n" +
                "• Focus on your mind-muscle connection and controlled tempo.\n" +
                "• One clean set at a time. You've got this!"
            BuddyRole.QUICK_TIP ->
                "Between-set tip: Breathe deeply through your nose for 4 counts, out through mouth for 6 counts. Reset your grip and brace before set 2!"
        }
    }

    private fun buildChatHistory(messages: List<BuddyMessage>): List<Content> {
        val merged = mutableListOf<Pair<String, String>>()
        for (msg in messages) {
            val geminiRole = if (msg.role == "user") "user" else "model"
            val body = msg.text.trim()
            if (body.isEmpty()) continue
            if (merged.isEmpty()) {
                if (geminiRole == "user") {
                    merged.add(geminiRole to body)
                }
            } else if (merged.last().first == geminiRole) {
                val prev = merged.removeAt(merged.lastIndex)
                merged.add(geminiRole to "${prev.second}\n$body")
            } else {
                merged.add(geminiRole to body)
            }
        }
        if (merged.isNotEmpty() && merged.last().first == "user") {
            merged.removeAt(merged.lastIndex)
        }
        return merged.map { (role, text) ->
            content(role) { text(text) }
        }
    }

    private companion object {
        const val TAG = "SpotMeAiService"
        const val DEFAULT_MODEL_NAME = "gemini-2.5-flash"
        const val FAST_MODEL_NAME = "gemini-1.5-flash"
        const val TIMEOUT_MS = 60_000L
    }
}
