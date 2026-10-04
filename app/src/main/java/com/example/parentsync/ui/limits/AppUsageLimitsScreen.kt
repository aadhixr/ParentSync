package com.example.parentsync.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.parentsync.data.model.AppCategory
import com.example.parentsync.data.model.AppUsageLimit
import com.example.parentsync.ui.theme.ParentSyncTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageLimitsScreen(
    viewModel: AppUsageLimitsViewModel? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm = viewModel ?: remember {
        AppUsageLimitsViewModel(context.applicationContext as android.app.Application)
    }

    val appLimits by vm.appUsageLimits.collectAsState()
    var selectedAppForQuota by remember { mutableStateOf<AppUsageLimit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Usage Limits & Blocking",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage daily quotas and instant restriction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                LimitsSummaryCard(appLimits = appLimits)
            }

            // Header for App List & Items
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installed Apps & Quotas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${appLimits.size} apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (appLimits.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Grant Usage Access permission to view and manage app limits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "No apps available for setting quotas or blocking. Please ensure Usage Access permission is granted.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(appLimits, key = { it.packageName }) { appLimit ->
                    AppLimitItemCard(
                        appLimit = appLimit,
                        onQuotaClick = { selectedAppForQuota = appLimit },
                        onBlockToggle = { isBlocked ->
                            vm.toggleBlock(appLimit.packageName, isBlocked)
                        }
                    )
                }
            }
        }
    }

    // Quota Selection Dialog / Bottom Sheet
    selectedAppForQuota?.let { appLimit ->
        QuotaSelectionDialog(
            appLimit = appLimit,
            onDismiss = { selectedAppForQuota = null },
            onSelectQuota = { minutes ->
                vm.setQuota(appLimit.packageName, minutes)
                selectedAppForQuota = null
            }
        )
    }
}

@Composable
fun LimitsSummaryCard(appLimits: List<AppUsageLimit>) {
    val totalApps = appLimits.size
    val blockedApps = appLimits.count { it.isBlocked }
    val limitedApps = appLimits.count { it.dailyQuotaMinutes > 0 }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Protection Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$blockedApps blocked • $limitedApps limited of $totalApps apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun AppLimitItemCard(
    appLimit: AppUsageLimit,
    onQuotaClick: () -> Unit,
    onBlockToggle: (Boolean) -> Unit
) {
    val usageHours = appLimit.usageTimeMillis / (1000 * 60 * 60)
    val usageMinutes = (appLimit.usageTimeMillis / (1000 * 60)) % 60

    val quotaText = if (appLimit.dailyQuotaMinutes > 0) {
        val qH = appLimit.dailyQuotaMinutes / 60
        val qM = appLimit.dailyQuotaMinutes % 60
        if (qH > 0 && qM > 0) "${qH}h ${qM}m quota" else if (qH > 0) "${qH}h quota" else "${qM}m quota"
    } else {
        "Unlimited"
    }

    val remainingText = if (appLimit.isBlocked) {
        "Blocked Instantly"
    } else if (appLimit.dailyQuotaMinutes > 0) {
        val rem = appLimit.remainingTimeMillis
        if (rem <= 0) {
            "Quota Exceeded"
        } else {
            val rH = rem / (1000 * 60 * 60)
            val rM = (rem / (1000 * 60)) % 60
            if (rH > 0) "${rH}h ${rM}m remaining" else "${rM}m remaining"
        }
    } else {
        "No time limit"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (appLimit.isBlocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = getCategoryColor(appLimit.category).copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(appLimit.category),
                                contentDescription = null,
                                tint = getCategoryColor(appLimit.category),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = appLimit.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Used: ${usageHours}h ${usageMinutes}m today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Instant Block Toggle Switch
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = appLimit.isBlocked,
                        onCheckedChange = onBlockToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                    Text(
                        text = if (appLimit.isBlocked) "Blocked" else "Allow",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (appLimit.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Quota & Remaining row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (appLimit.isBlocked || appLimit.remainingTimeMillis <= 0 && appLimit.dailyQuotaMinutes > 0)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onQuotaClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = quotaText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun QuotaSelectionDialog(
    appLimit: AppUsageLimit,
    onDismiss: () -> Unit,
    onSelectQuota: (Int) -> Unit
) {
    val quotaOptions = listOf(
        Pair("Unlimited", 0),
        Pair("15 minutes", 15),
        Pair("30 minutes", 30),
        Pair("45 minutes", 45),
        Pair("1 hour", 60),
        Pair("1 hour 30 mins", 90),
        Pair("2 hours", 120),
        Pair("3 hours", 180),
        Pair("4 hours", 240)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Set Quota for ${appLimit.appName}")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Choose daily time allowance:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(quotaOptions) { option ->
                        val isSelected = appLimit.dailyQuotaMinutes == option.second
                        Surface(
                            onClick = { onSelectQuota(option.second) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.first,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun getCategoryColor(category: AppCategory): Color {
    return when (category) {
        AppCategory.SOCIAL -> Color(0xFFE91E63)
        AppCategory.GAMES -> Color(0xFFFF9800)
        AppCategory.PRODUCTIVITY -> Color(0xFF2196F3)
        AppCategory.ENTERTAINMENT -> Color(0xFF9C27B0)
        AppCategory.EDUCATION -> Color(0xFF4CAF50)
        AppCategory.OTHER -> Color(0xFF607D8B)
    }
}

@Composable
fun getCategoryIcon(category: AppCategory) = when (category) {
    AppCategory.SOCIAL -> Icons.Rounded.Chat
    AppCategory.GAMES -> Icons.Rounded.SportsEsports
    AppCategory.PRODUCTIVITY -> Icons.Rounded.Work
    AppCategory.ENTERTAINMENT -> Icons.Rounded.Movie
    AppCategory.EDUCATION -> Icons.Rounded.School
    AppCategory.OTHER -> Icons.Rounded.Apps
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun AppUsageLimitsScreenPreview() {
    ParentSyncTheme {
        AppUsageLimitsScreen()
    }
}
