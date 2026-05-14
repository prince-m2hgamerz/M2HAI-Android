package com.m2h.m2haichatbot.ui.chat.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.m2h.m2haichatbot.ui.theme.CoralPrimary
import com.m2h.m2haichatbot.ui.theme.Ink
import com.m2h.m2haichatbot.ui.theme.SurfaceCard

@Composable
fun ChatInput(
    onSendMessage: (String, List<Uri>) -> Unit,
    onVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val selectedAttachments = remember { mutableStateListOf<Uri>() }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedAttachments.addAll(uris)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            if (selectedAttachments.isNotEmpty()) {
                AttachmentPreviewRow(
                    attachments = selectedAttachments,
                    onRemove = { selectedAttachments.remove(it) }
                )
            }
            
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { launcher.launch("*/*") }) {
                    Icon(
                        Icons.Outlined.AddCircleOutline,
                        contentDescription = "Attach",
                        tint = Ink.copy(alpha = 0.6f)
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    cursorBrush = SolidColor(CoralPrimary),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                "Message M2HAI...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ink.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )

                if (text.isNotEmpty() || selectedAttachments.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSendMessage(text, selectedAttachments.toList())
                            text = ""
                            selectedAttachments.clear()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = CoralPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(onClick = onVoiceInput) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Voice Input",
                            tint = Ink.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentPreviewRow(
    attachments: List<Uri>,
    onRemove: (Uri) -> Unit
) {
    // Basic implementation, can be expanded with thumbnails
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { uri ->
            AssistChip(
                onClick = { onRemove(uri) },
                label = { Text(uri.lastPathSegment ?: "File", maxLines = 1) },
                trailingIcon = { Icon(Icons.Default.Send, "Remove", modifier = Modifier.size(14.dp)) } // Replace with close icon
            )
        }
    }
}
