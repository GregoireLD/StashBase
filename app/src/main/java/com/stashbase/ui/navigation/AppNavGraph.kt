package com.stashbase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stashbase.ui.inventory.AddEditMaterialScreen
import com.stashbase.ui.inventory.InventoryScreen
import com.stashbase.ui.locations.LocationsScreen
import com.stashbase.ui.projects.AddEditProjectScreen
import com.stashbase.ui.projects.BomEditorScreen
import com.stashbase.ui.projects.ProjectDetailScreen
import com.stashbase.ui.projects.ProjectsScreen
import com.stashbase.ui.shopping.ShoppingListScreen

@Composable
fun AppNavGraph(navController: NavHostController, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    NavHost(navController = navController, startDestination = Screen.Inventory.route, modifier = modifier) {

        composable(Screen.Inventory.route) {
            InventoryScreen(
                onAddMaterial = { navController.navigate(Screen.AddEditMaterial.createRoute()) },
                onEditMaterial = { id -> navController.navigate(Screen.AddEditMaterial.createRoute(id)) },
                onAddTool = { navController.navigate(Screen.AddEditTool.createRoute()) },
                onEditTool = { id -> navController.navigate(Screen.AddEditTool.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditMaterial.route,
            arguments = listOf(navArgument("materialId") { type = NavType.LongType })
        ) { backStack ->
            val materialId = backStack.arguments?.getLong("materialId") ?: -1L
            AddEditMaterialScreen(
                materialId = if (materialId == -1L) null else materialId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.AddEditTool.route,
            arguments = listOf(navArgument("toolId") { type = NavType.LongType })
        ) { backStack ->
            val toolId = backStack.arguments?.getLong("toolId") ?: -1L
            com.stashbase.ui.inventory.AddEditToolScreen(
                toolId = if (toolId == -1L) null else toolId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onAddProject = { navController.navigate(Screen.AddEditProject.createRoute()) },
                onOpenProject = { id -> navController.navigate(Screen.ProjectDetail.createRoute(id)) },
            )
        }

        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStack ->
            val projectId = backStack.arguments!!.getLong("projectId")
            ProjectDetailScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.AddEditProject.createRoute(projectId)) },
                onOpenBom = { navController.navigate(Screen.BomEditor.createRoute(projectId)) },
            )
        }

        composable(
            route = Screen.AddEditProject.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStack ->
            val projectId = backStack.arguments?.getLong("projectId") ?: -1L
            AddEditProjectScreen(
                projectId = if (projectId == -1L) null else projectId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.BomEditor.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStack ->
            val projectId = backStack.arguments!!.getLong("projectId")
            BomEditorScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onGenerateShopping = {
                    navController.navigate(Screen.Shopping.route) {
                        popUpTo(Screen.Projects.route)
                    }
                },
            )
        }

        composable(Screen.Shopping.route) {
            ShoppingListScreen()
        }

        composable(Screen.Locations.route) {
            LocationsScreen(
                onAddLocation = { navController.navigate(Screen.AddEditLocation.createRoute()) },
                onEditLocation = { id -> navController.navigate(Screen.AddEditLocation.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditLocation.route,
            arguments = listOf(navArgument("locationId") { type = NavType.LongType })
        ) { backStack ->
            val locationId = backStack.arguments?.getLong("locationId") ?: -1L
            com.stashbase.ui.locations.AddEditLocationScreen(
                locationId = if (locationId == -1L) null else locationId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
