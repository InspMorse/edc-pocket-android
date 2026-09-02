package house.edc.pocket

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.Locale
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PocketTab(val label: String) {
    CLIP("Clip"),
    LIST("List"),
    SEND("Send"),
    SETTINGS("Settings"),
}

private val identities = listOf("Mike", "Mhairi")
private val clipFilters = listOf("All", "Mike", "Mhairi", "EDC")
private const val pollMs = 5_000L
private const val clipPreviewChars = 220

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketApp(
    settings: EdcSettings,
    store: SettingsStore,
    client: EdcClient,
) {
    EdcPocketTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        var tab by rememberSaveable { mutableStateOf(PocketTab.CLIP.name) }
        val currentTab = PocketTab.entries.find { it.name == tab } ?: PocketTab.CLIP
        var snapshot by remember { mutableStateOf(HostSnapshot()) }
        var loading by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf<String?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var stale by remember { mutableStateOf(false) }
        val resumeTick = rememberResumeTick()

        suspend fun refresh(silent: Boolean = false) {
            val base = settings.baseUrl
            if (base.isBlank()) {
                error = "Set a host URL in Settings."
                status = null
                if (!silent) snapshot = HostSnapshot()
                stale = false
                return
            }
            if (!silent) loading = true
            val result = withContext(Dispatchers.IO) {
                runCatching { client.load(base, settings.identity) }
            }
            if (!silent) loading = false
            result.fold(
                onSuccess = {
                    snapshot = it
                    error = null
                    stale = false
                    status = "Connected as ${settings.identity}"
                },
                onFailure = {
                    error = it.message ?: "Host unreachable"
                    status = null
                    stale = snapshot.latest != null ||
                        snapshot.history.isNotEmpty() ||
                        snapshot.todos.isNotEmpty() ||
                        snapshot.drops.isNotEmpty()
                },
            )
        }

        suspend fun <T> hostCall(okMessage: String?, block: () -> T) {
            val base = settings.baseUrl
            if (base.isBlank()) {
                error = "Set a host URL in Settings."
                return
            }
            loading = true
            val result = withContext(Dispatchers.IO) { runCatching { block() } }
            loading = false
            result.fold(
                onSuccess = {
                    error = null
                    if (okMessage != null) snackbar.showSnackbar(okMessage)
                    refresh()
                },
                onFailure = {
                    error = it.message ?: "Request failed"
                },
            )
        }

        LaunchedEffect(settings.baseUrl, settings.identity, resumeTick) {
            refresh()
        }

        LaunchedEffect(settings.baseUrl, settings.identity) {
            while (isActive) {
                delay(pollMs)
                refresh(silent = true)
            }
        }

        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = EdcBg,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("EDC pocket")
                            Text(
                                text = settings.identity + " · " + hostLabel(settings),
                                style = MaterialTheme.typography.labelSmall,
                                color = EdcMuted,
                            )
                        }
                    },
                    actions = {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp,
                                color = EdcCyan,
                            )
                        }
                        IconButton(onClick = { scope.launch { refresh() } }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EdcBg,
                        titleContentColor = EdcInk,
                        actionIconContentColor = EdcCyan,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = EdcSurface) {
                    PocketTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentTab == item,
                            onClick = { tab = item.name },
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        PocketTab.CLIP -> Icons.Outlined.ContentPaste
                                        PocketTab.LIST -> Icons.Outlined.Checklist
                                        PocketTab.SEND -> Icons.AutoMirrored.Outlined.Send
                                        PocketTab.SETTINGS -> Icons.Outlined.Settings
                                    },
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = EdcCyan,
                    )
                }
                val banner = when {
                    stale && error != null -> "$error · showing last known"
                    else -> error ?: status
                }
                if (banner != null) {
                    Text(
                        text = banner,
                        color = when {
                            stale -> Color(0xFFFFBB33)
                            error != null -> MaterialTheme.colorScheme.error
                            else -> EdcMuted
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                when (currentTab) {
                    PocketTab.CLIP -> ClipPane(
                        latest = snapshot.latest,
                        history = snapshot.history,
                        onCopy = { text ->
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("EDC", text))
                            scope.launch { snackbar.showSnackbar("Copied") }
                        },
                        onShare = { text ->
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, "Share clip"))
                        },
                        onOpenUrl = { openUrl(context, it) },
                        onSend = { text ->
                            scope.launch {
                                hostCall("Sent to clipboard") {
                                    client.sendText(settings.baseUrl, settings.identity, text)
                                }
                            }
                        },
                    )
                    PocketTab.LIST -> ListPane(
                        todos = snapshot.todos,
                        onAdd = { text ->
                            scope.launch {
                                hostCall("Added to list") {
                                    client.addTodo(settings.baseUrl, settings.identity, text)
                                }
                            }
                        },
                        onToggle = { item ->
                            val next = !item.done
                            snapshot = snapshot.copy(
                                todos = snapshot.todos.map {
                                    if (it.id == item.id) it.copy(done = next) else it
                                },
                            )
                            scope.launch {
                                hostCall(null) {
                                    client.toggleTodo(
                                        settings.baseUrl,
                                        settings.identity,
                                        item.id,
                                        next,
                                    )
                                }
                            }
                        },
                        onDelete = { item ->
                            snapshot = snapshot.copy(
                                todos = snapshot.todos.filter { it.id != item.id },
                            )
                            scope.launch {
                                hostCall("Removed") {
                                    client.deleteTodo(settings.baseUrl, settings.identity, item.id)
                                }
                            }
                        },
                        onShareList = {
                            scope.launch {
                                val base = settings.baseUrl
                                if (base.isBlank()) {
                                    error = "Set a host URL in Settings."
                                    return@launch
                                }
                                val text = withContext(Dispatchers.IO) {
                                    client.todoPlainText(base, settings.identity)
                                }
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(send, "Share list"))
                            }
                        },
                    )
                    PocketTab.SEND -> SendPane(
                        drops = snapshot.drops,
                        onSendClip = { text ->
                            scope.launch {
                                hostCall("Sent to clipboard") {
                                    client.sendText(settings.baseUrl, settings.identity, text)
                                }
                            }
                        },
                        onSendList = { text ->
                            scope.launch {
                                hostCall("Added to list") {
                                    client.addTodo(settings.baseUrl, settings.identity, text)
                                }
                            }
                        },
                        onUpload = { uri, name, session ->
                            scope.launch {
                                hostCall("Photo sent to Incoming") {
                                    client.uploadImage(
                                        settings.baseUrl,
                                        settings.identity,
                                        uri,
                                        name,
                                        session,
                                    )
                                }
                            }
                        },
                        onOpenDrop = { drop ->
                            val url = client.incomingOpenUrl(settings.baseUrl, drop)
                            if (url != null) openUrl(context, url) else scope.launch {
                                snackbar.showSnackbar("No download link for this file")
                            }
                        },
                    )
                    PocketTab.SETTINGS -> SettingsPane(
                        settings = settings,
                        onIdentity = { scope.launch { store.setIdentity(it) } },
                        onPreset = { scope.launch { store.setPreset(it) } },
                        onCustomUrl = { scope.launch { store.setCustomUrl(it) } },
                        onProbe = {
                            scope.launch {
                                val base = settings.baseUrl
                                if (base.isBlank()) {
                                    error = "Set a host URL first."
                                    return@launch
                                }
                                loading = true
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { client.probeHealth(base, settings.identity) }
                                }
                                loading = false
                                result.fold(
                                    onSuccess = {
                                        error = null
                                        status = it.summary()
                                        snackbar.showSnackbar(it.summary())
                                    },
                                    onFailure = {
                                        error = it.message ?: "Host unreachable"
                                    },
                                )
                            }
                        },
                        onFindHost = {
                            scope.launch {
                                loading = true
                                val found = withContext(Dispatchers.IO) {
                                    client.findReachableHost(settings, settings.identity)
                                }
                                loading = false
                                if (found == null) {
                                    error = "No host answered on Home or Away."
                                    return@launch
                                }
                                found.preset?.let { store.rememberWorkingPreset(it) }
                                error = null
                                status = found.health.summary() + " · ${found.baseUrl}"
                                snackbar.showSnackbar("Using ${found.preset?.label ?: "host"}")
                                refresh()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberResumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick += 1
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClipPane(
    latest: ClipEntry?,
    history: List<ClipEntry>,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filteredHistory = history.filter { entry ->
        filter == "All" || entry.from.equals(filter, ignoreCase = true)
    }.let { list ->
        if (latest == null) list
        else list.filter { it.id != latest.id || it.text != latest.text }
    }
    val showLatest = latest != null && (filter == "All" || latest.from.equals(filter, ignoreCase = true))
    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            clipFilters.forEach { name ->
                FilterChip(
                    selected = filter == name,
                    onClick = { filter = name },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0E3A43),
                        selectedLabelColor = EdcCyan,
                    ),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionLabel("Latest")
                Spacer(Modifier.height(8.dp))
                if (!showLatest) {
                    EmptyHint("Nothing on the house clipboard.")
                } else {
                    ClipCard(
                        entry = latest!!,
                        featured = true,
                        onCopy = onCopy,
                        onShare = onShare,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
            if (filteredHistory.isNotEmpty()) {
                item { SectionLabel("History") }
                items(filteredHistory, key = { it.id + it.ts }) { entry ->
                    ClipCard(
                        entry = entry,
                        featured = false,
                        onCopy = onCopy,
                        onShare = onShare,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        SendField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = "Type to send — manual only",
            actionLabel = "Send",
            onAction = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    onSend(text)
                    draft = ""
                }
            },
        )
    }
}

@Composable
private fun ListPane(
    todos: List<TodoItem>,
    onAdd: (String) -> Unit,
    onToggle: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    onShareList: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val open = todos.filter { !it.done }
    val done = todos.filter { it.done }
    Column(modifier = Modifier.fillMaxSize()) {
        SendField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = "Add to shopping / to-do",
            actionLabel = "Add",
            icon = Icons.Outlined.Add,
            onAction = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    onAdd(text)
                    draft = ""
                }
            },
        )
        if (todos.isNotEmpty()) {
            OutlinedButton(
                onClick = onShareList,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy / share list")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (todos.isEmpty()) {
                item { EmptyHint("List is empty.") }
            }
            items(open, key = { it.id }) { item ->
                TodoRow(item = item, onToggle = onToggle)
            }
            if (done.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Done")
                }
                items(done, key = { it.id }) { item ->
                    TodoRow(
                        item = item,
                        onToggle = onToggle,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun SendPane(
    drops: List<DropItem>,
    onSendClip: (String) -> Unit,
    onSendList: (String) -> Unit,
    onUpload: (Uri, String, String) -> Unit,
    onOpenDrop: (DropItem) -> Unit,
) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf("") }
    var session by rememberSaveable { mutableStateOf("") }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = captureUri
        if (ok && uri != null) onUpload(uri, "photo.jpg", session)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUpload(uri, uri.lastPathSegment ?: "photo.jpg", session)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Text or link")
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste a link or note") },
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        onSendClip(text)
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
            ) { Text("To clip") }
            OutlinedButton(
                onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        onSendList(text)
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
            ) { Text("To list") }
        }
        SectionLabel("Photo to Incoming")
        OutlinedTextField(
            value = session,
            onValueChange = { session = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Session folder (optional)") },
            placeholder = { Text("Drop/Sessions/2026-09-02 – Event") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val dir = File(context.cacheDir, "pics").apply { mkdirs() }
                    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file,
                    )
                    captureUri = uri
                    takePicture.launch(uri)
                },
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Library")
            }
        }
        SectionLabel("Incoming")
        if (drops.isEmpty()) {
            EmptyHint("No incoming files yet.")
        } else {
            drops.forEach { drop ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = EdcSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDrop(drop) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(drop.name, fontWeight = FontWeight.Medium)
                        Text(
                            text = metaLine(drop.from, drop.ts, formatSize(drop.size)),
                            style = MaterialTheme.typography.labelSmall,
                            color = EdcMuted,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPane(
    settings: EdcSettings,
    onIdentity: (String) -> Unit,
    onPreset: (HostPreset) -> Unit,
    onCustomUrl: (String) -> Unit,
    onProbe: () -> Unit,
    onFindHost: () -> Unit,
) {
    var customDraft by remember(settings.customUrl) { mutableStateOf(settings.customUrl) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Who is this phone")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            identities.forEach { name ->
                FilterChip(
                    selected = settings.identity == name,
                    onClick = { onIdentity(name) },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0E3A43),
                        selectedLabelColor = EdcCyan,
                    ),
                )
            }
        }
        SectionLabel("Host")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HostPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.preset == preset,
                    onClick = { onPreset(preset) },
                    label = { Text(preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0E3A43),
                        selectedLabelColor = EdcCyan,
                    ),
                )
            }
        }
        if (settings.preset == HostPreset.CUSTOM) {
            OutlinedTextField(
                value = customDraft,
                onValueChange = { customDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Custom URL") },
                placeholder = { Text("http://host:8765") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCustomUrl(customDraft) }),
            )
            Button(onClick = { onCustomUrl(customDraft.trim()) }) { Text("Save URL") }
        }
        Text(
            text = settings.baseUrl.ifBlank { "No host URL set" },
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = EdcMuted,
        )
        Button(onClick = onProbe, modifier = Modifier.fillMaxWidth()) { Text("Test connection") }
        OutlinedButton(onClick = onFindHost, modifier = Modifier.fillMaxWidth()) {
            Text("Find host (Home, then Away)")
        }
        Text(
            text = "Home Wi-Fi uses the house LAN. Away needs Tailscale on this phone. Find host tries both and keeps whichever answers.",
            style = MaterialTheme.typography.bodySmall,
            color = EdcMuted,
        )
    }
}

