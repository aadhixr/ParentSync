package com.example.parentsync.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Dashboard : Screen
    @Serializable
    data object Tracking : Screen
    @Serializable
    data object Limits : Screen
}
