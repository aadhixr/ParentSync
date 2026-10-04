package com.example.parentsync

import android.graphics.Bitmap
import android.graphics.Canvas
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parentsync.data.api.SyncApiClient
import com.example.parentsync.data.local.DeviceStateManager
import com.example.parentsync.data.repository.AppUsageRepositoryImpl
import com.example.parentsync.data.repository.SyncRepositoryImpl
import com.example.parentsync.service.DeviceSyncForegroundService
import com.example.parentsync.service.ParentSyncAdminReceiver
import com.example.parentsync.service.ScreenMirroringService
import com.example.parentsync.ui.permissions.PermissionSetupScreen
import com.example.parentsync.ui.theme.ParentSyncTheme
import com.example.parentsync.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start foreground service for background syncing & remote commands monitoring
        DeviceSyncForegroundService.startService(
            this,
            childId = "child_alex_01",
            apiKey = SyncApiClient.SUPABASE_PUBLISHABLE_KEY,
            authToken = SyncApiClient.SUPABASE_SECRET_KEY
        )

        setContent {
            ParentSyncTheme {
                val context = LocalContext.current
                val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
                val adminComponent = remember { ComponentName(context, ParentSyncAdminReceiver::class.java) }
                val appUsageRepository = remember { AppUsageRepositoryImpl(context) }

                var isAdminActive by remember { mutableStateOf(devicePolicyManager.isAdminActive(adminComponent)) }
                var isUsageAccessGranted by remember { mutableStateOf(appUsageRepository.hasUsageStatsPermission()) }
                var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

                LaunchedEffect(Unit) {
                    while (true) {
                        isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
                        isUsageAccessGranted = appUsageRepository.hasUsageStatsPermission()
                        isOverlayGranted = Settings.canDrawOverlays(context)
                        try {
                            val decorView = window.decorView
                            if (decorView.width > 0 && decorView.height > 0) {
                                val bitmap = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                decorView.draw(canvas)
                                ScreenMirroringService.latestDecorViewBitmap = bitmap
                            }
                        } catch (_: Exception) {
                        }
                        kotlinx.coroutines.delay(1000)
                    }
                }

                val allPermissionsGranted = isAdminActive && isUsageAccessGranted && isOverlayGranted

                val deviceStateManager = remember { DeviceStateManager.getInstance(context) }
                val isDeviceLocked by deviceStateManager.isDeviceLocked.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!allPermissionsGranted) {
                        PermissionSetupScreen(
                            onAllPermissionsGranted = {
                                isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
                                isUsageAccessGranted = appUsageRepository.hasUsageStatsPermission()
                                isOverlayGranted = Settings.canDrawOverlays(context)
                            }
                        )
                    } else {
                        NodeStatusScreen(
                            childId = "child_alex_01",
                            supabaseUrl = SyncApiClient.SUPABASE_URL,
                            dashboardUrl = "https://aadhixr.github.io/ParentSync/"
                        )
                    }

                    // Device Locked Overlay triggered by remote command or Device Admin lock
                    if (isDeviceLocked) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Device Locked by Parent",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Your parent has temporarily locked this device for bedtime or focus time. Please contact your parent to unlock from the web dashboard.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(36.dp))
                                Button(
                                    onClick = {
                                        deviceStateManager.setDeviceLocked(false)
                                        try {
                                            devicePolicyManager.lockNow()
                                        } catch (e: Exception) {
                                            // Ignore if admin not active or error
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text("Dismiss / Request Unlock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeStatusScreen(
    childId: String,
    supabaseUrl: String,
    dashboardUrl: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ParentSync Node") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Main Status Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ParentSync Node Active",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} (Release)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ParentSync Node Active - Telemetry syncing to web dashboard. Manage locks and limits from your website.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Node Configuration & Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Telemetry & Node Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    DetailRow(
                        icon = Icons.Rounded.Smartphone,
                        title = "Node Identifier (Child ID)",
                        value = childId
                    )

                    DetailRow(
                        icon = Icons.Rounded.CloudSync,
                        title = "Supabase Telemetry Backend",
                        value = supabaseUrl
                    )

                    DetailRow(
                        icon = Icons.Rounded.Security,
                        title = "Background Worker Status",
                        value = "Running continuously (Sync: 15m, Poll: 30s)"
                    )
                }
            }

            // Web Dashboard Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Web Dashboard Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = "Admin controls, usage analytics, screen time limits, and remote locking are fully managed from the web dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dashboardUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.OpenInBrowser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Web Dashboard")
                    }
                }
            }

            // Manual Sync Action Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Diagnostic Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                syncMessage = null
                                try {
                                    val appUsageRepo = AppUsageRepositoryImpl(context)
                                    val repo = SyncRepositoryImpl(context, appUsageRepo)
                                    val result = repo.syncNow(
                                        childId = childId,
                                        apiKey = SyncApiClient.SUPABASE_PUBLISHABLE_KEY,
                                        authToken = SyncApiClient.SUPABASE_SECRET_KEY
                                    )
                                    if (result.isSuccess) {
                                        syncMessage = "Telemetry synced successfully!"
                                    } else {
                                        syncMessage = "Sync failed: ${result.exceptionOrNull()?.localizedMessage}"
                                    }
                                } catch (e: Exception) {
                                    syncMessage = "Error: ${e.localizedMessage}"
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Telemetry...")
                        } else {
                            Text("Force Sync Telemetry Now")
                        }
                    }

                    syncMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.contains("successfully")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