@Composable
private fun ClipCard(
    entry: ClipEntry,
    featured: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var expanded by rememberSaveable(entry.id + entry.ts) { mutableStateOf(false) }
    val long = entry.text.length > clipPreviewChars
    val shown = if (expanded || !long) entry.text else entry.text.take(clipPreviewChars) + "…"
    val directUrl = firstUrl(entry.text)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (featured) EdcSurfaceHi else EdcSurface,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            LinkifiedText(
                text = shown,
                color = EdcInk,
                onOpenUrl = onOpenUrl,
                modifier = Modifier.fillMaxWidth(),
            )
            if (long) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Show more")
                }
            }
            if (directUrl != null && !entry.text.trim().equals(directUrl, ignoreCase = true)) {
                TextButton(onClick = { onOpenUrl(directUrl) }) { Text("Open link") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = metaLine(entry.from.ifBlank { "EDC" }, entry.ts),
                    style = MaterialTheme.typography.labelSmall,
                    color = EdcMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { onShare(entry.text) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
                IconButton(onClick = { onCopy(entry.text) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    item: TodoItem,
    onToggle: (TodoItem) -> Unit,
    onDelete: ((TodoItem) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(item) }
            .padding(vertical = 4.dp)
            .alpha(if (item.done) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.text,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            )
            val meta = metaLine(item.from, item.ts)
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelSmall, color = EdcMuted)
            }
        }
        if (item.done && onDelete != null) {
            TextButton(onClick = { onDelete(item) }) { Text("Remove") }
        }
    }
}

@Composable
private fun SendField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    actionLabel: String,
    onAction: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Outlined.Send,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                onAction()
                keyboard?.hide()
            }),
        )
        IconButton(
            onClick = {
                onAction()
                keyboard?.hide()
            },
            enabled = value.isNotBlank(),
        ) {
            Icon(icon, contentDescription = actionLabel)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EdcCyan,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = EdcMuted, style = MaterialTheme.typography.bodyMedium)
}

private fun hostLabel(settings: EdcSettings): String =
    if (settings.preset == HostPreset.CUSTOM) {
        settings.customUrl.ifBlank { "Custom" }
    } else {
        settings.preset.label
    }

private fun metaLine(from: String, ts: String, extra: String = ""): String =
    listOf(from, formatTs(ts), extra).filter { it.isNotBlank() }.joinToString(" · ")

private fun formatTs(ts: String): String {
    if (ts.isBlank()) return ""
    val instant = runCatching { Instant.parse(ts) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(ts).toInstant() }.getOrNull()
        ?: return ts
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}
