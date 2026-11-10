package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.DotMatrixBanner
import com.example.ui.components.NomaticCard
import com.example.ui.components.TelemetryPill
import com.example.ui.theme.NomaticEmerald
import com.example.ui.theme.NomaticRed

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(NomaticRed)
                )
                Text(
                    text = "NOMATIC AI",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "YOUR PERSONAL AI WORKSPACE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(
                color = NomaticRed,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "INITIALIZING ROOM DB & ANDROID KEYSTORE...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BiometricLockScreen(
    onRequestBiometricUnlock: () -> Unit,
    onBypassInEmulator: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NomaticCard(accentBorder = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Lock",
                    tint = NomaticRed,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "WORKSPACE LOCKED",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Biometric & Device Credential Guard Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestBiometricUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unlock_biometric_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("AUTHENTICATE TO UNLOCK")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onBypassInEmulator,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Unlock Session", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    biometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onSaveKeys: (openAiKey: String, geminiKey: String) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var openAiInput by remember { mutableStateOf("") }
    var geminiInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryPill(
                    label = "STEP ${step + 1} OF 3 • INITIALIZATION",
                    color = NomaticRed
                )
                TextButton(
                    onClick = onCompleteOnboarding,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text("SKIP TO OFFLINE WORKSPACE", style = MaterialTheme.typography.labelMedium)
                }
            }

            when (step) {
                0 -> {
                    DotMatrixBanner(
                        title = "NOMATIC AI",
                        subtitle = "Your Personal AI Workspace — Multi-Provider Intelligence, Mini Browser, Knowledge Vault & Developer Suite.",
                        statusLabel = "LOCAL-FIRST ARCHITECTURE"
                    )

                    NomaticCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = NomaticRed)
                            Text("Unified Personal Workspace", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Dual AI Engine: Seamlessly route between OpenAI / OpenRouter and Google Gemini with automatic fallback.\n" +
                                "• Hardened Mini Browser: Extract readable webpage content, summarize pages, and save research to your Vault.\n" +
                                "• Developer & Document Suite: Linux, Ab Initio, AWS, LDAP utilities, and PDF/Document Intelligence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                1 -> {
                    DotMatrixBanner(
                        title = "ZERO-TRUST PRIVACY",
                        subtitle = "All conversations, notes, prompts, tasks, and encrypted keys stay on your device.",
                        statusLabel = "AES-256-GCM KEYSTORE"
                    )

                    NomaticCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NomaticEmerald)
                            Text("Android Keystore Encryption", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your personal API keys are encrypted with AES-256-GCM backed by the hardware Android Keystore and stored only in private internal storage. Keys are never logged, never stored in plaintext in Room or DataStore, and never included in unencrypted backups.\n\nNote: Local encryption cannot fully protect keys on rooted or compromised devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Workspace Lock", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Require biometric / device authentication on launch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = onToggleBiometric
                            )
                        }
                    }
                }

                else -> {
                    DotMatrixBanner(
                        title = "PROVIDER KEYS",
                        subtitle = "Configure OpenAI or Google Gemini keys now, or skip to use all offline modules first.",
                        statusLabel = "OPTIONAL SETUP"
                    )

                    NomaticCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = NomaticRed)
                            Text("Personal API Keys (Encrypted Locally)", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = openAiInput,
                            onValueChange = { openAiInput = it },
                            label = { Text("OpenAI API Key (sk-...)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_openai_key_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = geminiInput,
                            onValueChange = { geminiInput = it },
                            label = { Text("Google Gemini API Key (AIza...)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_gemini_key_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can replace, test, or delete keys at any time in AI Provider Management.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("BACK")
                }
            }
            Button(
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        if (openAiInput.isNotBlank() || geminiInput.isNotBlank()) {
                            onSaveKeys(openAiInput, geminiInput)
                        }
                        onCompleteOnboarding()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NomaticRed),
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_next_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (step < 2) "CONTINUE" else "ENTER WORKSPACE")
            }
        }
    }
}
