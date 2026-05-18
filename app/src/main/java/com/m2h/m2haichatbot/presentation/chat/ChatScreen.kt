package com.m2h.m2haichatbot.presentation.chat

import android.content.Intent
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.m2h.m2haichatbot.domain.model.Message
import com.m2h.m2haichatbot.domain.model.MessageRole
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNewChatCreated: (String) -> Unit = {},
    initialMessage: String = "",
    autoSendInitialMessage: Boolean = false,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var initialHandled by remember(chatId, initialMessage) { mutableStateOf(false) }

    LaunchedEffect(initialMessage, autoSendInitialMessage, state.modelId) {
        if (!initialHandled && initialMessage.isNotBlank() && (!autoSendInitialMessage || state.modelId.isNotBlank())) {
            initialHandled = true
            if (autoSendInitialMessage) {
                viewModel.sendMessage(initialMessage)
            } else {
                messageText = initialMessage
            }
        }
    }

    LaunchedEffect(state.messages.size, state.streamingContent) {
        if (state.messages.isNotEmpty() || state.streamingContent.isNotEmpty()) {
            listState.animateScrollToItem(
                if (state.streamingContent.isNotEmpty()) state.messages.size else state.messages.size - 1
            )
        }
    }
    
    var navigatedAfterFirstSend by remember { mutableStateOf(false) }

    LaunchedEffect(state.chatId, state.isLoading, state.isStreaming, state.messages.size) {
        if (
            chatId == "new" &&
            !navigatedAfterFirstSend &&
            state.chatId != "new" &&
            state.chatId.isNotEmpty() &&
            !state.isLoading &&
            !state.isStreaming &&
            state.messages.isNotEmpty()
        ) {
            navigatedAfterFirstSend = true
            onNewChatCreated(state.chatId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    var showModelSheet by remember { mutableStateOf(false) }
                    val currentModel = availableModels.find { it.id == state.modelId }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showModelSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentModel?.name ?: state.modelId.split("/").last(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (showModelSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { showModelSheet = false },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 20.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            "AI Model",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 20.dp)
                                        )
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(availableModels) { model ->
                                                val isSelected = model.id == state.modelId
                                                Surface(
                                                    onClick = {
                                                        viewModel.setModel(model.id)
                                                        showModelSheet = false
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = if (model.provider.contains("Google", true)) Icons.Default.AutoAwesome else Icons.Default.SmartToy,
                                                                contentDescription = null,
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(model.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                                            Text(model.description ?: "High performance AI model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        if (isSelected) {
                                                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.userAvatarUrl != null) {
                                coil.compose.AsyncImage(
                                    model = state.userAvatarUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "U",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                message = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                enabled = !state.isLoading && !state.isStreaming,
                isStreaming = state.isStreaming,
                onStopClick = viewModel::stopStreaming,
                onPlaceholderClick = { text ->
                    scope.launch { snackbarHostState.showSnackbar(text) }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (state.messages.isEmpty() && !state.isLoading && !state.isStreaming) {
                // Empty state suggestions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SuggestionItem(
                        icon = Icons.Default.Image,
                        text = "Create an image",
                        onClick = { messageText = "Generate an image of " }
                    )
                    SuggestionItem(
                        icon = Icons.Default.Edit,
                        text = "Write or edit",
                        onClick = { messageText = "Help me write " }
                    )
                    SuggestionItem(
                        icon = Icons.Default.Language,
                        text = "Look something up",
                        onClick = { messageText = "Search for " }
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onAction = viewModel::submitMessageAction
                        )
                    }

                    if (state.isStreaming) {
                        item {
                            if (state.streamingContent.isEmpty()) {
                                ThinkingAnimation()
                            } else {
                                StreamingMessageBubble(content = state.streamingContent)
                            }
                        }
                    }

                    if (state.isLoading) {
                        item {
                            LoadingIndicator()
                        }
                    }
                }
            }

            if (state.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(state.error!!)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onAction: (Message, String) -> Unit = { _, _ -> }
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = 320.dp),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 6.dp
                ),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                )
            }
        } else {
            AssistantMessageCard(
                message = message,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun AssistantMessageCard(
    message: Message,
    onAction: (Message, String) -> Unit
) {
    var reaction by remember(message.id) { mutableStateOf<String?>(null) }
    var copied by remember(message.id) { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val imageUrl = remember(message.content) { message.content.extractGeneratedImageUrl() }
    val cleanedMarkdown = remember(message.content) { message.content.withoutGeneratedImageMarkdown() }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 620.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    GeneratedImageContent(content = message.content)
                    if (imageUrl != null && cleanedMarkdown.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (cleanedMarkdown.isNotBlank()) {
                        MarkdownText(
                            markdown = cleanedMarkdown,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(start = 40.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AssistantActionButton(
                icon = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy response",
                selected = copied,
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.content))
                    copied = true
                    onAction(message, "copy")
                }
            )
            AssistantActionButton(
                icon = Icons.Default.ThumbUp,
                contentDescription = "Like response",
                selected = reaction == "like",
                onClick = {
                    reaction = "like"
                    onAction(message, "like")
                }
            )
            AssistantActionButton(
                icon = Icons.Default.ThumbDown,
                contentDescription = "Unlike response",
                selected = reaction == "unlike",
                onClick = {
                    reaction = "unlike"
                    onAction(message, "unlike")
                }
            )
            AssistantActionButton(
                icon = Icons.Default.Share,
                contentDescription = "Share response",
                selected = false,
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message.content)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share response"))
                    onAction(message, "share")
                }
            )
        }
    }
}

@Composable
private fun AssistantActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun StreamingMessageBubble(content: String) {
    val imageUrl = remember(content) { content.extractGeneratedImageUrl() }
    val cleanedMarkdown = remember(content) { content.withoutGeneratedImageMarkdown() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 620.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                GeneratedImageContent(content = content)
                if (imageUrl != null && cleanedMarkdown.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (cleanedMarkdown.isNotBlank()) {
                    MarkdownText(
                        markdown = cleanedMarkdown,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(100)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GeneratedImageContent(content: String) {
    val imageUrl = remember(content) { content.extractGeneratedImageUrl() }
    if (imageUrl != null) {
        val bitmap = remember(imageUrl) { imageUrl.decodeDataImageBitmap() }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Generated image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun String.extractGeneratedImageUrl(): String? {
    val markdown = Regex("""!\[[^\]]*]\(([^)]+)\)""").find(this)?.groupValues?.getOrNull(1)
    if (!markdown.isNullOrBlank()) return markdown
    val dataImage = Regex("""data:image/[^;\s]+;base64,[A-Za-z0-9+/=_-]+""").find(this)?.value
    if (!dataImage.isNullOrBlank()) return dataImage
    return Regex("""https?://\S+\.(?:png|jpg|jpeg|webp)(?:\?\S*)?""", RegexOption.IGNORE_CASE)
        .find(this)
        ?.value
}

private fun String.decodeDataImageBitmap(): android.graphics.Bitmap? {
    if (!startsWith("data:image/")) return null
    val base64 = substringAfter("base64,", missingDelimiterValue = "").ifBlank { return null }
    return runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun String.isGeneratedImageOnly(): Boolean {
    val imageUrl = extractGeneratedImageUrl() ?: return false
    val withoutMarkdown = replace(Regex("""!\[[^\]]*]\([^)]+\)"""), "")
    return withoutMarkdown.replace(imageUrl, "").isBlank()
}

private fun String.withoutGeneratedImageMarkdown(): String {
    return replace(Regex("""!\[[^\]]*]\([^)]+\)"""), "").trim()
}

@Composable
fun ThinkingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier.widthIn(max = 100.dp),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = dot1Alpha), androidx.compose.foundation.shape.CircleShape))
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = dot2Alpha), androidx.compose.foundation.shape.CircleShape))
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = dot3Alpha), androidx.compose.foundation.shape.CircleShape))
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    isStreaming: Boolean,
    onStopClick: () -> Unit,
    onPlaceholderClick: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = { onPlaceholderClick("Camera") },
                enabled = !isStreaming,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            BasicTextField(
                value = message,
                onValueChange = onMessageChange,
                enabled = enabled && !isStreaming,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp, horizontal = 8.dp)
                    .heightIn(min = 20.dp, max = 150.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (message.isEmpty()) {
                            Text(
                                "Ask M2HAI...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (isStreaming) {
                IconButton(
                    onClick = onStopClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop response",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (message.isNotBlank()) {
                IconButton(
                    onClick = onSendClick,
                    enabled = enabled,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row {
                    IconButton(
                        onClick = { onPlaceholderClick("Voice") },
                        enabled = enabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = { onPlaceholderClick("Audio") },
                        enabled = enabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = "Audio", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
@Composable
fun SuggestionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyLarge)
    }
}
