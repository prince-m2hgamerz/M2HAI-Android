package com.m2h.m2haichatbot.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.m2h.m2haichatbot.BuildConfig
import com.m2h.m2haichatbot.domain.model.Chat
import com.m2h.m2haichatbot.presentation.auth.AuthViewModel
import com.m2h.m2haichatbot.presentation.auth.LoginScreen
import com.m2h.m2haichatbot.presentation.auth.SignUpScreen
import com.m2h.m2haichatbot.presentation.chat.ChatScreen
import com.m2h.m2haichatbot.presentation.home.HomeViewModel
import com.m2h.m2haichatbot.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object ImagePrompt : Screen("image-prompt/{prompt}") {
        fun createRoute(prompt: String) = "image-prompt/${Uri.encode(prompt)}"
    }
    object Projects : Screen("projects")
    object Images : Screen("images")
    object More : Screen("more")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            M2HAIDrawer(
                homeViewModel = homeViewModel,
                authViewModel = authViewModel,
                onClose = { scope.launch { drawerState.close() } },
                onNavigate = { route ->
                    navController.navigate(route)
                    scope.launch { drawerState.close() }
                },
                onLogout = {
                    homeViewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                ChatScreen(
                    chatId = "new",
                    onNavigateBack = {},
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNewChatCreated = { chatId ->
                        navController.navigate(Screen.Chat.createRoute(chatId)) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { entry ->
                val chatId = entry.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId = chatId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNewChatCreated = {}
                )
            }

            composable(
                route = Screen.ImagePrompt.route,
                arguments = listOf(navArgument("prompt") { type = NavType.StringType })
            ) { entry ->
                val prompt = Uri.decode(entry.arguments?.getString("prompt") ?: "")
                ChatScreen(
                    chatId = "new",
                    initialMessage = prompt,
                    autoSendInitialMessage = true,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNewChatCreated = { chatId ->
                        navController.navigate(Screen.Chat.createRoute(chatId)) {
                            popUpTo(Screen.ImagePrompt.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Projects.route) {
                val state by homeViewModel.state.collectAsState()
                UtilityListScreen(
                    title = "Projects",
                    subtitle = "Pinned and active project conversations",
                    icon = Icons.Default.Folder,
                    items = state.chats.filter { it.isPinned || !it.isArchived },
                    emptyText = "No project chats yet",
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onItemClick = { navController.navigate(Screen.Chat.createRoute(it.id)) }
                )
            }

            composable(Screen.Images.route) {
                val state by homeViewModel.state.collectAsState()
                ImagesScreen(
                    imageChats = state.chats.filter {
                        it.title.contains("image", true) ||
                            it.title.contains("photo", true) ||
                            it.title.contains("draw", true) ||
                            it.title.contains("logo", true) ||
                            it.modelId.contains("flux", true)
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onItemClick = { navController.navigate(Screen.Chat.createRoute(it.id)) },
                    onGenerate = { prompt -> navController.navigate(Screen.ImagePrompt.createRoute(prompt)) }
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onNewChat = { navController.navigate(Screen.Home.route) },
                    onImages = { navController.navigate(Screen.Images.route) },
                    onProjects = { navController.navigate(Screen.Projects.route) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M2HAIDrawer(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val state by homeViewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredChats = remember(state.chats, searchQuery) {
        val sortedChats = state.chats.sortedWith(
            compareByDescending<Chat> { it.isPinned }
                .thenBy { it.isArchived }
                .thenByDescending { it.updatedAt }
        )
        if (searchQuery.isBlank()) {
            sortedChats
        } else {
            sortedChats.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("M2HAI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search chats") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            DrawerActionRow(Icons.Default.Add, "New Chat") { onNavigate(Screen.Home.route) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    DrawerActionRow(Icons.Default.Folder, "Projects") { onNavigate(Screen.Projects.route) }
                    DrawerActionRow(Icons.Default.Image, "Images") { onNavigate(Screen.Images.route) }
                    DrawerActionRow(Icons.Default.MoreHoriz, "More") { onNavigate(Screen.More.route) }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Recent Chats",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                if (filteredChats.isEmpty()) {
                    item {
                        Text(
                            "No chats found",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                items(filteredChats, key = { it.id }) { chat ->
                    ChatDrawerRow(
                        chat = chat,
                        onOpen = { onNavigate(Screen.Chat.createRoute(chat.id)) },
                        onRename = { title -> homeViewModel.renameChat(chat.id, title) },
                        onTogglePin = { homeViewModel.togglePin(chat.id, !chat.isPinned) },
                        onToggleArchive = { homeViewModel.toggleArchive(chat.id, !chat.isArchived) },
                        onDelete = { homeViewModel.deleteChat(chat.id) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigate(Screen.Settings.route) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val user = authState.user
                    if (user?.avatarUrl != null) {
                        coil.compose.AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(user?.fullName?.take(1)?.uppercase() ?: "U", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Account Settings", fontWeight = FontWeight.SemiBold)
                    Text("View profile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ChatDrawerRow(
    chat: Chat,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var title by remember(chat.id) { mutableStateOf(chat.title) }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Chat menu", modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (chat.isArchived) "Restore" else "Archive") },
                    onClick = {
                        showMenu = false
                        onToggleArchive()
                    },
                    leadingIcon = {
                        Icon(
                            if (chat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showRename = true
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) onRename(title.trim())
                    showRename = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtilityListScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Chat>,
    emptyText: String,
    onOpenDrawer: () -> Unit,
    onItemClick: (Chat) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptySection(modifier = Modifier.fillMaxSize().padding(padding), icon = icon, text = emptyText)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { chat ->
                    ChatSectionCard(icon = icon, chat = chat, onClick = { onItemClick(chat) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagesScreen(
    imageChats: List<Chat>,
    onOpenDrawer: () -> Unit,
    onItemClick: (Chat) -> Unit,
    onGenerate: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Images", fontWeight = FontWeight.Bold)
                        Text("Generate and manage image chats", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Create Image", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Describe the image to generate...") },
                            minLines = 2
                        )
                        Button(
                            onClick = { onGenerate("Generate an image of ${prompt.trim()}") },
                            enabled = prompt.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate")
                        }
                    }
                }
            }

            item {
                Text("Image Chats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            if (imageChats.isEmpty()) {
                item { EmptySection(icon = Icons.Default.Image, text = "Generated image chats will appear here") }
            } else {
                items(imageChats, key = { it.id }) { chat ->
                    ChatSectionCard(icon = Icons.Default.Image, chat = chat, onClick = { onItemClick(chat) })
                }
            }
        }
    }
}

@Composable
private fun EmptySection(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChatSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chat: Chat,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(chat.modelId, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreScreen(
    onOpenDrawer: () -> Unit,
    onSettings: () -> Unit,
    onNewChat: () -> Unit,
    onImages: () -> Unit,
    onProjects: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MoreAction(Icons.Default.Add, "New Chat", "Start a fresh conversation", onNewChat) }
            item { MoreAction(Icons.Default.Folder, "Projects", "Open project conversations", onProjects) }
            item { MoreAction(Icons.Default.Image, "Images", "Generate and review image chats", onImages) }
            item { MoreAction(Icons.Default.Settings, "Settings", "Profile, theme, and provider keys", onSettings) }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("M2HAI ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                        Text("Version code ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
