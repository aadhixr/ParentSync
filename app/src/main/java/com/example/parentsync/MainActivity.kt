package com.example.parentsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.example.parentsync.data.local.DeviceStateManager
import com.example.parentsync.ui.dashboard.RemoteDashboardScreen
import com.example.parentsync.ui.limits.AppUsageLimitsScreen
import com.example.parentsync.ui.navigation.Screen
import com.example.parentsync.ui.theme.ParentSyncTheme
import com.example.parentsync.ui.tracking.AppActivityTrackingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParentSyncTheme {
                val context = applicationContext
                val deviceStateManager = remember { DeviceStateManager.getInstance(context) }
                val isDeviceLocked by deviceStateManager.isDeviceLocked.collectAsState()

                val backStack = remember { mutableStateListOf<NavKey>(Screen.Dashboard) }
                val currentScreen = backStack.lastOrNull() ?: Screen.Dashboard

                val configuration = LocalConfiguration.current
                val isExpandedOrMedium = configuration.screenWidthDp >= 600

                val navItems = listOf(
                    Triple(Screen.Dashboard, "Dashboard", Icons.Rounded.Dashboard),
                    Triple(Screen.Tracking, "Activity", Icons.Rounded.Analytics),
                    Triple(Screen.Limits, "Limits", Icons.Rounded.Block)
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedOrMedium) {
                        // Tablet / Foldable layout with Navigation Rail
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail {
                                Spacer(modifier = Modifier.height(24.dp))
                                navItems.forEach { (screen, label, icon) ->
                                    NavigationRailItem(
                                        selected = currentScreen == screen,
                                        onClick = {
                                            if (currentScreen != screen) {
                                                backStack.clear()
                                                backStack.add(screen)
                                            }
                                        },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                NavDisplay(
                                    backStack = backStack,
                                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                                    entryProvider = { key ->
                                        getNavEntry(key, backStack)
                                    }
                                )
                            }
                        }
                    } else {
                        // Phone layout with Bottom Navigation
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    navItems.forEach { (screen, label, icon) ->
                                        NavigationBarItem(
                                            selected = currentScreen == screen,
                                            onClick = {
                                                if (currentScreen != screen) {
                                                    backStack.clear()
                                                    backStack.add(screen)
                                                }
                                            },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                NavDisplay(
                                    backStack = backStack,
                                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                                    entryProvider = { key ->
                                        getNavEntry(key, backStack)
                                    }
                                )
                            }
                        }
                    }

                    // Device Locked Overlay triggered by remote command
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
                                    text = "Your parent has temporarily locked this device for bedtime or focus time. Please contact your parent to unlock.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(36.dp))
                                Button(
                                    onClick = {
                                        // For testing/debugging ease or override if needed
                                        deviceStateManager.setDeviceLocked(false)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text("Request Unlock / Dismiss")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getNavEntry(key: NavKey, backStack: MutableList<NavKey>): NavEntry<NavKey> {
    return when (key) {
        is Screen.Dashboard -> NavEntry(key) {
            RemoteDashboardScreen(
                onNavigateToTracking = {
                    backStack.add(Screen.Tracking)
                },
                onNavigateToLimits = {
                    backStack.add(Screen.Limits)
                }
            )
        }
        is Screen.Tracking -> NavEntry(key) {
            AppActivityTrackingScreen(
                onNavigateToLimits = {
                    backStack.add(Screen.Limits)
                }
            )
        }
        is Screen.Limits -> NavEntry(key) {
            AppUsageLimitsScreen(
                onBack = {
                    backStack.removeLastOrNull()
                }
            )
        }
        else -> NavEntry(key) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unknown screen")
            }
        }
    }
}
