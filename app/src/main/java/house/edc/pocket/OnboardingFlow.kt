package house.edc.pocket

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val onboardingIdentities = listOf("Mike", "Mhairi")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlow(
    store: SettingsStore,
    client: EdcClient,
    onComplete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = rememberEdcHaptic()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var identity by rememberSaveable { mutableStateOf("Mike") }
    var preset by rememberSaveable { mutableStateOf(HostPreset.LAN.name) }
    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }
    val hostPreset = HostPreset.entries.find { it.name == preset } ?: HostPreset.LAN
    val totalSteps = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Welcome to EDC pocket",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth(),
            color = EdcAccent,
        )
        Text(
            text = "Step ${step + 1} of $totalSteps",
            style = MaterialTheme.typography.labelSmall,
            color = EdcMuted,
        )

        when (step) {
            0 -> {
                Text(
                    text = "Your phone talks to the house Everyday Clipboard — read clips, manage the shared list, and send photos home.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Who is this phone?",
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onboardingIdentities.forEach { name ->
                        FilterChip(
                            selected = identity == name,
                            onClick = { identity = name },
                            label = { Text(name) },
                            colors = chipColors(),
                        )
                    }
                }
            }
            1 -> {
                Text(
                    text = "Where is the house host?",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Home Wi‑Fi is the LAN address. Away uses Tailscale — install and connect Tailscale before using Away.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EdcMuted,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HostPreset.entries.filter { it != HostPreset.CUSTOM }.forEach { item ->
                        FilterChip(
                            selected = hostPreset == item,
                            onClick = { preset = item.name },
                            label = { Text(item.label) },
                            colors = chipColors(),
                        )
                    }
                }
            }
            2 -> {
                Text(
                    text = "Test the connection",
                    fontWeight = FontWeight.SemiBold,
                )
                if (testing) {
                    CircularProgressIndicator(color = EdcAccent)
                } else if (testOk) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = EdcAccent)
                    Text("Host reached — you're connected.", color = EdcAccent)
                } else {
                    OutlinedButton(
                        onClick = {
                            testing = true
                            testError = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        client.probeHealth(hostPreset.url, identity)
                                    }
                                }
                                testing = false
                                result.fold(
                                    onSuccess = {
                                        testOk = it.ok
                                        if (!it.ok) testError = "Host answered but reported not OK."
                                        else haptic()
                                    },
                                    onFailure = {
                                        testOk = false
                                        testError = hostFailureMessage(
                                            EdcSettings(preset = hostPreset, identity = identity),
                                            it.message,
                                        )
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Test ${hostPreset.label}")
                    }
                }
                testError?.let { err ->
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                }
            }
            3 -> {
                Text(
                    text = "Optional — faster access",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "• Add the home screen widget for the latest clip\n" +
                        "• Quick Settings tile “EDC clip” copies the house clipboard\n" +
                        "• Enable background alerts in Settings → Glanceable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EdcMuted,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        RowButtons(
            step = step,
            totalSteps = totalSteps,
            canAdvance = when (step) {
                2 -> testOk
                else -> true
            },
            onBack = { if (step > 0) step -= 1 },
            onNext = {
                when (step) {
                    totalSteps - 1 -> {
                        scope.launch {
                            store.setIdentity(identity)
                            store.setPreset(hostPreset)
                            store.completeOnboarding()
                            haptic()
                            onComplete()
                        }
                    }
                    2 -> if (testOk) step += 1
                    else -> step += 1
                }
            },
            onSkipTest = { step = 3 },
        )
    }
}

@Composable
private fun RowButtons(
    step: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkipTest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onNext,
            enabled = canAdvance,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (step == totalSteps - 1) "Start using EDC pocket" else "Continue")
        }
        if (step > 0) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
        if (step == 2 && !canAdvance) {
            OutlinedButton(onClick = onSkipTest, modifier = Modifier.fillMaxWidth()) {
                Text("Skip test for now")
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color(0xFF0E3A43),
    selectedLabelColor = EdcAccent,
)
