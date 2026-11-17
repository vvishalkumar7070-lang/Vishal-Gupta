package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.HomeWorkoutBg
import com.example.ui.theme.HomeWorkoutCard
import com.example.ui.theme.HomeWorkoutCardBorder
import com.example.ui.theme.HomeWorkoutCardSelected
import com.example.ui.theme.HomeWorkoutCyan
import com.example.ui.theme.HomeWorkoutTextMuted
import com.example.ui.theme.HomeWorkoutTextPrimary
import com.example.ui.theme.HomeWorkoutTextSecondary
import com.example.ui.theme.HomeWorkoutVolt
import kotlinx.coroutines.delay

private const val TOTAL_ONBOARDING_STEPS = 7

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    existingProfile: UserProfile?,
    defaultName: String,
    isGenerating: Boolean,
    errorMessage: String?,
    onBack: (() -> Unit)? = null,
    onGeneratePlan: (
        displayName: String,
        goal: String,
        experienceLevel: String,
        equipmentAccess: String,
        sportFocus: String,
        dietaryStyle: String,
        daysPerWeek: Int
    ) -> Unit
) {
    // Current step in wizard: 1 through 7
    var currentStep by remember { mutableIntStateOf(1) }

    // User response state
    var selectedGender by remember { mutableStateOf("male") }
    var selectedGoal by remember(existingProfile) {
        mutableStateOf(existingProfile?.goal ?: "fat_loss")
    }
    var selectedFocusAreas by remember {
        mutableStateOf(setOf("Full Body", "Chest & Arms"))
    }
    var pushupBenchmark by remember { mutableStateOf("intermediate") }
    var selectedEquipment by remember(existingProfile) {
        mutableStateOf(existingProfile?.equipmentAccess ?: "home_bodyweight")
    }
    var selectedDaysPerWeek by remember(existingProfile) {
        mutableIntStateOf(existingProfile?.daysPerWeek ?: 4)
    }
    var athleteName by remember(existingProfile) {
        mutableStateOf(existingProfile?.displayName?.ifBlank { defaultName } ?: defaultName)
    }
    var selectedDietStyle by remember(existingProfile) {
        mutableStateOf(existingProfile?.dietaryStyle ?: "balanced")
    }
    var weightInput by remember { mutableStateOf("72") }
    var heightInput by remember { mutableStateOf("175") }

    val progress = (currentStep.toFloat() / TOTAL_ONBOARDING_STEPS.toFloat()).coerceIn(0f, 1f)

    if (isGenerating) {
        PlanSynthesisLoadingView(
            athleteName = athleteName,
            goal = selectedGoal
        )
        return
    }

    Scaffold(
        containerColor = HomeWorkoutBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        IconButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Step",
                                tint = HomeWorkoutTextPrimary
                            )
                        }
                    } else if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = HomeWorkoutTextPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Text(
                        text = "STEP $currentStep OF $TOTAL_ONBOARDING_STEPS",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = HomeWorkoutVolt
                    )

                    if (currentStep < TOTAL_ONBOARDING_STEPS) {
                        Text(
                            text = "SKIP",
                            modifier = Modifier
                                .clickable { currentStep++ }
                                .padding(8.dp),
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = HomeWorkoutTextMuted
                        )
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = HomeWorkoutVolt,
                    trackColor = HomeWorkoutCardBorder
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeWorkoutBg)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (currentStep < TOTAL_ONBOARDING_STEPS) {
                            currentStep++
                        } else {
                            val sportTag = selectedFocusAreas.joinToString(", ").ifBlank { "Full Body" }
                            val mappedExperience = when (pushupBenchmark) {
                                "beginner" -> "day_one"
                                "advanced" -> "intermediate"
                                else -> "returning"
                            }
                            onGeneratePlan(
                                athleteName.trim().ifEmpty { defaultName },
                                selectedGoal,
                                mappedExperience,
                                selectedEquipment,
                                sportTag,
                                selectedDietStyle,
                                selectedDaysPerWeek
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_primary_cta"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeWorkoutVolt,
                        contentColor = Color(0xFF0D0E12)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentStep == TOTAL_ONBOARDING_STEPS) "BUILD MY 28-DAY PLAN" else "CONTINUE",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentStep == TOTAL_ONBOARDING_STEPS) Icons.Filled.AutoAwesome else Icons.Filled.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .widthIn(max = 580.dp)
                    .align(Alignment.TopCenter)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFF261214),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF453A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF9F9C),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "OnboardingStepAnimation"
                ) { step ->
                    when (step) {
                        1 -> StepTargetProfile(
                            selectedGender = selectedGender,
                            onSelect = { selectedGender = it }
                        )
                        2 -> StepPrimaryGoal(
                            selectedGoal = selectedGoal,
                            onSelect = { selectedGoal = it }
                        )
                        3 -> StepFocusAreas(
                            selectedAreas = selectedFocusAreas,
                            onToggle = { area ->
                                selectedFocusAreas = if (selectedFocusAreas.contains(area)) {
                                    if (selectedFocusAreas.size > 1) selectedFocusAreas - area else selectedFocusAreas
                                } else {
                                    selectedFocusAreas + area
                                }
                            }
                        )
                        4 -> StepStrengthBenchmark(
                            selectedLevel = pushupBenchmark,
                            onSelect = { pushupBenchmark = it }
                        )
                        5 -> StepEquipment(
                            selectedEquip = selectedEquipment,
                            onSelect = { selectedEquipment = it }
                        )
                        6 -> StepWeeklySchedule(
                            selectedDays = selectedDaysPerWeek,
                            onSelect = { selectedDaysPerWeek = it }
                        )
                        7 -> StepBiometricsAndName(
                            name = athleteName,
                            onNameChange = { athleteName = it },
                            weight = weightInput,
                            onWeightChange = { weightInput = it },
                            height = heightInput,
                            onHeightChange = { heightInput = it },
                            diet = selectedDietStyle,
                            onDietChange = { selectedDietStyle = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: Target Biological Profile
// -------------------------------------------------------------
@Composable
private fun StepTargetProfile(
    selectedGender: String,
    onSelect: (String) -> Unit
) {
    StepHeader(
        eyebrow = "CUSTOMIZE PHYSIQUE",
        title = "WHO IS THIS PLAN FOR?",
        subtitle = "We tailor metabolic recovery speed, hormonal curves, and default movement patterns to your profile."
    )

    Spacer(modifier = Modifier.height(20.dp))

    val options = listOf(
        Triple("male", "MALE", "Focus on upper-body hypertrophy, V-taper delts, and core density."),
        Triple("female", "FEMALE", "Focus on glute activation, waist taper, and lean muscle tone."),
        Triple("neutral", "ALL-ROUND ATHLETE", "Balanced functional conditioning, mobility, and core endurance.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (key, title, desc) ->
            SlickSelectableCard(
                title = title,
                subtitle = desc,
                isSelected = selectedGender == key,
                onClick = { onSelect(key) }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2: Primary Goal
// -------------------------------------------------------------
@Composable
private fun StepPrimaryGoal(
    selectedGoal: String,
    onSelect: (String) -> Unit
) {
    StepHeader(
        eyebrow = "PRIMARY OBJECTIVE",
        title = "WHAT IS YOUR MAIN GOAL?",
        subtitle = "This calibrates your reps, set cadences, and daily caloric balance."
    )

    Spacer(modifier = Modifier.height(20.dp))

    val goals = listOf(
        Fourfold("fat_loss", "LOSE FAT & GET LEAN", "High metabolic output, short rest intervals, and caloric deficit.", Icons.Filled.LocalFireDepartment),
        Fourfold("build_muscle", "BUILD MUSCLE & HYPERTROPHY", "Progressive overload, high time-under-tension, and strength volume.", Icons.Filled.FitnessCenter),
        Fourfold("sport_stamina", "ATHLETIC STAMINA & SPEED", "Functional agility, explosive plyometrics, and cardio endurance.", Icons.Filled.Speed),
        Fourfold("general_fitness", "STRENGTH & POSTURAL BALANCE", "Mobility, spine stabilization, and pain-free joints.", Icons.Filled.SelfImprovement)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        goals.forEach { item ->
            SlickSelectableCard(
                title = item.title,
                subtitle = item.desc,
                icon = item.icon,
                isSelected = selectedGoal == item.key,
                onClick = { onSelect(item.key) }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 3: Focus Target Areas
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepFocusAreas(
    selectedAreas: Set<String>,
    onToggle: (String) -> Unit
) {
    StepHeader(
        eyebrow = "ANATOMICAL FOCUS",
        title = "TARGET MUSCLE GROUPS",
        subtitle = "Select one or more zones to receive prioritized weekly training volume."
    )

    Spacer(modifier = Modifier.height(20.dp))

    val areas = listOf(
        "Full Body" to "Balanced head-to-toe stimulation",
        "Chest & Arms" to "Pectorals, triceps & bicep definition",
        "Abs & Core Six-Pack" to "Rectus abdominis, obliques & deep core",
        "Legs & Glutes" to "Quadriceps, hamstrings & calves",
        "Back & Posture" to "Lats, rhomboids & scapular retraction"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        areas.forEach { (name, desc) ->
            val isSelected = selectedAreas.contains(name)
            SlickSelectableCard(
                title = name.uppercase(),
                subtitle = desc,
                isSelected = isSelected,
                onClick = { onToggle(name) }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 4: Strength / Push-up Benchmark
// -------------------------------------------------------------
@Composable
private fun StepStrengthBenchmark(
    selectedLevel: String,
    onSelect: (String) -> Unit
) {
    StepHeader(
        eyebrow = "BASELINE STRENGTH",
        title = "HOW MANY PUSH-UPS IN ONE GO?",
        subtitle = "We use push-up capacity as the universal calisthenics baseline to prescribe correct variations."
    )

    Spacer(modifier = Modifier.height(20.dp))

    val benchmarks = listOf(
        Triple("beginner", "0 TO 5 PUSH-UPS (BEGINNER)", "Plan will include knee push-ups, wall inclines, and eccentric control."),
        Triple("intermediate", "6 TO 18 PUSH-UPS (INTERMEDIATE)", "Plan will include standard push-ups, slow tempos, and diamond reps."),
        Triple("advanced", "19+ PUSH-UPS (ADVANCED)", "Plan will include archer push-ups, explosive plyometrics, and deficit reps.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        benchmarks.forEach { (key, title, desc) ->
            SlickSelectableCard(
                title = title,
                subtitle = desc,
                isSelected = selectedLevel == key,
                onClick = { onSelect(key) }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 5: Equipment Availability
// -------------------------------------------------------------
@Composable
private fun StepEquipment(
    selectedEquip: String,
    onSelect: (String) -> Unit
) {
    StepHeader(
        eyebrow = "TRAINING ENVIRONMENT",
        title = "WHERE DO YOU WORK OUT?",
        subtitle = "Zero equipment? No problem. Every single movement will fit your available gear."
    )

    Spacer(modifier = Modifier.height(20.dp))

    val equips = listOf(
        Triple("home_bodyweight", "HOME / NO EQUIPMENT", "100% Bodyweight calisthenics using floor, wall, and chairs."),
        Triple("dumbbells_only", "DUMBBELLS & BANDS ONLY", "Minimal home setup with pairs of dumbbells or resistance loop bands."),
        Triple("full_gym", "FULL GYM ACCESS", "Commercial fitness gym with barbells, cables, and pin-loaded machines.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        equips.forEach { (key, title, desc) ->
            SlickSelectableCard(
                title = title,
                subtitle = desc,
                isSelected = selectedEquip == key,
                onClick = { onSelect(key) }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 6: Weekly Schedule Commitment
// -------------------------------------------------------------
@Composable
private fun StepWeeklySchedule(
    selectedDays: Int,
    onSelect: (Int) -> Unit
) {
    StepHeader(
        eyebrow = "WEEKLY RHYTHM",
        title = "HOW MANY DAYS PER WEEK?",
        subtitle = "Consistency beats intensity. Choose a schedule you can maintain for the next 28 days."
    )

    Spacer(modifier = Modifier.height(20.dp))

    val options = listOf(
        Triple(3, "3 DAYS / WEEK • RECOVERY OPTIMIZED", "Ideal for beginners or busy schedules. Complete recovery between sessions."),
        Triple(4, "4 DAYS / WEEK • HYPERTROPHY SWEET SPOT", "Recommended. 2 days on, 1 day off, 2 days on. Best balance of growth and rest."),
        Triple(5, "5 DAYS / WEEK • ATHLETIC OVERLOAD", "High commitment split for seasoned athletes pushing for rapid transformation.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (days, title, desc) ->
            SlickSelectableCard(
                title = title,
                subtitle = desc,
                isSelected = selectedDays == days,
                onClick = { onSelect(days) }
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 7: Biometrics & Name
// -------------------------------------------------------------
@Composable
private fun StepBiometricsAndName(
    name: String,
    onNameChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    diet: String,
    onDietChange: (String) -> Unit
) {
    StepHeader(
        eyebrow = "ATHLETE PROFILE",
        title = "FINALIZE YOUR DETAILS",
        subtitle = "We calculate your basal metabolic rate and daily hydration baseline."
    )

    Spacer(modifier = Modifier.height(20.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Athlete Display Name", color = HomeWorkoutTextSecondary) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HomeWorkoutCard,
                unfocusedContainerColor = HomeWorkoutCard,
                focusedBorderColor = HomeWorkoutVolt,
                unfocusedBorderColor = HomeWorkoutCardBorder,
                focusedTextColor = HomeWorkoutTextPrimary,
                unfocusedTextColor = HomeWorkoutTextPrimary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = weight,
                onValueChange = { onWeightChange(it.filter { char -> char.isDigit() || char == '.' }) },
                label = { Text("Weight (kg)", color = HomeWorkoutTextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HomeWorkoutCard,
                    unfocusedContainerColor = HomeWorkoutCard,
                    focusedBorderColor = HomeWorkoutVolt,
                    unfocusedBorderColor = HomeWorkoutCardBorder,
                    focusedTextColor = HomeWorkoutTextPrimary,
                    unfocusedTextColor = HomeWorkoutTextPrimary
                )
            )

            OutlinedTextField(
                value = height,
                onValueChange = { onHeightChange(it.filter { char -> char.isDigit() }) },
                label = { Text("Height (cm)", color = HomeWorkoutTextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HomeWorkoutCard,
                    unfocusedContainerColor = HomeWorkoutCard,
                    focusedBorderColor = HomeWorkoutVolt,
                    unfocusedBorderColor = HomeWorkoutCardBorder,
                    focusedTextColor = HomeWorkoutTextPrimary,
                    unfocusedTextColor = HomeWorkoutTextPrimary
                )
            )
        }

        // Live BMI calculation pill
        val weightNum = weight.toDoubleOrNull() ?: 72.0
        val heightM = (height.toDoubleOrNull() ?: 175.0) / 100.0
        val bmi = if (heightM > 0) weightNum / (heightM * heightM) else 23.5
        val bmiCategory = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Healthy Range"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }

        Surface(
            color = HomeWorkoutCard,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HomeWorkoutCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ESTIMATED BMI: ${String.format(java.util.Locale.US, "%.1f", bmi)}",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = HomeWorkoutTextPrimary
                )
                Text(
                    text = bmiCategory.uppercase(),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = HomeWorkoutVolt
                )
            }
        }

        Text(
            text = "NUTRITION & DIETARY PREFERENCE",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = HomeWorkoutTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        val diets = listOf(
            "balanced" to "Balanced (All Foods)",
            "high_protein_budget" to "High-Protein Desi Budget",
            "vegetarian" to "Vegetarian (Paneer & Dals)",
            "eggitarian" to "Eggitarian (Eggs & Dairy)",
            "vegan" to "100% Plant-Based"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            diets.forEach { (key, label) ->
                SlickSelectableCard(
                    title = label,
                    subtitle = "",
                    isSelected = diet == key,
                    onClick = { onDietChange(key) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 8: Dynamic Plan Synthesis Loading Screen
// -------------------------------------------------------------
@Composable
private fun PlanSynthesisLoadingView(
    athleteName: String,
    goal: String
) {
    var checkIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (checkIndex < 3) {
            delay(900)
            checkIndex++
        }
    }

    val checklist = listOf(
        "Calibrating push-pull volume balance...",
        "Structuring 28-day progressive overload curves...",
        "Locking in high-protein nutrition & hydration targets..."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeWorkoutBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(96.dp),
                    color = HomeWorkoutVolt,
                    strokeWidth = 3.dp,
                    trackColor = HomeWorkoutCardBorder
                )
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = HomeWorkoutVolt,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "BUILDING YOUR 28-DAY PLAN",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = HomeWorkoutTextPrimary
            )

            Text(
                text = "Personalizing routines for $athleteName based on ${goal.replace('_', ' ').uppercase()}...",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = HomeWorkoutTextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = HomeWorkoutCard,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HomeWorkoutCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    checklist.forEachIndexed { index, item ->
                        val isDone = checkIndex > index
                        val isCurrent = checkIndex == index

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> HomeWorkoutVolt
                                            isCurrent -> HomeWorkoutCyan.copy(alpha = 0.2f)
                                            else -> HomeWorkoutCardBorder
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0D0E12),
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isCurrent) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = HomeWorkoutCyan
                                    )
                                }
                            }

                            Text(
                                text = item,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = if (isDone || isCurrent) FontWeight.Medium else FontWeight.Normal
                                ),
                                color = if (isDone || isCurrent) HomeWorkoutTextPrimary else HomeWorkoutTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Reusable UI Components for Slick Home Workout Style
// -------------------------------------------------------------
@Composable
private fun StepHeader(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = eyebrow,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = HomeWorkoutVolt
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = HomeWorkoutTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            ),
            color = HomeWorkoutTextSecondary
        )
    }
}

@Composable
private fun SlickSelectableCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) HomeWorkoutVolt else HomeWorkoutCardBorder,
        animationSpec = tween(durationMillis = 150),
        label = "cardBorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) HomeWorkoutCardSelected else HomeWorkoutCard,
        animationSpec = tween(durationMillis = 150),
        label = "cardBgColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) HomeWorkoutVolt.copy(alpha = 0.15f) else Color(0xFF1E222F)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) HomeWorkoutVolt else HomeWorkoutTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isSelected) HomeWorkoutTextPrimary else HomeWorkoutTextPrimary.copy(alpha = 0.9f)
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (isSelected) HomeWorkoutTextSecondary else HomeWorkoutTextMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) HomeWorkoutVolt else HomeWorkoutCardBorder,
                        shape = CircleShape
                    )
                    .background(if (isSelected) HomeWorkoutVolt else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF0D0E12),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private data class Fourfold(
    val key: String,
    val title: String,
    val desc: String,
    val icon: ImageVector
)
