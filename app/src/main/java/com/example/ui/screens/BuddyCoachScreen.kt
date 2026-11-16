package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ai.BuddyRole
import com.example.data.model.BuddyMessage

private val StarterQuestions = listOf(
    "Bhai 100g paneer me kitna protein hota hai?",
    "Sattu vs Whey: Desi lifters ke liye kya better hai?",
    "Incline dumbbell press correct bench angle?",
    "Lateral raise me traps engage ho rahe hain, kaise rokein?",
    "Hostel/PG me budget high-protein diet?",
    "Creatine kab aur kaise lena chahiye?"
)

private val FollowUpPrompts = listOf(
    "Machine pe work-in kaise poochen?",
    "Desi ₹12 pre-workout formula",
    "Soya chunks benefits & preparation",
    "Next session me weight kitna badhayein?",
    "Squat knee caving fix",
    "Amul high protein buttermilk guide"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuddyCoachScreen(
    messages: List<BuddyMessage>,
    streamingReply: String?,
    isAsking: Boolean,
    selectedRole: BuddyRole,
    activeContextTag: String,
    liveSummary: String,
    onSelectRole: (BuddyRole) -> Unit,
    onClearContextTag: () -> Unit,
    onSendQuestion: (question: String, contextTag: String) -> Unit,
    onClearChat: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + (if (streamingReply != null) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem((total - 1).coerceAtLeast(0))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
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
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = "SpotMe Coach",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ask Your Gym Buddy",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${selectedRole.label} • ${selectedRole.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (messages.isNotEmpty()) {
                    TextButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_buddy_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear chat",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Clear")
                    }
                }
            }

            // Interactive Role / Persona Switcher
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                items(BuddyRole.entries, key = { it.id }) { role ->
                    val roleIcon = when (role) {
                        BuddyRole.FORM_SPOTTER -> Icons.Filled.FitnessCenter
                        BuddyRole.NUTRITION_BUDDY -> Icons.Filled.Restaurant
                        BuddyRole.HYPE_MINDSET -> Icons.Filled.Psychology
                        BuddyRole.QUICK_TIP -> Icons.Filled.Bolt
                    }
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { onSelectRole(role) },
                        leadingIcon = {
                            Icon(
                                imageVector = roleIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = role.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("buddy_role_${role.id}")
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && streamingReply == null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Multi-turn coaching with your live routine & macro context.",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (liveSummary.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = liveSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Tap a starter below or switch coach roles above:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StarterQuestions.forEachIndexed { idx, prompt ->
                                        SuggestionChip(
                                            onClick = { onSendQuestion(prompt, activeContextTag) },
                                            label = { Text(prompt, style = MaterialTheme.typography.bodySmall) },
                                            modifier = Modifier.testTag("buddy_starter_$idx")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        isUser = msg.role == "user",
                        text = msg.text,
                        contextTag = msg.contextTag
                    )
                }

                if (streamingReply != null) {
                    item {
                        ChatBubble(
                            isUser = false,
                            text = streamingReply.ifBlank { "Thinking..." },
                            contextTag = ""
                        )
                    }
                }
            }

            if (messages.isNotEmpty() && !isAsking) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(FollowUpPrompts) { prompt ->
                        SuggestionChip(
                            onClick = { onSendQuestion(prompt, activeContextTag) },
                            label = { Text(prompt, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            if (activeContextTag.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Context: $activeContextTag",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = onClearContextTag,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear context",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask ${selectedRole.label} anything...") },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("buddy_question_input"),
                    shape = RoundedCornerShape(16.dp)
                )
                Surface(
                    onClick = {
                        if (inputText.isNotBlank() && !isAsking) {
                            val q = inputText
                            inputText = ""
                            onSendQuestion(q, activeContextTag)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("buddy_send_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAsking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send question",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ChatBubble(
    isUser: Boolean,
    text: String,
    contextTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (contextTag.isNotBlank() && isUser) {
                    Text(
                        text = "Re: $contextTag",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
