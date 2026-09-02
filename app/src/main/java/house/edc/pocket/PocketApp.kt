package house.edc.pocket

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
private const val pollMsActive = 5_000L
private const val pollMsIdle = 15_000L
private const val clipPreviewChars = 220

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketApp(
    settings: EdcSettings,
    store: SettingsStore,
    client: EdcClient,
    outboxStore: OutboxStore,
    outboxProcessor: OutboxProcessor,
    hostConnector: HostConnector,
    networkMonitor: NetworkMonitor,
    launchAction: LaunchAction = LaunchAction.NONE,
    onLaunchActionHandled: () -> Unit = {},
) {
    EdcPocketTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val haptic = rememberEdcHaptic()
        val notifPermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { _ -> }
        val networkKind by networkMonitor.networkKind.collectAsState(initial = NetworkKind.OTHER)
        val outboxItems by outboxStore.items.collectAsState(initial = emptyList())
        var tab by rememberSaveable { mutableStateOf(PocketTab.CLIP.name) }
        val currentTab = PocketTab.entries.find { it.name == tab } ?: PocketTab.CLIP
        var snapshot by remember { mutableStateOf(HostSnapshot()) }
        var loading by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf<String?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var stale by remember { mutableStateOf(false) }
        var hostHealth by remember { mutableStateOf<HostHealth?>(null) }
        var pullRefreshing by remember { mutableStateOf(false) }
        var urlValidationError by remember { mutableStateOf<String?>(null) }
        var uploadProgress by remember { mutableStateOf<UploadProgress?>(null) }
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
                runCatching {
                    val snap = client.load(base, settings.identity)
                    val health = runCatching { client.probeHealth(base, settings.identity) }.getOrNull()
                    snap to health
                }
            }
            if (!silent) loading = false
            result.fold(
                onSuccess = { (snap, health) ->
                    snapshot = snap
                    if (health != null) hostHealth = health
                    error = null
                    stale = false
                    status = connectionLabel(settings, stale = false, error = null)
                    val entry = snap.latest ?: snap.history.firstOrNull()
                    if (entry != null) {
                        LatestClipStore(context).save(entry)
                        scope.launch { EdcWidgetUpdater.updateAll(context) }
                    }
                },
                onFailure = {
                    error = hostFailureMessage(settings, it.message)
                    status = null
                    stale = snapshot.latest != null ||
                        snapshot.history.isNotEmpty() ||
                        snapshot.todos.isNotEmpty() ||
                        snapshot.drops.isNotEmpty()
                },
            )
        }

        suspend fun flushOutbox(notify: Boolean = true): Int {
            val sent = withContext(Dispatchers.IO) { outboxProcessor.flush(settings) }
            if (sent > 0) {
                refresh(silent = true)
                if (notify) {
                    val msg = if (sent == 1) "Sent queued item" else "Sent $sent queued items"
                    snackbar.showSnackbar(msg)
                }
            }
            return sent
        }

        suspend fun sendWithOutbox(
            okMessage: String?,
            enqueueItem: suspend () -> OutboxItem,
            send: suspend () -> Unit,
        ) {
            val base = settings.baseUrl
            if (base.isBlank()) {
                error = "Set a host URL in Settings."
                return
            }
            loading = true
            val result = withContext(Dispatchers.IO) { runCatching { send() } }
            loading = false
            result.fold(
                onSuccess = {
                    error = null
                    haptic()
                    if (okMessage != null) snackbar.showSnackbar(okMessage)
                    refresh()
                },
                onFailure = {
                    withContext(Dispatchers.IO) { outboxStore.enqueue(enqueueItem()) }
                    error = null
                    snackbar.showSnackbar("Queued — will send when host is back")
                },
            )
        }

        suspend fun uploadPhotos(uris: List<Uri>, session: String) {
            if (uris.isEmpty()) return
            uploadProgress = UploadProgress(0, uris.size)
            var sent = 0
            uris.forEachIndexed { index, uri ->
                val name = uri.lastPathSegment ?: "photo_${index + 1}.jpg"
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        client.uploadImage(
                            settings.baseUrl,
                            settings.identity,
                            uri,
                            name,
                            session,
                        )
                    }.isSuccess
                }
                if (!ok) {
                    withContext(Dispatchers.IO) {
                        outboxStore.enqueuePhoto(context.contentResolver, uri, name, session)
                    }
                } else {
                    sent++
                }
                uploadProgress = UploadProgress(index + 1, uris.size)
            }
            uploadProgress = null
            refresh()
            snackbar.showSnackbar(
                when {
                    sent == uris.size -> {
                        if (sent == 1) "Photo sent to Incoming" else "$sent photos sent"
                    }
                    sent == 0 -> "Queued ${uris.size} photo(s)"
                    else -> "Sent $sent, queued ${uris.size - sent}"
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
                    error = hostFailureMessage(settings, it.message)
                },
            )
        }

        suspend fun pullRefresh() {
            pullRefreshing = true
            refresh(silent = true)
            flushOutbox(notify = false)
            pullRefreshing = false
        }

        LaunchedEffect(settings.backgroundPoll) {
            PollScheduler.apply(context, settings.backgroundPoll)
        }

        LaunchedEffect(settings.baseUrl, settings.identity, resumeTick) {
            refresh()
            flushOutbox()
        }

        LaunchedEffect(networkKind, settings.autoHost, settings.preset, settings.identity) {
            if (networkKind == NetworkKind.NONE) return@LaunchedEffect
            val found = withContext(Dispatchers.IO) {
                hostConnector.syncHost(settings, store, networkKind)
            }
            if (found != null) {
                hostHealth = found.health
                error = null
                refresh(silent = true)
            } else if (settings.baseUrl.isNotBlank()) {
                val health = withContext(Dispatchers.IO) { hostConnector.reprobe(settings) }
                if (health != null) hostHealth = health
            }
            flushOutbox()
        }

        LaunchedEffect(currentTab, settings.baseUrl, settings.identity) {
            while (isActive) {
                val delayMs = when (currentTab) {
                    PocketTab.CLIP, PocketTab.LIST -> pollMsActive
                    else -> pollMsIdle
                }
                delay(delayMs)
                refresh(silent = true)
            }
        }

        LaunchedEffect(currentTab, settings.baseUrl, settings.identity) {
            if (currentTab != PocketTab.SETTINGS || settings.baseUrl.isBlank()) return@LaunchedEffect
            val health = withContext(Dispatchers.IO) {
                runCatching { client.probeHealth(settings.baseUrl, settings.identity) }.getOrNull()
            }
            if (health != null) hostHealth = health
        }

        LaunchedEffect(launchAction, settings.baseUrl, settings.identity) {
            if (launchAction == LaunchAction.NONE) return@LaunchedEffect
            when (launchAction) {
                LaunchAction.OPEN_SEND -> tab = PocketTab.SEND.name
                LaunchAction.OPEN_LIST -> tab = PocketTab.LIST.name
                LaunchAction.COPY_LATEST -> {
                    val base = settings.baseUrl
                    if (base.isBlank()) {
                        error = "Set a host URL in Settings."
                    } else {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { client.load(base, settings.identity) }
                        }
                        result.fold(
                            onSuccess = { snap ->
                                val text = snap.latest?.text ?: snap.history.firstOrNull()?.text
                                if (text.isNullOrBlank()) {
                                    snackbar.showSnackbar("Nothing on the house clipboard")
                                } else {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("EDC", text))
                                    haptic()
                                    snackbar.showSnackbar("Copied latest clip")
                                }
                            },
                            onFailure = {
                                error = it.message ?: "Host unreachable"
                            },
                        )
                    }
                }
                LaunchAction.NONE -> Unit
            }
            onLaunchActionHandled()
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
                                text = settings.identity + " · " + connectionLabel(settings, stale, error),
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
                    error != null -> error
                    outboxItems.isNotEmpty() -> "${outboxItems.size} send(s) queued"
                    else -> status
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
                val caps = hostHealth?.capabilities ?: HostCapabilities.ALL
                when (currentTab) {
                    PocketTab.CLIP -> ClipPane(
                        latest = snapshot.latest,
                        history = snapshot.history,
                        filter = settings.clipFilter,
                        clipboardEnabled = caps.clipboard,
                        onFilterChange = { scope.launch { store.setClipFilter(it) } },
                        isRefreshing = pullRefreshing,
                        onRefresh = { scope.launch { pullRefresh() } },
                        onCopy = { text ->
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("EDC", text))
                            haptic()
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
                        onOpenDashboard = { entry ->
                            HostLinks.clipDashboardUrl(hostHealth, entry)?.let { openUrl(context, it) }
                                ?: scope.launch { snackbar.showSnackbar("No dashboard link for this clip") }
                        },
                        showDashboardLinks = caps.dashboard,
                        onSend = { text ->
                            scope.launch {
                                sendWithOutbox(
                                    okMessage = "Sent to clipboard",
                                    enqueueItem = {
                                        OutboxItem(kind = OutboxKind.CLIP, text = text)
                                    },
                                    send = {
                                        client.sendText(settings.baseUrl, settings.identity, text)
                                    },
                                )
                            }
                        },
                    )
                    PocketTab.LIST -> ListPane(
                        todos = snapshot.todos,
                        listEnabled = caps.todo,
                        shareListEnabled = caps.todoText,
                        deleteEnabled = caps.todoDelete,
                        showDashboardLinks = caps.dashboard,
                        isRefreshing = pullRefreshing,
                        onRefresh = { scope.launch { pullRefresh() } },
                        onAdd = { text ->
                            scope.launch {
                                sendWithOutbox(
                                    okMessage = "Added to list",
                                    enqueueItem = {
                                        OutboxItem(kind = OutboxKind.LIST, text = text)
                                    },
                                    send = {
                                        client.addTodo(settings.baseUrl, settings.identity, text)
                                    },
                                )
                            }
                        },
                        onToggle = { item ->
                            val next = !item.done
                            haptic()
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
                            scope.launch {
                                haptic()
                                val previous = snapshot.todos
                                snapshot = snapshot.copy(
                                    todos = snapshot.todos.filter { it.id != item.id },
                                )
                                val undo = snackbar.showSnackbar(
                                    message = "Removed",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short,
                                )
                                if (undo == SnackbarResult.ActionPerformed) {
                                    snapshot = snapshot.copy(todos = previous)
                                    return@launch
                                }
                                hostCall(null) {
                                    client.deleteTodo(
                                        settings.baseUrl,
                                        settings.identity,
                                        item.id,
                                    )
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
                        onOpenDashboard = { item ->
                            HostLinks.todoDashboardUrl(hostHealth, item)?.let { openUrl(context, it) }
                                ?: scope.launch { snackbar.showSnackbar("No dashboard link for this item") }
                        },
                    )
                    PocketTab.SEND -> SendPane(
                        drops = snapshot.drops,
                        baseUrl = settings.baseUrl,
                        uploadProgress = uploadProgress,
                        clipEnabled = caps.clipboard,
                        listEnabled = caps.todo,
                        uploadEnabled = caps.upload,
                        sessionUploadEnabled = caps.sessionUpload,
                        incomingEnabled = caps.incoming,
                        incomingUrl = { drop -> client.incomingOpenUrl(settings.baseUrl, drop) },
                        onSendClip = { text ->
                            scope.launch {
                                sendWithOutbox(
                                    okMessage = "Sent to clipboard",
                                    enqueueItem = {
                                        OutboxItem(kind = OutboxKind.CLIP, text = text)
                                    },
                                    send = {
                                        client.sendText(settings.baseUrl, settings.identity, text)
                                    },
                                )
                            }
                        },
                        onSendList = { text ->
                            scope.launch {
                                sendWithOutbox(
                                    okMessage = "Added to list",
                                    enqueueItem = {
                                        OutboxItem(kind = OutboxKind.LIST, text = text)
                                    },
                                    send = {
                                        client.addTodo(settings.baseUrl, settings.identity, text)
                                    },
                                )
                            }
                        },
                        onUpload = { uri, name, session ->
                            scope.launch { uploadPhotos(listOf(uri), session) }
                        },
                        onUploadMultiple = { uris, session ->
                            scope.launch { uploadPhotos(uris, session) }
                        },
                        onOpenDrop = { drop ->
                            val url = client.incomingOpenUrl(settings.baseUrl, drop)
                            if (url != null) openUrl(context, url) else scope.launch {
                                snackbar.showSnackbar("No download link for this file")
                            }
                        },
                        onShareDrop = { drop ->
                            scope.launch {
                                loading = true
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val (bytes, mime) = client.downloadIncoming(
                                            settings.baseUrl,
                                            settings.identity,
                                            drop,
                                        )
                                        IncomingFiles.shareBytes(context, drop.name, bytes, mime)
                                    }
                                }
                                loading = false
                                result.onFailure {
                                    snackbar.showSnackbar(it.message ?: "Share failed")
                                }
                            }
                        },
                        onSaveDrop = { drop ->
                            scope.launch {
                                loading = true
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val (bytes, mime) = client.downloadIncoming(
                                            settings.baseUrl,
                                            settings.identity,
                                            drop,
                                        )
                                        IncomingFiles.saveToDevice(context, drop.name, bytes, mime)
                                    }
                                }
                                loading = false
                                result.fold(
                                    onSuccess = { snackbar.showSnackbar("Saved to device") },
                                    onFailure = {
                                        snackbar.showSnackbar(it.message ?: "Save failed")
                                    },
                                )
                            }
                        },
                    )
                    PocketTab.SETTINGS -> SettingsPane(
                        settings = settings,
                        onIdentity = { scope.launch { store.setIdentity(it) } },
                        onPreset = { scope.launch { store.setPreset(it) } },
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
                                        hostHealth = it
                                        status = it.summary()
                                        snackbar.showSnackbar(it.summary())
                                    },
                                    onFailure = {
                                        error = hostFailureMessage(settings, it.message)
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
                                hostHealth = found.health
                                status = found.health.summary() + " · ${found.baseUrl}"
                                snackbar.showSnackbar("Using ${found.preset?.label ?: "host"}")
                                refresh()
                            }
                        },
                        hostHealth = hostHealth,
                        outboxItems = outboxItems,
                        autoHost = settings.autoHost,
                        onAutoHost = { scope.launch { store.setAutoHost(it) } },
                        backgroundPoll = settings.backgroundPoll,
                        onBackgroundPoll = { mode ->
                            scope.launch { store.setBackgroundPoll(mode) }
                            PollScheduler.apply(context, mode)
                            if (mode != BackgroundPollMode.OFF && Build.VERSION.SDK_INT >= 33) {
                                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        urlValidationError = urlValidationError,
                        onCustomUrl = { raw ->
                            val err = validateHostUrl(raw)
                            if (err != null) {
                                urlValidationError = err
                            } else {
                                urlValidationError = null
                                scope.launch { store.setCustomUrl(raw) }
                            }
                        },
                        onFlushOutbox = { scope.launch { flushOutbox() } },
                        onClearOutbox = { scope.launch { outboxStore.clear() } },
                        onOpenDashboard = {
                            val url = hostHealth?.dashboardUrl?.takeIf { it.isNotBlank() }
                            if (url != null) openUrl(context, url) else scope.launch {
                                snackbar.showSnackbar("No dashboard URL from host")
                            }
                        },
                        useHttps = settings.useHttps,
                        onUseHttps = { scope.launch { store.setUseHttps(it) } },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ClipPane(
    latest: ClipEntry?,
    history: List<ClipEntry>,
    filter: String,
    clipboardEnabled: Boolean,
    onFilterChange: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenDashboard: (ClipEntry) -> Unit,
    showDashboardLinks: Boolean,
    onSend: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    fun matchesSearch(entry: ClipEntry): Boolean =
        search.isBlank() || entry.text.contains(search, ignoreCase = true)
    val filteredHistory = history.filter { entry ->
        (filter == "All" || entry.from.equals(filter, ignoreCase = true)) && matchesSearch(entry)
    }.let { list ->
        if (latest == null) list
        else list.filter { it.id != latest.id || it.text != latest.text }
    }
    val showLatest = latest != null &&
        (filter == "All" || latest.from.equals(filter, ignoreCase = true)) &&
        matchesSearch(latest)
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
            placeholder = { Text("Search clips") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            clipFilters.forEach { name ->
                FilterChip(
                    selected = filter == name,
                    onClick = { onFilterChange(name) },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0E3A43),
                        selectedLabelColor = EdcCyan,
                    ),
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            item {
                SectionLabel("Latest")
                Spacer(Modifier.height(8.dp))
                if (!showLatest) {
                    EmptyHint(
                        if (search.isNotBlank()) "No clips match \"$search\"."
                        else "Nothing on the house clipboard.",
                    )
                } else {
                    ClipCard(
                        entry = latest!!,
                        featured = true,
                        onCopy = onCopy,
                        onShare = onShare,
                        onOpenUrl = onOpenUrl,
                        onOpenDashboard = if (showDashboardLinks) onOpenDashboard else null,
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
                        onOpenDashboard = if (showDashboardLinks) onOpenDashboard else null,
                    )
                }
            }
        }
        }
        if (clipboardEnabled) {
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
        } else {
            EmptyHint(
                "House clipboard is read-only on this host.",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListPane(
    todos: List<TodoItem>,
    listEnabled: Boolean,
    shareListEnabled: Boolean,
    deleteEnabled: Boolean,
    showDashboardLinks: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
    onShareList: () -> Unit,
    onOpenDashboard: (TodoItem) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val open = todos.filter { !it.done }
    val done = todos.filter { it.done }
    Column(modifier = Modifier.fillMaxSize()) {
        if (listEnabled) {
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
        } else {
            EmptyHint(
                "Shared list is unavailable on this host.",
                modifier = Modifier.padding(16.dp),
            )
        }
        if (todos.isNotEmpty() && shareListEnabled) {
            OutlinedButton(
                onClick = onShareList,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy / share list")
            }
        }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            if (todos.isEmpty()) {
                item { EmptyHint("List is empty.") }
            }
            items(open, key = { it.id }) { item ->
                TodoRow(
                    item = item,
                    onToggle = onToggle,
                    onOpenDashboard = if (showDashboardLinks) {
                        { onOpenDashboard(item) }
                    } else {
                        null
                    },
                )
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
        }
    }
}

@Composable
private fun SendPane(
    drops: List<DropItem>,
    baseUrl: String,
    uploadProgress: UploadProgress?,
    clipEnabled: Boolean,
    listEnabled: Boolean,
    uploadEnabled: Boolean,
    sessionUploadEnabled: Boolean,
    incomingEnabled: Boolean,
    incomingUrl: (DropItem) -> String?,
    onSendClip: (String) -> Unit,
    onSendList: (String) -> Unit,
    onUpload: (Uri, String, String) -> Unit,
    onUploadMultiple: (List<Uri>, String) -> Unit,
    onOpenDrop: (DropItem) -> Unit,
    onShareDrop: (DropItem) -> Unit,
    onSaveDrop: (DropItem) -> Unit,
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
    val pickMultiple = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris ->
        if (uris.isNotEmpty()) onUploadMultiple(uris, session)
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
            if (clipEnabled) {
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
            }
            if (listEnabled) {
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
        }
        if (uploadEnabled) {
            SectionLabel("Photo to Incoming")
            if (sessionUploadEnabled) {
                OutlinedTextField(
                    value = session,
                    onValueChange = { session = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Session folder (optional)") },
                    placeholder = { Text("Drop/Sessions/2026-09-02 – Event") },
                )
            }
        uploadProgress?.let { progress ->
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                    color = EdcCyan,
                )
                Text(
                    text = "Uploading ${progress.done} / ${progress.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EdcMuted,
                )
            }
        }
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
                enabled = uploadProgress == null,
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
                enabled = uploadProgress == null,
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("One")
            }
            OutlinedButton(
                onClick = {
                    pickMultiple.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = uploadProgress == null,
            ) {
                Text("Many")
            }
        }
        }
        if (incomingEnabled) {
            SectionLabel("Incoming")
            if (drops.isEmpty()) {
                EmptyHint("No incoming files yet.")
            } else {
                drops.forEach { drop ->
                    DropCard(
                        drop = drop,
                        imageUrl = incomingUrl(drop)?.takeIf { drop.isImage() },
                        onOpen = { onOpenDrop(drop) },
                        onShare = { onShareDrop(drop) },
                        onSave = { onSaveDrop(drop) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DropCard(
    drop: DropItem,
    imageUrl: String?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = EdcSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = drop.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(drop.name, fontWeight = FontWeight.Medium)
                    Text(
                        text = metaLine(drop.from, drop.ts, formatSize(drop.size)),
                        style = MaterialTheme.typography.labelSmall,
                        color = EdcMuted,
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onSave) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsPane(
    settings: EdcSettings,
    hostHealth: HostHealth?,
    outboxItems: List<OutboxItem>,
    autoHost: Boolean,
    onIdentity: (String) -> Unit,
    onPreset: (HostPreset) -> Unit,
    urlValidationError: String?,
    onCustomUrl: (String) -> Unit,
    onProbe: () -> Unit,
    onFindHost: () -> Unit,
    onAutoHost: (Boolean) -> Unit,
    backgroundPoll: BackgroundPollMode,
    onBackgroundPoll: (BackgroundPollMode) -> Unit,
    onFlushOutbox: () -> Unit,
    onClearOutbox: () -> Unit,
    onOpenDashboard: () -> Unit,
    useHttps: Boolean,
    onUseHttps: (Boolean) -> Unit,
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
            effectiveIdentities(hostHealth).forEach { name ->
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
                isError = urlValidationError != null,
                supportingText = urlValidationError?.let { err -> { Text(err) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCustomUrl(customDraft) }),
            )
            Button(onClick = { onCustomUrl(customDraft.trim()) }) { Text("Save URL") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Home / Away", fontWeight = FontWeight.Medium)
                Text(
                    text = "Switch host preset when Wi‑Fi or Tailscale changes",
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
            }
            Switch(checked = autoHost, onCheckedChange = onAutoHost)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use HTTPS", fontWeight = FontWeight.Medium)
                Text(
                    text = "Talk to host over https:// instead of http://",
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
            }
            Switch(checked = useHttps, onCheckedChange = onUseHttps)
        }
        SectionLabel("Glanceable")
        Text(
            text = "Add the home screen widget, Quick Settings tile “EDC clip”, and optional background alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = EdcMuted,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackgroundPollMode.entries.forEach { mode ->
                FilterChip(
                    selected = backgroundPoll == mode,
                    onClick = { onBackgroundPoll(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0E3A43),
                        selectedLabelColor = EdcCyan,
                    ),
                )
            }
        }
        Text(
            text = when (backgroundPoll) {
                BackgroundPollMode.OFF -> "No background checks."
                BackgroundPollMode.CONSERVATIVE -> "Checks about once an hour when idle."
                BackgroundPollMode.ACTIVE -> "Checks every 15 minutes (Android minimum)."
            },
            style = MaterialTheme.typography.bodySmall,
            color = EdcMuted,
        )
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
        if (hostHealth != null) {
            SectionLabel("Host info")
            Text(
                text = hostHealth.summary(),
                style = MaterialTheme.typography.bodyMedium,
            )
            hostHealth.capabilities.summaryLines().forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
            }
            if (hostHealth.knownUsers.isNotEmpty()) {
                Text(
                    text = "Identities from host: ${hostHealth.knownUsers.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
            }
            if (hostHealth.dashboardUrl.isNotBlank()) {
                Text(
                    text = hostHealth.dashboardUrl,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
            }
            OutlinedButton(onClick = onOpenDashboard, modifier = Modifier.fillMaxWidth()) {
                Text("Open house dashboard")
            }
        }
        if (outboxItems.isNotEmpty()) {
            SectionLabel("Pending sends (${outboxItems.size})")
            outboxItems.forEach { item ->
                Text(
                    text = item.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
            }
            Button(onClick = onFlushOutbox, modifier = Modifier.fillMaxWidth()) {
                Text("Retry queued sends")
            }
            OutlinedButton(onClick = onClearOutbox, modifier = Modifier.fillMaxWidth()) {
                Text("Clear queue")
            }
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
    onOpenDashboard: ((ClipEntry) -> Unit)? = null,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy(entry.text) },
            )
            if (long) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "Show more")
                }
            }
            if (directUrl != null && !entry.text.trim().equals(directUrl, ignoreCase = true)) {
                TextButton(onClick = { onOpenUrl(directUrl) }) { Text("Open link") }
            }
            if (onOpenDashboard != null) {
                TextButton(onClick = { onOpenDashboard(entry) }) { Text("On dashboard") }
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
    onOpenDashboard: (() -> Unit)? = null,
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
        if (onOpenDashboard != null) {
            TextButton(onClick = onOpenDashboard) { Text("Dashboard") }
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
private fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(text, color = EdcMuted, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

private fun effectiveIdentities(health: HostHealth?): List<String> =
    health?.knownUsers?.takeIf { it.isNotEmpty() } ?: identities

private fun connectionLabel(settings: EdcSettings, stale: Boolean, error: String?): String {
    if (stale && error != null) return "Offline · cached"
    if (error != null) return "Unreachable"
    return when (settings.preset) {
        HostPreset.LAN -> "Home LAN"
        HostPreset.TAILSCALE -> "Away"
        HostPreset.CUSTOM -> settings.customUrl.ifBlank { "Custom" }
    }
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
