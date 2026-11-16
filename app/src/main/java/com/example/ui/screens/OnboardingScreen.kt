package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var name by remember(existingProfile) {
        mutableStateOf(existingProfile?.displayName?.ifBlank { defaultName } ?: defaultName)
    }
    var goal by remember(existingProfile) {
        mutableStateOf(existingProfile?.goal ?: "build_muscle")
    }
    var experience by remember(existingProfile) {
        mutableStateOf(existingProfile?.experienceLevel ?: "day_one")
    }
    var equipment by remember(existingProfile) {
        mutableStateOf(existingProfile?.equipmentAccess ?: "full_gym")
    }
    var sportFocus by remember(existingProfile) {
        mutableStateOf(existingProfile?.sportFocus?.ifBlank { "General Gym" } ?: "General Gym")
    }
    var dietStyle by remember(existingProfile) {
        mutableStateOf(existingProfile?.dietaryStyle ?: "balanced")
    }
    var daysPerWeek by remember(existingProfile) {
        mutableIntStateOf(existingProfile?.daysPerWeek ?: 4)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingProfile?.onboardingCompleted == true) {
                            "Update Your Plan"
                        } else {
                            "Welcome to SpotMe"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No guesswork on Day 1.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tell us where you're starting from and what you actually eat. We'll build a 7-day plan with clear setup steps and meal targets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What should we call you?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                SectionLabel("1. What's your main goal?")
                val goals = listOf(
                    "build_muscle" to "Build Muscle & Tone",
                    "fat_loss" to "Burn Fat & Get Lean",
                    "sport_stamina" to "Sport Stamina & Agility",
                    "general_fitness" to "Feel Stronger & Healthy"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.forEach { (key, label) ->
                        ChoiceChip(
                            label = label,
                            selected = goal == key,
                            testTag = "goal_chip_$key",
                            onClick = { goal = key }
                        )
                    }
                }

                SectionLabel("2. Where are you starting?")
                val levels = listOf(
                    "day_one" to "Day 1 Beginner",
                    "returning" to "Returning After Break",
                    "intermediate" to "Comfortable in Gym"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levels.forEach { (key, label) ->
                        ChoiceChip(
                            label = label,
                            selected = experience == key,
                            testTag = "exp_chip_$key",
                            onClick = { experience = key }
                        )
                    }
                }

                SectionLabel("3. Where do you train?")
                val equips = listOf(
                    "full_gym" to "Full Gym (Machines & Weights)",
                    "dumbbells_only" to "Dumbbells Only",
                    "home_bodyweight" to "Home / No Equipment",
                    "sport_court" to "Field / Court / Track"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    equips.forEach { (key, label) ->
                        ChoiceChip(
                            label = label,
                            selected = equipment == key,
                            testTag = "equip_chip_$key",
                            onClick = { equipment = key }
                        )
                    }
                }

                SectionLabel("4. Sport or focus (optional)")
                val sports = listOf("General Gym", "Running", "Basketball", "Cricket", "Football", "Badminton")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sports.forEach { sp ->
                        ChoiceChip(
                            label = sp,
                            selected = sportFocus == sp,
                            testTag = "sport_chip_${sp.lowercase().replace(" ", "_")}",
                            onClick = { sportFocus = sp }
                        )
                    }
                }

                SectionLabel("5. How many workout days per week?")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (2..6).forEach { dayCount ->
                        FilterChip(
                            selected = daysPerWeek == dayCount,
                            onClick = { daysPerWeek = dayCount },
                            label = {
                                Text(
                                    text = "${dayCount}d",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("days_chip_$dayCount")
                        )
                    }
                }

                SectionLabel("6. How do you eat?")
                val diets = listOf(
                    "balanced" to "Balanced (All Foods)",
                    "high_protein_budget" to "Budget High-Protein",
                    "vegetarian" to "Vegetarian",
                    "eggitarian" to "Eggitarian",
                    "vegan" to "Plant-Based / Vegan"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    diets.forEach { (key, label) ->
                        ChoiceChip(
                            label = label,
                            selected = dietStyle == key,
                            testTag = "diet_chip_$key",
                            onClick = { dietStyle = key }
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        onGeneratePlan(
                            name,
                            goal,
                            experience,
                            equipment,
                            sportFocus,
                            dietStyle,
                            daysPerWeek
                        )
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("generate_plan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Building your personalized routine...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Build My Routine & Diet Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag(testTag)
    )
}
