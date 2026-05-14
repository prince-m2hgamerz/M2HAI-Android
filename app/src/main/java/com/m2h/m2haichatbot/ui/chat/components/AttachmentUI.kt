package com.m2h.m2haichatbot.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.m2h.m2haichatbot.domain.model.Attachment
import com.m2h.m2haichatbot.ui.theme.SurfaceDarkSoft

@Composable
fun MessageAttachments(attachments: List<Attachment>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        attachments.forEach { attachment ->
            if (attachment.type == "image") {
                AsyncImage(
                    model = attachment.url,
                    contentDescription = attachment.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    color = SurfaceDarkSoft,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = attachment.name ?: "Document",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
