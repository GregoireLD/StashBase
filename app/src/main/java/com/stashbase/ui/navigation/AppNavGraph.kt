package com.stashbase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stashbase.ui.blueprints.AddEditBlueprintScreen
import com.stashbase.ui.blueprints.BlueprintDetailScreen
import com.stashbase.ui.blueprints.BlueprintsScreen
import com.stashbase.ui.inventory.AddEditFinishedItemScreen
import com.stashbase.ui.inventory.AddEditMaterialScreen
import com.stashbase.ui.inventory.FinishedItemDetailScreen
import com.stashbase.ui.inventory.InventoryScreen
import com.stashbase.ui.inventory.MaterialDetailScreen
import com.stashbase.ui.inventory.ToolDetailScreen
import com.stashbase.ui.locations.LocationsScreen
import com.stashbase.ui.runs.AddEditRunScreen
import com.stashbase.ui.runs.RunDetailScreen
import com.stashbase.ui.runs.RunsScreen
import com.stashbase.ui.shopping.ShoppingListScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Inventory.route,
        modifier = modifier,
    ) {

        // ── Inventory ──────────────────────────────────────────────────────────
        composable(Screen.Inventory.route) {
            InventoryScreen(
                onAddMaterial = { navController.navigate(Screen.AddEditMaterial.createRoute()) },
                onViewMaterial = { id -> navController.navigate(Screen.MaterialDetail.createRoute(id)) },
                onAddTool = { navController.navigate(Screen.AddEditTool.createRoute()) },
                onViewTool = { id -> navController.navigate(Screen.ToolDetail.createRoute(id)) },
                onAddFinishedItem = { navController.navigate(Screen.AddEditFinishedItem.createRoute()) },
                onViewFinishedItem = { id -> navController.navigate(Screen.FinishedItemDetail.createRoute(id)) },
            )
        }

        composable(
            route = Screen.MaterialDetail.route,
            arguments = listOf(navArgument("materialId") { type = NavType.LongType }),
        ) {
            MaterialDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditMaterial.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditMaterial.route,
            arguments = listOf(navArgument("materialId") { type = NavType.LongType }),
        ) { back ->
            val id = back.arguments?.getLong("materialId") ?: -1L
            AddEditMaterialScreen(
                materialId = if (id == -1L) null else id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack(Screen.Inventory.route, inclusive = false) },
            )
        }

        composable(
            route = Screen.ToolDetail.route,
            arguments = listOf(navArgument("toolId") { type = NavType.LongType }),
        ) {
            ToolDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditTool.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditTool.route,
            arguments = listOf(navArgument("toolId") { type = NavType.LongType }),
        ) { back ->
            val id = back.arguments?.getLong("toolId") ?: -1L
            com.stashbase.ui.inventory.AddEditToolScreen(
                toolId = if (id == -1L) null else id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack(Screen.Inventory.route, inclusive = false) },
            )
        }

        composable(
            route = Screen.FinishedItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) {
            FinishedItemDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditFinishedItem.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditFinishedItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType }),
        ) { back ->
            val id = back.arguments?.getLong("itemId") ?: -1L
            AddEditFinishedItemScreen(
                itemId = if (id == -1L) null else id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack(Screen.Inventory.route, inclusive = false) },
            )
        }

        // ── Blueprints ─────────────────────────────────────────────────────────
        composable(Screen.Blueprints.route) {
            BlueprintsScreen(
                onAdd = { navController.navigate(Screen.AddEditBlueprint.createRoute()) },
                onOpen = { id -> navController.navigate(Screen.BlueprintDetail.createRoute(id)) },
            )
        }

        composable(
            route = Screen.BlueprintDetail.route,
            arguments = listOf(navArgument("blueprintId") { type = NavType.LongType }),
        ) {
            BlueprintDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditBlueprint.createRoute(id)) },
                onNavigateToRun = { runId ->
                    navController.navigate(Screen.RunDetail.createRoute(runId)) {
                        popUpTo(Screen.Blueprints.route)
                    }
                },
            )
        }

        composable(
            route = Screen.AddEditBlueprint.route,
            arguments = listOf(navArgument("blueprintId") { type = NavType.LongType }),
        ) { back ->
            val id = back.arguments?.getLong("blueprintId") ?: -1L
            val isNew = id == -1L
            AddEditBlueprintScreen(
                blueprintId = if (isNew) null else id,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    if (isNew) {
                        navController.navigate(Screen.BlueprintDetail.createRoute(savedId)) {
                            popUpTo(Screen.Blueprints.route)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        // ── Runs ───────────────────────────────────────────────────────────────
        composable(Screen.Runs.route) {
            RunsScreen(
                onAdd = { navController.navigate(Screen.AddEditRun.createRoute()) },
                onOpen = { id -> navController.navigate(Screen.RunDetail.createRoute(id)) },
            )
        }

        composable(
            route = Screen.RunDetail.route,
            arguments = listOf(navArgument("runId") { type = NavType.LongType }),
        ) {
            RunDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Screen.AddEditRun.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditRun.route,
            arguments = listOf(navArgument("runId") { type = NavType.LongType }),
        ) { back ->
            val id = back.arguments?.getLong("runId") ?: -1L
            val isNew = id == -1L
            AddEditRunScreen(
                runId = if (isNew) null else id,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    if (isNew) {
                        navController.navigate(Screen.RunDetail.createRoute(savedId)) {
                            popUpTo(Screen.Runs.route)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        // ── Shopping ───────────────────────────────────────────────────────────
        composable(Screen.Shopping.route) {
            ShoppingListScreen(
                onNavigateToRun = { runId ->
                    navController.navigate(Screen.RunDetail.createRoute(runId)) {
                        popUpTo(Screen.Shopping.route)
                    }
                },
            )
        }

        // ── Locations ──────────────────────────────────────────────────────────
        composable(Screen.Locations.route) {
            LocationsScreen(
                onAddLocation = { navController.navigate(Screen.AddEditLocation.createRoute()) },
                onEditLocation = { id -> navController.navigate(Screen.AddEditLocation.createRoute(id)) },
            )
        }

        composable(
            route = Screen.AddEditLocation.route,
            arguments = listOf(navArgument("locationId") { type = NavType.LongType }),
        ) { back ->
            val id = back.arguments?.getLong("locationId") ?: -1L
            com.stashbase.ui.locations.AddEditLocationScreen(
                locationId = if (id == -1L) null else id,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
