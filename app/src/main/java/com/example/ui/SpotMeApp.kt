package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.ai.SpotMeAiService
import com.example.data.model.BuddyMessage
import com.example.data.model.MealLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutDay
import com.example.data.model.WorkoutLog
import com.example.data.repository.SpotMeRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BuddyCoachScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlanProfileScreen
import com.example.ui.screens.TodayWorkoutScreen
import com.example.ui.viewmodel.SpotMeViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object SpotMeRoutes {
    const val TODAY = "today"
    const val FUEL = "fuel"
    const val BUDDY = "buddy"
    const val PLAN = "plan"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val SpotMeTabs = listOf(
    NavTab(SpotMeRoutes.TODAY, "Today", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    NavTab(SpotMeRoutes.FUEL, "Fuel", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    NavTab(SpotMeRoutes.BUDDY, "Buddy", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    NavTab(SpotMeRoutes.PLAN, "Plan", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday)
)

@Composable
fun SpotMeRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateFlow automatically transitions */ })
    } else {
        androidx.compose.runtime.key(user.uid) {
            AuthenticatedSpotMeApp(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: "Athlete"
            )
        }
    }
}

@Composable
private fun AuthenticatedSpotMeApp(
    currentUserId: String,
    defaultDisplayName: String
) {
    val viewModel: SpotMeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                SpotMeViewModel(
                    repository = SpotMeRepository(db),
                    aiService = SpotMeAiService(),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val workoutDaysState by viewModel.workoutDaysState.collectAsStateWithLifecycle()
    val workoutLogsState by viewModel.workoutLogsState.collectAsStateWithLifecycle()
    val mealLogsState by viewModel.mealLogsState.collectAsStateWithLifecycle()
    val buddyMessagesState by viewModel.buddyMessagesState.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isEditingPlan by remember { mutableStateOf(false) }
    var buddyContextTag by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(session.bannerMessage, session.errorMessage) {
        val msg = session.bannerMessage ?: session.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    when (val pState = profileState) {
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            OnboardingScreen(
                existingProfile = null,
                defaultName = defaultDisplayName,
                isGenerating = session.isGeneratingPlan,
                errorMessage = pState.message,
                onGeneratePlan = { name, goal, exp, equip, sport, diet, days ->
                    viewModel.generateAndSavePlan(name, goal, exp, equip, sport, diet, days)
                }
            )
        }
        is UiState.Success -> {
            val profile: UserProfile? = pState.data
            if (profile == null || !profile.onboardingCompleted || isEditingPlan) {
                if (isEditingPlan && profile != null) {
                    BackHandler { isEditingPlan = false }
                }
                OnboardingScreen(
                    existingProfile = profile,
                    defaultName = defaultDisplayName,
                    isGenerating = session.isGeneratingPlan,
                    errorMessage = session.errorMessage,
                    onBack = if (isEditingPlan && profile != null) {
                        { isEditingPlan = false }
                    } else null,
                    onGeneratePlan = { name, goal, exp, equip, sport, diet, days ->
                        viewModel.generateAndSavePlan(name, goal, exp, equip, sport, diet, days)
                        isEditingPlan = false
                    }
                )
            } else {
                val navController = rememberNavController()
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route ?: SpotMeRoutes.TODAY

                val workoutDays: List<WorkoutDay> =
                    (workoutDaysState as? UiState.Success)?.data ?: emptyList()
                val workoutLogs: List<WorkoutLog> =
                    (workoutLogsState as? UiState.Success)?.data ?: emptyList()
                val mealLogs: List<MealLog> =
                    (mealLogsState as? UiState.Success)?.data ?: emptyList()
                val buddyMessages: List<BuddyMessage> =
                    (buddyMessagesState as? UiState.Success)?.data ?: emptyList()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 700.dp

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar {
                                    SpotMeTabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(SpotMeRoutes.TODAY) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail {
                                    SpotMeTabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(SpotMeRoutes.TODAY) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag("rail_tab_${tab.route}")
                                        )
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = SpotMeRoutes.TODAY,
                                modifier = Modifier.weight(1f)
                            ) {
                                composable(SpotMeRoutes.TODAY) {
                                    TodayWorkoutScreen(
                                        profile = profile,
                                        workoutDays = workoutDays,
                                        session = session,
                                        onSelectDay = viewModel::selectDay,
                                        onToggleSet = viewModel::toggleSetComplete,
                                        onStartTimer = viewModel::startRestTimer,
                                        onStopTimer = viewModel::stopRestTimer,
                                        onQuickSwap = viewModel::swapExerciseQuick,
                                        onAiSwap = { day, ex ->
                                            viewModel.swapExerciseWithAi(profile, day, ex)
                                        },
                                        onAskBuddyAboutExercise = { exName ->
                                            buddyContextTag = exName
                                            navController.navigate(SpotMeRoutes.BUDDY) {
                                                popUpTo(SpotMeRoutes.TODAY) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onLogWorkout = viewModel::logCompletedWorkout
                                    )
                                }

                                composable(SpotMeRoutes.FUEL) {
                                    NutritionScreen(
                                        profile = profile,
                                        todayDateKey = viewModel.todayDateKey,
                                        mealLogs = mealLogs,
                                        session = session,
                                        onRequestAiMeal = { remCal, remPro, slot ->
                                            viewModel.requestMealSuggestion(profile, remCal, remPro, slot)
                                        },
                                        onDismissSuggestion = viewModel::dismissMealSuggestion,
                                        onAddMeal = viewModel::addMeal,
                                        onDeleteMeal = viewModel::deleteMeal
                                    )
                                }

                                composable(SpotMeRoutes.BUDDY) {
                                    BuddyCoachScreen(
                                        messages = buddyMessages,
                                        streamingReply = session.streamingCoachReply,
                                        isAsking = session.isAskingBuddy,
                                        activeContextTag = buddyContextTag,
                                        onClearContextTag = { buddyContextTag = "" },
                                        onSendQuestion = { q, tag ->
                                            viewModel.askBuddy(profile, q, tag)
                                        },
                                        onClearChat = viewModel::clearBuddyHistory
                                    )
                                }

                                composable(SpotMeRoutes.PLAN) {
                                    PlanProfileScreen(
                                        profile = profile,
                                        workoutDays = workoutDays,
                                        workoutLogs = workoutLogs,
                                        onEditPlan = { isEditingPlan = true },
                                        onSelectDayToTrain = { dayNum ->
                                            viewModel.selectDay(dayNum)
                                            navController.navigate(SpotMeRoutes.TODAY) {
                                                popUpTo(SpotMeRoutes.TODAY) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        onSignOut = {
                                            signOutUser(
                                                credentialManager = credentialManager,
                                                scope = scope
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
