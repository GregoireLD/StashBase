package com.stashbase.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Bottom nav
    data object Inventory : Screen("inventory")
    data object Blueprints : Screen("blueprints")
    data object Runs : Screen("runs")
    data object Shopping : Screen("shopping")
    data object Locations : Screen("locations")

    // Inventory sub-screens
    data object AddEditMaterial : Screen("material/edit/{materialId}") {
        fun createRoute(materialId: Long = -1L) = "material/edit/$materialId"
    }
    data object AddEditTool : Screen("tool/edit/{toolId}") {
        fun createRoute(toolId: Long = -1L) = "tool/edit/$toolId"
    }
    data object AddEditFinishedItem : Screen("finished/edit/{itemId}") {
        fun createRoute(itemId: Long = -1L) = "finished/edit/$itemId"
    }

    // Blueprint sub-screens
    data object BlueprintDetail : Screen("blueprint/{blueprintId}") {
        fun createRoute(blueprintId: Long) = "blueprint/$blueprintId"
    }
    data object AddEditBlueprint : Screen("blueprint/edit/{blueprintId}") {
        fun createRoute(blueprintId: Long = -1L) = "blueprint/edit/$blueprintId"
    }

    // Run sub-screens
    data object RunDetail : Screen("run/{runId}") {
        fun createRoute(runId: Long) = "run/$runId"
    }
    data object AddEditRun : Screen("run/edit/{runId}") {
        fun createRoute(runId: Long = -1L) = "run/edit/$runId"
    }

    // Location sub-screens
    data object AddEditLocation : Screen("location/edit/{locationId}") {
        fun createRoute(locationId: Long = -1L) = "location/edit/$locationId"
    }
}

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Inventory, "Inventaire", Icons.Default.Inventory),
    BottomNavItem(Screen.Blueprints, "Modèles", Icons.Default.AutoStories),
    BottomNavItem(Screen.Runs, "Projets", Icons.Default.Construction),
    BottomNavItem(Screen.Shopping, "Courses", Icons.Default.ShoppingCart),
    BottomNavItem(Screen.Locations, "Lieux", Icons.Default.Place),
)
