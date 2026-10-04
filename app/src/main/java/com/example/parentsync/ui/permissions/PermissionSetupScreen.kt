package com.example.parentsync.ui.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parentsync.data.repository.AppUsageRepositoryImpl
import com.example.parentsync.service.ParentSyncAdminReceiver

@Composable
fun PermissionSetupScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, ParentSyncAdminReceiver::class.java) }
    val appUsageRepository = remember { AppUsageRepositoryImpl(context) }

    var isAdminActive by remember { mutableStateOf(devicePolicyManager.isAdminActive(adminComponent)) }
    var isUsageAccessGranted by remember { mutableStateOf(appUsageRepository.hasUsageStatsPermission()) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Refresh state when returning from settings
    LaunchedEffect(Unit) {
        while (true) {
            isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
            isUsageAccessGranted = appUsageRepository.hasUsageStatsPermission()
            isOverlayGranted = Settings.canDrawOverlays(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    val allGranted = isAdminActive && isUsageAccessGranted && isOverlayGranted

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ParentSync Permission Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To protect this device and monitor screen time effectively, ParentSync requires the following system privileges.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Device Admin Permission Card
            PermissionCard(
                title = "Device Administrator",
                description = "Enables remote locking, security policies, and prevents unauthorized app uninstallation.",
                icon = Icons.Rounded.AdminPanelSettings,
                isGranted = isAdminActive,
                onRequestClick = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "ParentSync needs Device Administrator access to secure the child's device and support remote locking."
                        )
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Usage Access Permission Card
            PermissionCard(
                title = "Usage Access",
                description = "Required to monitor app usage statistics and enforce daily time limits.",
                icon = Icons.Rounded.Timeline,
                isGranted = isUsageAccessGranted,
                onRequestClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. System Overlay Permission Card
            PermissionCard(
                title = "Display Over Other Apps",
                description = "Required to display bedtime lock screens and block restricted apps instantly.",
                icon = Icons.Rounded.Layers,
                isGranted = isOverlayGranted,
                onRequestClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onAllPermissionsGranted,
                enabled = allGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (allGranted) "Continue to ParentSync" else "Grant All Permissions to Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isGranted) {
                OutlinedButton(
                    onClick = onRequestClick
                ) {
                    Text("Grant")
                }
            } else {
                Text(
                    text = "Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
