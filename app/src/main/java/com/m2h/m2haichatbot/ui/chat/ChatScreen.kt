package com.m2h.m2haichatbot.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.m2h.m2haichatbot.ui.chat.components.ChatInput
import com.m2h.m2haichatbot.ui.chat.components.MessageBubble
import com.m2h.m2haichatbot.ui.chat.components.ModelSelectorSheet
import com.m2h.m2haichatbot.ui.chat.components.Sidebar
import com.m2h.m2haichatbot.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showModelSelector by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Sidebar(
                chats = uiState.filteredChats,
                currentChatId = uiState.currentChat?.id,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onChatSelect = {
                    viewModel.selectChat(it)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.createNewChat()
                    scope.launch { drawerState.close() }
                },
                onLogout = onLogout,
                onDeleteChat = { viewModel.deleteChat(it) },
                onRenameChat = { id, title -> viewModel.renameChat(id, title) },
                onTogglePin = { viewModel.togglePin(it) }
            )
        }
    ) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        TextButton(onClick = { showModelSelector = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    uiState.selectedModelId.split("/").last().uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Ink
                                )
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = CoralPrimary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Ink)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Ink)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Canvas
                    )
                )
            },
            bottomBar = {
                Column {
                    if (uiState.isStreaming) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { viewModel.stopStreaming() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceCard,
                                    contentColor = Ink
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.StopCircle, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Stop generating", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    ChatInput(
                        onSendMessage = { text, uris -> viewModel.sendMessage(text, uris) },
                        onVoiceInput = { /* Handle voice */ },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (uiState.messages.isEmpty()) {
                    EmptyChatView()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.messages.size) { index ->
                            val message = uiState.messages[index]
                            MessageBubble(
                                message = message,
                                isLast = index == uiState.messages.size - 1,
                                onRegenerate = {
                                    // Logic for regeneration: remove last assistant message and resend last user message
                                    // This can be implemented in ViewModel
                                }
                            )
                        }
                        if (uiState.isStreaming) {
                            item {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showModelSelector) {
        ModelSelectorSheet(
            availableModels = uiState.availableModels,
            selectedModelId = uiState.selectedModelId,
            onModelSelect = { viewModel.updateModel(it.id) },
            onDismiss = { showModelSelector = false }
        )
    }
}

@Composable
fun EmptyChatView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Placeholder for the Spike logo
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Ink, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("*", color = Canvas, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "How can I help you today?",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = CoralPrimary
        )
    }
}
