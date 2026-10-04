package com.example.parentsync.ui.tracking

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.parentsync.data.model.AppCategory
import com.example.parentsync.data.model.AppUsage
import com.example.parentsync.data.model.CategoryBreakdown
import com.example.parentsync.data.model.ScreenTimeSummary
import com.example.parentsync.ui.theme.ParentSyncTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActivityTrackingScreen(
    viewModel: AppActivityTrackingViewModel? = null,
    onNavigateToLimits: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // If viewModel is null (e.g. in preview), we can use dummy or instantiate via factory
    val vm = viewModel ?: remember {
        AppActivityTrackingViewModel(context.applicationContext as android.app.Application)
    }

    val appUsages by vm.appUsages.collectAsState()
    val summary by vm.screenTimeSummary.collectAsState()
    val breakdowns by vm.categoryBreakdowns.collectAsState()
    var showPermissionBanner by remember { mutableStateOf(!vm.hasPermission) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Activity & Screen Time",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Today's Digital Wellbeing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLimits) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = "Usage Limits & Blocking"
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Usage Stats"
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
            // Permission Banner if usage stats permission is missing
            if (showPermissionBanner) {
                item {
                    PermissionBanner(
                        onRequestPermission = {
                            try {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        onDismiss = { showPermissionBanner = false }
                    )
                }
            }

            // Screen Time Summary Card
            item {
                summary?.let {
                    ScreenTimeSummaryCard(summary = it)
                }
            }

            // Category Breakdown Section
            if (breakdowns.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                item {
                    CategoryBreakdownCard(breakdowns = breakdowns)
                }
            }

            // App Usage List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Usage Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${appUsages.size} apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // App Usage Items
            items(appUsages, key = { it.packageName }) { appUsage ->
                AppUsageItemCard(appUsage = appUsage)
            }
        }
    }
}

@Composable
fun PermissionBanner(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Usage Access Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "ParentSync needs Usage Access permission to accurately monitor screen time and app usage stats for child safety.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun ScreenTimeSummaryCard(summary: ScreenTimeSummary) {
    val totalHours = summary.totalUsageMillis / (1000 * 60 * 60)
    val totalMinutes = (summary.totalUsageMillis / (1000 * 60)) % 60

    val avgHours = summary.dailyAverageMillis / (1000 * 60 * 60)
    val avgMinutes = (summary.dailyAverageMillis / (1000 * 60)) % 60

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Screen Time Today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "${totalHours}h ${totalMinutes}m",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Daily Average",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${avgHours}h ${avgMinutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "vs Yesterday",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (summary.comparisonToYesterdayPercent <= 0) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = if (summary.comparisonToYesterdayPercent <= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${kotlin.math.abs(summary.comparisonToYesterdayPercent)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.comparisonToYesterdayPercent <= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(breakdowns: List<CategoryBreakdown>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stacked progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
            ) {
                breakdowns.forEach { breakdown ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(breakdown.percentage.coerceAtLeast(1f))
                            .background(getCategoryColor(breakdown.category))
                    )
                }
            }

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdowns.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { breakdown ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(breakdown.category))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${breakdown.category.displayName} (${String.format("%.1f", breakdown.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItemCard(appUsage: AppUsage) {
    val hours = appUsage.usageTimeMillis / (1000 * 60 * 60)
    val minutes = (appUsage.usageTimeMillis / (1000 * 60)) % 60

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getCategoryColor(appUsage.category).copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(appUsage.category),
                            contentDescription = null,
                            tint = getCategoryColor(appUsage.category),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = appUsage.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getCategoryColor(appUsage.category).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = appUsage.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = getCategoryColor(appUsage.category),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
    AppCategory.SOCIAL -> Icons.AutoMirrored.Rounded.Chat
    AppCategory.GAMES -> Icons.Rounded.SportsEsports
    AppCategory.PRODUCTIVITY -> Icons.Rounded.Work
    AppCategory.ENTERTAINMENT -> Icons.Rounded.Movie
    AppCategory.EDUCATION -> Icons.Rounded.School
    AppCategory.OTHER -> Icons.Rounded.Apps
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun AppActivityTrackingScreenPreview() {
    ParentSyncTheme {
        AppActivityTrackingScreen()
    }
}
