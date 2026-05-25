package com.stashbase.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stashbase.domain.model.FinishedItem
import com.stashbase.domain.model.Material
import com.stashbase.domain.model.MaterialType
import com.stashbase.domain.model.Tool
import com.stashbase.ui.theme.LowStockRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAddMaterial: () -> Unit,
    onEditMaterial: (Long) -> Unit,
    onAddTool: () -> Unit,
    onEditTool: (Long) -> Unit,
    onAddFinishedItem: () -> Unit,
    onEditFinishedItem: (Long) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventaire") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (state.tab) {
                        InventoryTab.MATERIALS -> onAddMaterial()
                        InventoryTab.TOOLS -> onAddTool()
                        InventoryTab.FINISHED_ITEMS -> onAddFinishedItem()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true,
            )

            TabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(
                    selected = state.tab == InventoryTab.MATERIALS,
                    onClick = { viewModel.setTab(InventoryTab.MATERIALS) },
                    text = { Text("Matériaux (${state.materials.size})") }
                )
                Tab(
                    selected = state.tab == InventoryTab.TOOLS,
                    onClick = { viewModel.setTab(InventoryTab.TOOLS) },
                    text = { Text("Outils (${state.tools.size})") }
                )
                Tab(
                    selected = state.tab == InventoryTab.FINISHED_ITEMS,
                    onClick = { viewModel.setTab(InventoryTab.FINISHED_ITEMS) },
                    text = { Text("Réalisations (${state.finishedItems.size})") }
                )
            }

            when (state.tab) {
                InventoryTab.MATERIALS -> MaterialsList(
                    materials = state.materials,
                    selectedType = state.selectedType,
                    onTypeFilter = viewModel::setTypeFilter,
                    onEdit = onEditMaterial,
                    onDelete = viewModel::deleteMaterial,
                )
                InventoryTab.TOOLS -> ToolsList(
                    tools = state.tools,
                    onEdit = onEditTool,
                    onDelete = viewModel::deleteTool,
                )
                InventoryTab.FINISHED_ITEMS -> FinishedItemsList(
                    items = state.finishedItems,
                    onEdit = onEditFinishedItem,
                    onDelete = viewModel::deleteFinishedItem,
                )
            }
        }
    }
}

@Composable
private fun MaterialsList(
    materials: List<Material>,
    selectedType: MaterialType?,
    onTypeFilter: (MaterialType?) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { onTypeFilter(null) },
                        label = { Text("Tous") }
                    )
                }
                items(MaterialType.entries) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeFilter(if (selectedType == type) null else type) },
                        label = { Text(type.label) }
                    )
                }
            }
        }

        if (materials.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun matériau", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        items(materials, key = { it.id }) { material ->
            MaterialCard(material = material, onEdit = onEdit, onDelete = onDelete)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun MaterialCard(
    material: Material,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(material.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (material.isLowStock) {
                    Icon(Icons.Default.Warning, contentDescription = "Stock bas", tint = LowStockRed, modifier = Modifier.size(16.dp))
                }
            }
        },
        supportingContent = {
            Column {
                Text(
                    text = buildString {
                        append(material.type.label)
                        if (material.brand != null) append(" · ${material.brand}")
                        if (material.color != null) append(" · ${material.color}")
                        if (material.colorCode != null) append(" (${material.colorCode})")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (material.locationName != null) {
                    Text(
                        text = "📍 ${material.locationName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${material.quantity} ${material.unit.abbreviation}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (material.allocatedQuantity > 0) {
                        Text(
                            text = "−${material.allocatedQuantity} alloué",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit(material.id) }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { showMenu = false; onDelete(material.id) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ToolsList(
    tools: List<Tool>,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn {
        if (tools.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun outil", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        items(tools, key = { it.id }) { tool ->
            ToolCard(tool = tool, onEdit = onEdit, onDelete = onDelete)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun ToolCard(
    tool: Tool,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(tool.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = buildString {
                    append(tool.type.label)
                    if (tool.brand != null) append(" · ${tool.brand}")
                    if (tool.size != null) append(" · ${tool.size}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { showMenu = false; onEdit(tool.id) }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { showMenu = false; onDelete(tool.id) }
                    )
                }
            }
        }
    )
}

@Composable
private fun FinishedItemsList(
    items: List<FinishedItem>,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn {
        if (items.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune réalisation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        items(items, key = { it.id }) { item ->
            FinishedItemCard(item = item, onEdit = onEdit, onDelete = onDelete)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun FinishedItemCard(
    item: FinishedItem,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(
                    text = buildString {
                        append(item.status.label)
                        if (!item.runName.isNullOrBlank()) append(" · ${item.runName}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.sellingPrice != null) {
                    Text(
                        text = "Prix : ${item.sellingPrice} €",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.quantity} ${item.unit.abbreviation}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit(item.id) }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { showMenu = false; onDelete(item.id) }
                        )
                    }
                }
            }
        }
    )
}
