package com.m2h.m2haichatbot.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.m2h.m2haichatbot.domain.model.Chat
import com.m2h.m2haichatbot.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sidebar(
    chats: List<Chat>,
    currentChatId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onChatSelect: (Chat) -> Unit,
    onNewChat: () -> Unit,
    onLogout: () -> Unit,
    onDeleteChat: (String) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onTogglePin: (Chat) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Canvas,
        drawerContentColor = Ink,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPrimary,
                    contentColor = OnPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search chats...", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CoralPrimary,
                    unfocusedBorderColor = Hairline,
                    focusedContainerColor = SurfaceSoft,
                    unfocusedContainerColor = SurfaceSoft
                ),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Recent Conversations",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            // Chat List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(chats) { chat ->
                    SidebarItem(
                        chat = chat,
                        isSelected = chat.id == currentChatId,
                        onClick = { onChatSelect(chat) },
                        onDelete = { onDeleteChat(chat.id) },
                        onTogglePin = { onTogglePin(chat) }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Hairline)

            // Bottom Actions
            SidebarItemBase(
                title = "Settings",
                icon = Icons.Outlined.Settings,
                onClick = { /* Navigate to Settings */ }
            )
            SidebarItemBase(
                title = "Log Out",
                icon = Icons.Outlined.Logout,
                onClick = onLogout,
                color = Error
            )
        }
    }
}

@Composable
private fun SidebarItem(
    chat: Chat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = if (isSelected) SurfaceCard else Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (chat.isPinned) Icons.Default.PushPin else Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) CoralPrimary else Muted
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (isSelected) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp), tint = Muted)
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Canvas)
        ) {
            DropdownMenuItem(
                text = { Text(if (chat.isPinned) "Unpin" else "Pin") },
                onClick = { 
                    onTogglePin()
                    showMenu = false 
                },
                leadingIcon = { Icon(Icons.Default.PushPin, null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = Error) },
                onClick = { 
                    onDelete()
                    showMenu = false 
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Error) }
            )
        }
    }
}

@Composable
private fun SidebarItemBase(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = Ink
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}
