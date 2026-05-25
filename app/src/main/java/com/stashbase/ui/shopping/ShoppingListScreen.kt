package com.stashbase.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.ShoppingListItem
import com.stashbase.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: ShoppingListRepository,
) : ViewModel() {

    val items: StateFlow<List<ShoppingListItem>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setChecked(id: Long, checked: Boolean) = viewModelScope.launch { repository.setChecked(id, checked) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
    fun deleteChecked() = viewModelScope.launch { repository.deleteChecked() }

    fun addItem(name: String, quantity: Double? = null) = viewModelScope.launch {
        repository.upsert(ShoppingListItem(name = name, quantity = quantity))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onNavigateToProject: (Long) -> Unit = {},
    viewModel: ShoppingListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val checkedCount = items.count { it.isChecked }

    // Group: project items first (sorted by project name), manual items last
    val projectGroups = remember(items) {
        items
            .filter { it.sourceProjectId != null }
            .groupBy { it.sourceProjectId!! }
            .entries
            .sortedBy { (_, v) -> v.first().sourceProjectName ?: "" }
    }
    val manualItems = remember(items) {
        items.filter { it.sourceProjectId == null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liste de courses") },
                actions = {
                    if (checkedCount > 0) {
                        TextButton(onClick = viewModel::deleteChecked) {
                            Text("Effacer cochés ($checkedCount)")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un article")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Text("Liste vide", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // ── Project groups ─────────────────────────────────────────────────
            projectGroups.forEach { (projectId, groupItems) ->
                val projectName = groupItems.first().sourceProjectName ?: "Projet #$projectId"
                item(key = "header_$projectId") {
                    ProjectGroupHeader(
                        projectName = projectName,
                        onClick = { onNavigateToProject(projectId) },
                    )
                }
                items(groupItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onChecked = { viewModel.setChecked(item.id, it) },
                        onDelete = { viewModel.delete(item.id) },
                        showProjectName = false,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Manual items ───────────────────────────────────────────────────
            if (manualItems.isNotEmpty()) {
                item(key = "header_manual") {
                    ProjectGroupHeader(
                        projectName = "Courses libres",
                        onClick = null,
                    )
                }
                items(manualItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onChecked = { viewModel.setChecked(item.id, it) },
                        onDelete = { viewModel.delete(item.id) },
                        showProjectName = false,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddShoppingItemDialog(
            onConfirm = { name -> viewModel.addItem(name); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun ProjectGroupHeader(projectName: String, onClick: (() -> Unit)?) {
    val modifier = if (onClick != null)
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    else
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = projectName,
            style = MaterialTheme.typography.titleSmall,
            color = if (onClick != null)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Aller au projet",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    onChecked: (Boolean) -> Unit,
    onDelete: () -> Unit,
    showProjectName: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                if (item.quantity != null && item.unit != null) {
                    Text(
                        "${item.quantity} ${item.unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (showProjectName && item.sourceProjectName != null) {
                    Text(
                        item.sourceProjectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        leadingContent = {
            Checkbox(checked = item.isChecked, onCheckedChange = onChecked)
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    )
}

@Composable
private fun AddShoppingItemDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un article") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom de l'article") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
