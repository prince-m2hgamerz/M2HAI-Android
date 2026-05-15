package com.m2h.m2haichatbot.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.m2h.m2haichatbot.presentation.auth.AuthViewModel
import com.m2h.m2haichatbot.presentation.auth.LoginScreen
import com.m2h.m2haichatbot.presentation.auth.SignUpScreen
import com.m2h.m2haichatbot.presentation.chat.ChatScreen
import com.m2h.m2haichatbot.presentation.home.HomeScreen
import com.m2h.m2haichatbot.presentation.settings.SettingsScreen

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.m2h.m2haichatbot.presentation.home.HomeViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Settings : Screen("settings")
}

// Removed
// Removed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // We only want to show the drawer on Home and Chat screens.
    // For simplicity, we can provide it everywhere except Login/SignUp if we wanted,
    // but the drawer can just be disabled on auth screens by not providing a menu button.
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Content (Recents)
                val homeViewModel: HomeViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                val state by homeViewModel.state.collectAsState()
                
                var searchQuery by remember { mutableStateOf("") }
                val filteredChats = remember(state.chats, searchQuery) {
                    if (searchQuery.isBlank()) state.chats
                    else state.chats.filter { it.title.contains(searchQuery, ignoreCase = true) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("M2HAI", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search chats", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // New Chat Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clickable {
                                navController.navigate(Screen.Home.route)
                                scope.launch { drawerState.close() }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("New Chat", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Projects", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Images", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("More", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            }
                            
                        Spacer(modifier = Modifier.height(24.dp))
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No chats found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                        
                        items(filteredChats, key = { it.id }) { chat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        navController.navigate(Screen.Chat.createRoute(chat.id))
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chat.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                var showChatMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showChatMenu = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    }

                                    var showRenameDialog by remember { mutableStateOf(false) }
                                    var newTitle by remember { mutableStateOf(chat.title) }

                                    if (showRenameDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showRenameDialog = false },
                                            title = { Text("Rename Chat", color = MaterialTheme.colorScheme.onSurface) },
                                            text = {
                                                OutlinedTextField(
                                                    value = newTitle,
                                                    onValueChange = { newTitle = it },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                    )
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    homeViewModel.renameChat(chat.id, newTitle)
                                                    showRenameDialog = false
                                                }) {
                                                    Text("Rename")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showRenameDialog = false }) {
                                                    Text("Cancel")
                                                }
                                            },
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showChatMenu,
                                        onDismissRequest = { showChatMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                showRenameDialog = true
                                                showChatMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                homeViewModel.deleteChat(chat.id)
                                                showChatMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Footer
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                navController.navigate(Screen.Settings.route)
                                scope.launch { drawerState.close() }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            // Note: AuthViewModel provides the current user profile
                            val currentUser = authViewModel.state.collectAsState().value.user
                            if (currentUser?.avatarUrl != null) {
                                coil.compose.AsyncImage(
                                    model = currentUser.avatarUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                val initials = currentUser?.fullName?.take(1)?.uppercase() ?: "U"
                                Text(
                                    text = initials, 
                                    color = MaterialTheme.colorScheme.onPrimaryContainer, 
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Account Settings",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "View profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { 
                            homeViewModel.logout {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
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

            // HomeScreen is now the empty chat screen
            composable(Screen.Home.route) {
                ChatScreen(
                    chatId = "new",
                    onNavigateBack = { },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNewChatCreated = { newChatId -> 
                        navController.navigate(Screen.Chat.createRoute(newChatId)) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId = chatId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNewChatCreated = { }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
