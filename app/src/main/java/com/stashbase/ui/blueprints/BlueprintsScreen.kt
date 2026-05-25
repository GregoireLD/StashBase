package com.stashbase.ui.blueprints

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.Blueprint
import com.stashbase.domain.repository.BlueprintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BlueprintsViewModel @Inject constructor(
    private val repository: BlueprintRepository,
) : ViewModel() {

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    val blueprints: StateFlow<List<Blueprint>> = _search
        .flatMapLatest { q -> if (q.isBlank()) repository.getAll() else repository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(q: String) { _search.value = q }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: BlueprintsViewModel = hiltViewModel(),
) {
    val blueprints by viewModel.blueprints.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modèles") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau modèle")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = viewModel::setSearch,
                placeholder = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (search.isNotEmpty()) {
                    { IconButton(onClick = { viewModel.setSearch("") }) { Icon(Icons.Default.Clear, null) } }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )

            if (blueprints.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.AutoStories,
                            null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Text("Aucun modèle", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn {
                    items(blueprints, key = { it.id }) { blueprint ->
                        BlueprintListItem(
                            blueprint = blueprint,
                            onClick = { onOpen(blueprint.id) },
                            onDelete = { viewModel.delete(blueprint.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueprintListItem(
    blueprint: Blueprint,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(blueprint.name) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!blueprint.category.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(blueprint.category, style = MaterialTheme.typography.labelSmall) },
                    )
                }
                if (blueprint.bomEntries.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${blueprint.bomEntries.size} matières", style = MaterialTheme.typography.labelSmall) },
                    )
                }
                if (blueprint.steps.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${blueprint.steps.size} étapes", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (blueprint.missingMaterials.isNotEmpty()) {
                    Icon(
                        Icons.Default.Warning,
                        "Matières manquantes",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Supprimer", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le modèle ?") },
            text = { Text("\"${blueprint.name}\" sera supprimé définitivement.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            },
        )
    }
}
