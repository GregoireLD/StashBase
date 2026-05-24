package com.stashbase.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Bottom nav destinations
    data object Inventory : Screen("inventory")
    data object Projects : Screen("projects")
    data object Shopping : Screen("shopping")
    data object Locations : Screen("locations")

    // Inventory sub-screens
    data object AddEditMaterial : Screen("material/edit/{materialId}") {
        fun createRoute(materialId: Long = -1L) = "material/edit/$materialId"
    }
    data object MaterialDetail : Screen("material/{materialId}") {
        fun createRoute(materialId: Long) = "material/$materialId"
    }
    data object AddEditTool : Screen("tool/edit/{toolId}") {
        fun createRoute(toolId: Long = -1L) = "tool/edit/$toolId"
    }

    // Project sub-screens
    data object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: Long) = "project/$projectId"
    }
    data object AddEditProject : Screen("project/edit/{projectId}") {
        fun createRoute(projectId: Long = -1L) = "project/edit/$projectId"
    }
    data object BomEditor : Screen("project/{projectId}/bom") {
        fun createRoute(projectId: Long) = "project/$projectId/bom"
    }

    // Location sub-screens
    data object AddEditLocation : Screen("location/edit/{locationId}") {
        fun createRoute(locationId: Long = -1L) = "location/edit/$locationId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Inventory, "Inventaire", Icons.Default.Inventory),
    BottomNavItem(Screen.Projects, "Projets", Icons.Default.FolderOpen),
    BottomNavItem(Screen.Shopping, "Courses", Icons.Default.ShoppingCart),
    BottomNavItem(Screen.Locations, "Lieux", Icons.Default.Place),
)
