package com.m2h.m2haichatbot.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.m2h.m2haichatbot.domain.model.AIModel
import com.m2h.m2haichatbot.ui.theme.Canvas
import com.m2h.m2haichatbot.ui.theme.CoralPrimary
import com.m2h.m2haichatbot.ui.theme.Ink
import com.m2h.m2haichatbot.ui.theme.Muted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorSheet(
    availableModels: List<AIModel>,
    selectedModelId: String,
    onModelSelect: (AIModel) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Select AI Model",
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn {
                items(availableModels) { model ->
                    ModelItem(
                        model = model,
                        isSelected = model.id == selectedModelId,
                        onClick = {
                            onModelSelect(model)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelItem(
    model: AIModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            model.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = CoralPrimary
            )
        }
    }
}
