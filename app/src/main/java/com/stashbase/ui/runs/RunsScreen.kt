package com.stashbase.ui.runs

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.Run
import com.stashbase.domain.model.RunStatus
import com.stashbase.domain.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunsViewModel @Inject constructor(
    private val repository: RunRepository,
) : ViewModel() {

    val runs: StateFlow<List<Run>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: RunsViewModel = hiltViewModel(),
) {
    val allRuns by viewModel.runs.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("En cours", "Terminés")

    val filteredRuns = remember(allRuns, selectedTab) {
        when (selectedTab) {
            0 -> allRuns.filter { it.status != RunStatus.COMPLETED }
            else -> allRuns.filter { it.status == RunStatus.COMPLETED }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Projets") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau projet")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            if (filteredRuns.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Construction,
                            null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            if (selectedTab == 0) "Aucun projet en cours" else "Aucun projet terminé",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(filteredRuns, key = { it.id }) { run ->
                        RunListItem(
                            run = run,
                            onClick = { onOpen(run.id) },
                            onDelete = { viewModel.delete(run.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun RunListItem(
    run: Run,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(run.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (run.blueprintName.isNotBlank()) {
                    Text(
                        "Modèle : ${run.blueprintName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(run.status.label, style = MaterialTheme.typography.labelSmall) },
                    )
                    if (run.totalSteps > 0) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "${run.completedSteps}/${run.totalSteps} étapes",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (run.missingMaterials.isNotEmpty()) {
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
            title = { Text("Supprimer le projet ?") },
            text = { Text("\"${run.name}\" sera supprimé définitivement.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            },
        )
    }
}
