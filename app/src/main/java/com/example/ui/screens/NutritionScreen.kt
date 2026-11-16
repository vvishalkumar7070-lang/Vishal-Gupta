package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MealLog
import com.example.data.model.MealSuggestion
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.SessionUiState
import kotlin.math.roundToInt

private data class DesiFuelItem(
    val slot: String,
    val name: String,
    val category: String, // "all", "veg", "egg", "non_veg", "hostel"
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val approxPriceRs: Int,
    val badge: String
)

private val IndianGenZMeals = listOf(
    DesiFuelItem("snack", "Desi Sattu Power Shake", "veg", 280, 24, 36, 5, 20, "₹0.83/g P • Desi Superfood"),
    DesiFuelItem("snack", "Amul High Protein Buttermilk", "veg", 85, 15, 5, 0, 25, "₹1.66/g P • Zero Sugar"),
    DesiFuelItem("lunch", "Boiled Soya Chunks Chaat (50g)", "veg", 185, 26, 16, 1, 15, "₹0.58/g P • Ultra Budget"),
    DesiFuelItem("breakfast", "3 Boiled Eggs + 2 Multigrain Toast", "egg", 310, 22, 28, 12, 30, "₹1.36/g P • Gold Standard"),
    DesiFuelItem("dinner", "100g Paneer Bhurji + 2 Rotis", "veg", 360, 20, 32, 16, 55, "₹2.75/g P • High Satiety"),
    DesiFuelItem("breakfast", "Peanut Butter & Banana Brown Toast", "hostel", 290, 12, 38, 11, 25, "Hostel Friendly"),
    DesiFuelItem("post_workout", "Pre-Workout: Black Coffee + Banana", "hostel", 105, 2, 27, 0, 12, "₹12 Clean Pre"),
    DesiFuelItem("dinner", "Double Egg Chicken Roll (Hostel Style)", "non_veg", 480, 32, 40, 18, 90, "32g Protein Punch"),
    DesiFuelItem("breakfast", "Hostel Kettle Oats + Milk + Almonds", "hostel", 360, 16, 48, 12, 35, "Electric Kettle Quick"),
    DesiFuelItem("post_workout", "Amul High Protein Lassi", "veg", 130, 15, 12, 1, 25, "₹1.66/g P • Cold Refresh")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionScreen(
    profile: UserProfile?,
    todayDateKey: String,
    mealLogs: List<MealLog>,
    session: SessionUiState,
    onAdjustWater: (Int) -> Unit,
    onRequestAiMeal: (remainingCal: Int, remainingProtein: Int, slot: String) -> Unit,
    onEstimateMeal: (description: String, slot: String) -> Unit,
    onDismissSuggestion: () -> Unit,
    onAddMeal: (slot: String, name: String, cal: Int, pro: Int, carbs: Int, fat: Int, isAi: Boolean) -> Unit,
    onDeleteMeal: (String) -> Unit
) {
    val todayMeals = remember(mealLogs, todayDateKey) {
        mealLogs.filter { it.dateKey == todayDateKey }
    }
    val caloriesTarget = profile?.dailyCalorieTarget ?: 2200
    val proteinTarget = profile?.dailyProteinTargetG ?: 135
    val carbsTarget = profile?.dailyCarbsTargetG ?: 240
    val fatTarget = profile?.dailyFatTargetG ?: 70

    val caloriesDone = todayMeals.sumOf { it.calories }
    val proteinDone = todayMeals.sumOf { it.proteinG }
    val carbsDone = todayMeals.sumOf { it.carbsG }
    val fatDone = todayMeals.sumOf { it.fatG }

    val remainingCal = (caloriesTarget - caloriesDone).coerceAtLeast(0)
    val remainingProtein = (proteinTarget - proteinDone).coerceAtLeast(0)

    val calProgress by animateFloatAsState(
        targetValue = (caloriesDone.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f),
        label = "cal_progress"
    )

    var selectedSlot by remember { mutableStateOf("lunch") }
    var mealDescription by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var selectedFoodCategory by remember { mutableStateOf("all") }

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
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.img_nutrition_fuel_1794806580577),
                            contentDescription = "Balanced protein meal bowl illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "TODAY'S FUEL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$caloriesDone / $caloriesTarget kcal",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "$remainingCal left",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { calProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MacroBarBox(
                                    label = "Protein",
                                    current = proteinDone,
                                    target = proteinTarget,
                                    modifier = Modifier.weight(1f)
                                )
                                MacroBarBox(
                                    label = "Carbs",
                                    current = carbsDone,
                                    target = carbsTarget,
                                    modifier = Modifier.weight(1f)
                                )
                                MacroBarBox(
                                    label = "Fats",
                                    current = fatDone,
                                    target = fatTarget,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Hydration Tracker
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WaterDrop,
                                    contentDescription = "Hydration",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Hydration: ${session.waterGlassesToday} / 8 glasses",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${session.waterGlassesToday * 250} ml today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = { onAdjustWater(-1) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("water_minus_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Remove water glass")
                                }
                            }
                            Surface(
                                onClick = { onAdjustWater(1) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("water_plus_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add water glass",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Smart Meal Helper",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Need ideas for your remaining ${remainingProtein}g protein & $remainingCal kcal, or want to estimate a meal you just ate?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "breakfast" to "Breakfast",
                                "lunch" to "Lunch",
                                "post_workout" to "Post-Workout",
                                "dinner" to "Dinner",
                                "snack" to "Snack"
                            ).forEach { (slot, label) ->
                                FilterChip(
                                    selected = selectedSlot == slot,
                                    onClick = { selectedSlot = slot },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mealDescription,
                                onValueChange = { mealDescription = it },
                                placeholder = { Text("Describe what you ate (e.g. 2 eggs & toast)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("describe_meal_input")
                            )
                            Button(
                                onClick = {
                                    val desc = mealDescription
                                    mealDescription = ""
                                    onEstimateMeal(desc, selectedSlot)
                                },
                                enabled = mealDescription.isNotBlank() && !session.isGeneratingMeal,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("estimate_meal_button")
                            ) {
                                Text("Estimate")
                            }
                        }

                        Button(
                            onClick = { onRequestAiMeal(remainingCal, remainingProtein, selectedSlot) },
                            enabled = !session.isGeneratingMeal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("suggest_meal_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (session.isGeneratingMeal) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Working on your macros...")
                            } else {
                                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Suggest Meal For My Macros")
                            }
                        }

                        val suggestion = session.mealSuggestion
                        if (suggestion != null) {
                            AiMealSuggestionCard(
                                suggestion = suggestion,
                                onLog = { scale ->
                                    onAddMeal(
                                        suggestion.mealSlot,
                                        if (scale == 1f) suggestion.name else "${suggestion.name} (${scale}x)",
                                        (suggestion.calories * scale).roundToInt(),
                                        (suggestion.proteinG * scale).roundToInt(),
                                        (suggestion.carbsG * scale).roundToInt(),
                                        (suggestion.fatG * scale).roundToInt(),
                                        true
                                    )
                                },
                                onDismiss = onDismissSuggestion
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Desi High-Protein Quick Add",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Budget Indian staples with ₹/g protein ratings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showCustomDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("custom_meal_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Custom")
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "All Foods",
                        "veg" to "🌱 Pure Veg",
                        "egg" to "🥚 Eggetarian",
                        "non_veg" to "🍗 Non-Veg",
                        "hostel" to "🎒 Hostel / Budget (< ₹30)"
                    ).forEach { (catKey, catLabel) ->
                        FilterChip(
                            selected = selectedFoodCategory == catKey,
                            onClick = { selectedFoodCategory = catKey },
                            label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            item {
                val filteredMeals = remember(selectedFoodCategory) {
                    if (selectedFoodCategory == "all") IndianGenZMeals
                    else IndianGenZMeals.filter { it.category == selectedFoodCategory }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredMeals.forEachIndexed { index, qm ->
                        Surface(
                            onClick = {
                                onAddMeal(qm.slot, qm.name, qm.calories, qm.proteinG, qm.carbsG, qm.fatG, false)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.testTag("quick_meal_$index")
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .widthIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ ${qm.name}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${qm.proteinG}g Protein",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${qm.calories} kcal • ~₹${qm.approxPriceRs}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = qm.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 Gen-Z Indian Lifter Hacks",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• Soya chunks (₹15) & Chana Sattu (₹20) offer the highest protein per rupee in India (< ₹1/g).\n" +
                                "• Pair Sattu with milk or eggs to complete its leucine profile for muscle building.\n" +
                                "• Amul High Protein Buttermilk gives 15g clean whey protein with zero sugar for just ₹25.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Logged Today (${todayMeals.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (todayMeals.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Nothing logged yet today. Tap a quick meal above or describe what you ate!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todayMeals, key = { it.id }) { meal ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meal.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${meal.mealSlot.replace(\"_\", \" \").uppercase()} • ${meal.calories} kcal • ${meal.proteinG}g P • ${meal.carbsG}g C • ${meal.fatG}g F",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteMeal(meal.id) },
                                modifier = Modifier.testTag("delete_meal_${meal.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete meal",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showCustomDialog) {
        CustomMealDialog(
            defaultSlot = selectedSlot,
            onDismiss = { showCustomDialog = false },
            onSave = { slot, name, cal, pro, carbs, fat ->
                showCustomDialog = false
                onAddMeal(slot, name, cal, pro, carbs, fat, false)
            }
        )
    }
}

@Composable
private fun MacroBarBox(
    label: String,
    current: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val barProgress by animateFloatAsState(
        targetValue = (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
        label = "macro_bar_$label"
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${current}/${target}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            LinearProgressIndicator(
                progress = { barProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            )
        }
    }
}

@Composable
private fun AiMealSuggestionCard(
    suggestion: MealSuggestion,
    onLog: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var portionScale by remember(suggestion.name) { mutableFloatStateOf(1f) }
    val scaledCal = (suggestion.calories * portionScale).roundToInt()
    val scaledPro = (suggestion.proteinG * portionScale).roundToInt()
    val scaledCarbs = (suggestion.carbsG * portionScale).roundToInt()
    val scaledFat = (suggestion.fatG * portionScale).roundToInt()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$scaledCal kcal • ${scaledPro}g P • ${scaledCarbs}g C • ${scaledFat}g F",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Note: ${suggestion.quickPrepNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portion:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(0.5f to "0.5x", 1f to "1x", 1.5f to "1.5x").forEach { (scale, label) ->
                    FilterChip(
                        selected = portionScale == scale,
                        onClick = { portionScale = scale },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Button(
                    onClick = { onLog(portionScale) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("log_suggested_meal_button")
                ) {
                    Text("Log This Meal")
                }
            }
        }
    }
}

@Composable
private fun CustomMealDialog(
    defaultSlot: String,
    onDismiss: () -> Unit,
    onSave: (slot: String, name: String, cal: Int, pro: Int, carbs: Int, fat: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("450") }
    var protein by remember { mutableStateOf("28") }
    var carbs by remember { mutableStateOf("45") }
    var fat by remember { mutableStateOf("14") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal name (e.g., Egg Bhurji + Roti)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_meal_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Calories") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeName = name.trim().ifEmpty { "Home Meal" }
                    onSave(
                        defaultSlot,
                        safeName,
                        calories.toIntOrNull() ?: 400,
                        protein.toIntOrNull() ?: 25,
                        carbs.toIntOrNull() ?: 40,
                        fat.toIntOrNull() ?: 12
                    )
                },
                modifier = Modifier.testTag("save_custom_meal_button")
            ) {
                Text("Add Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
