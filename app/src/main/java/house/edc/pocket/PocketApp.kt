package house.edc.pocket

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Top-level UI for EDC pocket. It is a thin HTTP client for the house host:
 * Clip (latest clipboard), List (shopping / to-do), Send (text or photo), and
 * Settings (identity + host preset). All network work is dispatched to IO and
 * failures surface as a status line rather than crashing the app.
 */
@Composable
fun PocketApp(settings: EdcSettings, store: SettingsStore, client: EdcClient) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var tab by remember { mutableIntStateOf(0) }
        var snapshot by remember { mutableStateOf(HostSnapshot()) }
        var status by remember { mutableStateOf("Pick a host, then Test connection.") }
        var busy by remember { mutableStateOf(false) }

        fun refresh() {
            if (settings.baseUrl.isBlank()) {
                status = "Set a host URL in Settings first."
                return
            }
            scope.launch {
                busy = true
                status = "Loading from ${settings.baseUrl}…"
                val result = withContext(Dispatchers.IO) {
                    runCatching { client.load(settings.baseUrl, settings.identity) }
                }
                busy = false
                result
                    .onSuccess {
                        snapshot = it
                        status = "Updated as ${settings.identity}."
                    }
                    .onFailure { status = it.message ?: "Host unreachable" }
            }
        }

        // Runs a suspend action off the main thread, shows the message it
        // returns, and optionally refreshes the snapshot afterward.
        fun act(refreshAfter: Boolean = true, work: suspend () -> String) {
            scope.launch {
                busy = true
                val result = withContext(Dispatchers.IO) { runCatching { work() } }
                busy = false
                result
                    .onSuccess {
                        status = it
                        if (refreshAfter) refresh()
                    }
                    .onFailure { status = it.message ?: "Action failed" }
            }
        }

        val tabs = listOf("Clip", "List", "Send", "Settings")
        Scaffold { insets ->
            Column(
                Modifier
                    .padding(insets)
                    .fillMaxSize(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("EDC pocket", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    if (busy) CircularProgressIndicator(Modifier.height(20.dp).width(20.dp))
                }
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(title) },
                        )
                    }
                }
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider()
                Box(
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
                ) {
                    when (tab) {
                        0 -> ClipTab(snapshot, onRefresh = ::refresh) { text ->
                            act {
                                client.sendText(settings.baseUrl, settings.identity, text)
                                "Sent to clipboard."
                            }
                        }
                        1 -> ListTab(
                            snapshot = snapshot,
                            onRefresh = ::refresh,
                            onAdd = { text ->
                                act {
                                    client.addTodo(settings.baseUrl, settings.identity, text)
                                    "Added to list."
                                }
                            },
                            onToggle = { item ->
                                act {
                                    val newDone = !item.done
                                    client.toggleTodo(
                                        settings.baseUrl,
                                        settings.identity,
                                        item.id,
                                        newDone,
                                    )
                                    if (newDone) "Ticked." else "Un-ticked."
                                }
                            },
                        )
                        2 -> SendTab(
                            onSendText = { text ->
                                act {
                                    client.sendText(settings.baseUrl, settings.identity, text)
                                    "Sent to clipboard."
                                }
                            },
                            onSendPhoto = { uri, name ->
                                act {
                                    client.uploadImage(settings.baseUrl, settings.identity, uri, name)
                                    "Photo sent to Incoming."
                                }
                            },
                        )
                        else -> SettingsTab(
                            settings = settings,
                            onIdentity = { value ->
                                act(refreshAfter = false) {
                                    store.setIdentity(value)
                                    "Identity set to $value."
                                }
                            },
                            onPreset = { preset ->
                                act(refreshAfter = false) {
                                    store.setPreset(preset)
                                    "Host set to ${preset.label}."
                                }
                            },
                            onCustomUrl = { value ->
                                act(refreshAfter = false) {
                                    store.setCustomUrl(value)
                                    "Custom URL saved."
                                }
                            },
                            onTest = {
                                act(refreshAfter = false) {
                                    client.probe(settings.baseUrl, settings.identity)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipTab(
    snapshot: HostSnapshot,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var draft by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Latest clipboard", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val latest = snapshot.latest
        Card(Modifier.fillMaxWidth()) {
            Text(
                latest?.text?.ifBlank { "(empty)" } ?: "Nothing yet.",
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = onRefresh) { Text("Refresh") }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = latest?.text?.isNotBlank() == true,
                onClick = { clipboard.setText(AnnotatedString(latest?.text.orEmpty())) },
            ) { Text("Copy") }
        }
        Spacer(Modifier.height(20.dp))
        Text("Send to clipboard", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Text or link") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = draft.isNotBlank(),
            onClick = {
                onSend(draft.trim())
                draft = ""
            },
        ) { Text("Send") }
        Spacer(Modifier.height(20.dp))
        Text("History", style = MaterialTheme.typography.titleMedium)
        if (snapshot.history.isEmpty()) {
            Text("No history.", style = MaterialTheme.typography.bodyMedium)
        } else {
            snapshot.history.take(20).forEach { entry ->
                Text(
                    "• ${entry.text}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ListTab(
    snapshot: HostSnapshot,
    onRefresh: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (TodoItem) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Add item") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = draft.isNotBlank(),
                onClick = {
                    onAdd(draft.trim())
                    draft = ""
                },
                modifier = Modifier.align(Alignment.CenterVertically),
            ) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        Spacer(Modifier.height(8.dp))
        if (snapshot.todos.isEmpty()) {
            Text("List is empty.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(snapshot.todos, key = { it.id.ifBlank { it.text } }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
                        Spacer(Modifier.width(8.dp))
                        Text(item.text, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SendTab(
    onSendText: (String) -> Unit,
    onSendPhoto: (Uri, String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onSendPhoto(uri, uri.lastPathSegment ?: "photo.jpg")
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Send text or link", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Text or link") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = draft.isNotBlank(),
            onClick = {
                onSendText(draft.trim())
                draft = ""
            },
        ) { Text("Send to clipboard") }
        Spacer(Modifier.height(24.dp))
        Text("Send a photo", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { picker.launch("image/*") }) { Text("Choose from library") }
    }
}

@Composable
private fun SettingsTab(
    settings: EdcSettings,
    onIdentity: (String) -> Unit,
    onPreset: (HostPreset) -> Unit,
    onCustomUrl: (String) -> Unit,
    onTest: () -> Unit,
) {
    var custom by remember(settings.customUrl) { mutableStateOf(settings.customUrl) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Identity", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Mike", "Mhairi").forEach { name ->
                val selected = settings.identity == name
                if (selected) {
                    Button(onClick = { onIdentity(name) }) { Text(name) }
                } else {
                    OutlinedButton(onClick = { onIdentity(name) }) { Text(name) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Host", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        HostPreset.entries.forEach { preset ->
            val selected = settings.preset == preset
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Button(onClick = { onPreset(preset) }) { Text(preset.label) }
                } else {
                    OutlinedButton(onClick = { onPreset(preset) }) { Text(preset.label) }
                }
                Spacer(Modifier.width(12.dp))
                if (preset.url.isNotBlank()) {
                    Text(preset.url, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        if (settings.preset == HostPreset.CUSTOM) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                label = { Text("Custom URL (http://host:port)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = custom.isNotBlank(),
                onClick = { onCustomUrl(custom.trim()) },
            ) { Text("Save URL") }
        }
        Spacer(Modifier.height(20.dp))
        Text("Active host: ${settings.baseUrl.ifBlank { "(none)" }}")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onTest) { Text("Test connection") }
    }
}
