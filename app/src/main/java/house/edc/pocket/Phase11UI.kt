package house.edc.pocket

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Scanner
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

enum class IncomingViewMode(val label: String) {
    FLAT("All files"),
    SESSIONS("Sessions"),
    GALLERY("Gallery"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodoExtraEditorDialog(
    item: TodoItem,
    extra: TodoExtra,
    latestClipUrl: String,
    onDismiss: () -> Unit,
    onSave: (TodoExtra) -> Unit,
) {
    var note by remember(item.id) { mutableStateOf(extra.note.ifBlank { item.note }) }
    var due by remember(item.id) { mutableStateOf(extra.dueDate.ifBlank { item.dueDate }) }
    var category by remember(item.id) { mutableStateOf(extra.category.ifBlank { item.category }) }
    var linked by remember(item.id) {
        mutableStateOf(extra.linkedClipUrl.ifBlank { item.linkedClipUrl })
    }
    var recurrence by remember(item.id) {
        mutableStateOf(
            RecurrenceRule.from(item.recurrence.ifBlank { extra.recurrence.name }),
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("List item details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.text, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Due date") },
                    placeholder = { Text("2026-09-10") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Aisle / category") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = linked,
                    onValueChange = { linked = it },
                    label = { Text("Linked clip URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (latestClipUrl.isNotBlank() && linked.isBlank()) {
                    TextButton(onClick = { linked = latestClipUrl }) {
                        Icon(Icons.Outlined.Link, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Link latest clip")
                    }
                }
                Text("Repeat", style = MaterialTheme.typography.labelMedium, color = EdcMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceRule.entries.forEach { rule ->
                        FilterChip(
                            selected = recurrence == rule,
                            onClick = { recurrence = rule },
                            label = { Text(rule.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0E3A43),
                                selectedLabelColor = EdcAccent,
                            ),
                        )
                    }
                }
                if (item.subItems.isNotEmpty()) {
                    Text("Sub-items", style = MaterialTheme.typography.labelMedium, color = EdcMuted)
                    item.subItems.forEach { sub ->
                        Text(
                            text = "${if (sub.done) "☑" else "☐"} ${sub.text}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        TodoExtra(
                            note = note.trim(),
                            dueDate = due.trim(),
                            category = category.trim(),
                            linkedClipUrl = linked.trim(),
                            linkedClipText = item.linkedClipText,
                            recurrence = recurrence,
                            subItems = item.subItems,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
internal fun TodoRowRich(
    item: TodoItem,
    isPinned: Boolean,
    onToggle: (TodoItem) -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onOpenLinkedClip: ((String) -> Unit)? = null,
    onDelete: ((TodoItem) -> Unit)? = null,
    onOpenDashboard: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(item) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onEdit() },
        ) {
            Text(
                text = item.text,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            )
            val aisle = GroceryAisle.infer(item.text, item.category)
            if (aisle != GroceryAisle.OTHER || item.category.isNotBlank()) {
                Text(
                    text = item.category.ifBlank { aisle.label },
                    style = MaterialTheme.typography.labelSmall,
                    color = EdcCyan,
                )
            }
            if (item.dueDate.isNotBlank()) {
                Text(
                    text = "Due ${item.dueDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EdcMuted,
                )
            }
            if (item.note.isNotBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item.subItems.forEach { sub ->
                Text(
                    text = "${if (sub.done) "☑" else "☐"} ${sub.text}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EdcMuted,
                )
            }
            if (item.recurrence.isNotBlank() && !item.recurrence.equals("NONE", true)) {
                Text(
                    text = "Repeats ${item.recurrence.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EdcMuted,
                )
            }
            val meta = metaLine(item.from, item.ts)
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelSmall, color = EdcMuted)
            }
        }
        if (item.linkedClipUrl.isNotBlank() && onOpenLinkedClip != null) {
            TextButton(onClick = { onOpenLinkedClip(item.linkedClipUrl) }) {
                Text("Clip")
            }
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                if (isPinned) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isPinned) "Unpin" else "Pin",
                tint = if (isPinned) EdcAccent else EdcMuted,
            )
        }
        if (onOpenDashboard != null) {
            TextButton(onClick = onOpenDashboard) { Text("Dashboard") }
        }
        if (item.done && onDelete != null) {
            TextButton(onClick = { onDelete(item) }) { Text("Remove") }
        }
    }
}

@Composable
internal fun AisleGroupedTodoList(
    open: List<TodoItem>,
    done: List<TodoItem>,
    sortMode: ListSortMode,
    pinnedTodoIds: Set<String>,
    listEnabled: Boolean,
    deleteEnabled: Boolean,
    showDashboardLinks: Boolean,
    onToggle: (TodoItem) -> Unit,
    onTogglePin: (String) -> Unit,
    onEdit: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    onOpenDashboard: (TodoItem) -> Unit,
    onOpenLinkedClip: (String) -> Unit,
) {
    if (sortMode == ListSortMode.BY_AISLE) {
        groupTodosByAisle(open).forEach { (aisle, items) ->
            SectionLabel(aisle.label)
            items.forEach { item ->
                SwipeTodoRow(enabled = listEnabled, onComplete = { onToggle(item) }) {
                    TodoRowRich(
                        item = item,
                        isPinned = item.id in pinnedTodoIds,
                        onToggle = onToggle,
                        onTogglePin = { onTogglePin(item.id) },
                        onEdit = { onEdit(item) },
                        onOpenLinkedClip = onOpenLinkedClip,
                        onOpenDashboard = if (showDashboardLinks) {
                            { onOpenDashboard(item) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    } else {
        open.forEach { item ->
            SwipeTodoRow(enabled = listEnabled, onComplete = { onToggle(item) }) {
                TodoRowRich(
                    item = item,
                    isPinned = item.id in pinnedTodoIds,
                    onToggle = onToggle,
                    onTogglePin = { onTogglePin(item.id) },
                    onEdit = { onEdit(item) },
                    onOpenLinkedClip = onOpenLinkedClip,
                    onOpenDashboard = if (showDashboardLinks) {
                        { onOpenDashboard(item) }
                    } else {
                        null
                    },
                )
            }
        }
    }
    if (done.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionLabel("Done")
        done.forEach { item ->
            TodoRowRich(
                item = item,
                isPinned = item.id in pinnedTodoIds,
                onToggle = onToggle,
                onTogglePin = { onTogglePin(item.id) },
                onEdit = { onEdit(item) },
                onDelete = if (deleteEnabled) onDelete else null,
                onOpenDashboard = if (showDashboardLinks) {
                    { onOpenDashboard(item) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
internal fun IncomingBulkBar(
    selectedCount: Int,
    onDownloadZip: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    if (selectedCount == 0) return
    Card(
        colors = CardDefaults.cardColors(containerColor = EdcSurfaceHi),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("$selectedCount selected", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onDownloadZip) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Zip")
            }
            OutlinedButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Delete")
            }
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

@Composable
internal fun IncomingScanRow(
    onDocumentScan: () -> Unit,
    onBarcodeScan: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onDocumentScan) {
            Icon(Icons.Outlined.Scanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan doc")
        }
        OutlinedButton(onClick = onBarcodeScan) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan code")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IncomingViewModeChips(
    mode: IncomingViewMode,
    onModeChange: (IncomingViewMode) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IncomingViewMode.entries.forEach { entry ->
            FilterChip(
                selected = mode == entry,
                onClick = { onModeChange(entry) },
                label = { Text(entry.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0E3A43),
                    selectedLabelColor = EdcAccent,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionGalleryGrid(
    groups: List<SessionGroup>,
    incomingUrl: (DropItem) -> String?,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onToggleSelect: (DropItem) -> Unit,
    onOpenDrop: (DropItem) -> Unit,
) {
    groups.forEach { group ->
        if (group.folder.isNotBlank()) {
            SectionLabel(group.folder)
        } else if (groups.size > 1) {
            SectionLabel("Unsorted")
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(108.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height((((group.drops.size.coerceAtMost(6) / 3) + 1).coerceAtLeast(1) * 120).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            userScrollEnabled = false,
        ) {
            items(group.drops, key = { it.id + it.name }) { drop ->
                SessionThumb(
                    drop = drop,
                    imageUrl = incomingUrl(drop)?.takeIf { drop.isImage() },
                    selected = dropSelectionKey(drop) in selectedIds,
                    selectionMode = selectionMode,
                    onToggleSelect = { onToggleSelect(drop) },
                    onOpen = { onOpenDrop(drop) },
                )
            }
        }
    }
}

@Composable
private fun SessionThumb(
    drop: DropItem,
    imageUrl: String?,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelect() else onOpen()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) EdcSurfaceHi else EdcSurface,
        ),
    ) {
        Column {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                    contentDescription = drop.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = drop.name.take(12),
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
            }
        }
    }
}

@Composable
internal fun RichDropCard(
    drop: DropItem,
    imageUrl: String?,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = EdcSurface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) onToggleSelect() else onOpen()
            },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                }
                when {
                    drop.isPdf() -> Text("PDF", color = EdcCyan)
                    drop.isVideo() -> Text("Video", color = EdcCyan)
                    drop.isAudio() -> Text("Audio", color = EdcCyan)
                    imageUrl != null -> AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = drop.name,
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(drop.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (drop.sessionFolder().isNotBlank()) {
                        Text(
                            text = drop.sessionFolder(),
                            style = MaterialTheme.typography.labelSmall,
                            color = EdcMuted,
                        )
                    }
                    Text(
                        text = metaLine(drop.from, drop.ts),
                        style = MaterialTheme.typography.labelSmall,
                        color = EdcMuted,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onSave) { Text("Save") }
            }
        }
    }
}

internal fun dropSelectionKey(drop: DropItem): String = drop.id.ifBlank { drop.name + drop.path }
