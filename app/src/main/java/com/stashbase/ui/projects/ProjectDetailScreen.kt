package com.stashbase.ui.projects

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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.Project
import com.stashbase.domain.repository.ProjectRepository
import com.stashbase.ui.theme.CoveredGreen
import com.stashbase.ui.theme.LowStockRed
import com.stashbase.ui.theme.PartialOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    val project: StateFlow<Project?> = projectRepository.getById(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenBom: () -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val project by viewModel.project.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Projet") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Modifier") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenBom,
                icon = { Icon(Icons.Default.List, null) },
                text = { Text("Nomenclature") }
            )
        }
    ) { padding ->
        val p = project
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AssistChip(onClick = {}, label = { Text(p.status.label) })
                            if (p.category != null) {
                                SuggestionChip(onClick = {}, label = { Text(p.category) })
                            }
                        }
                        if (p.description != null) {
                            Text(p.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (p.notes != null) {
                            HorizontalDivider()
                            Text("Notes", style = MaterialTheme.typography.labelLarge)
                            Text(p.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (p.bomEntries.isNotEmpty()) {
                item {
                    Text("Nomenclature (${p.bomEntries.size} matériaux)", style = MaterialTheme.typography.titleMedium)
                }
                items(p.bomEntries, key = { it.id }) { entry ->
                    val coverColor = when {
                        entry.isCovered -> CoveredGreen
                        entry.effectiveAvailable > 0 -> PartialOrange
                        else -> LowStockRed
                    }
                    ListItem(
                        headlineContent = { Text(entry.materialName.ifBlank { "Matériau #${entry.materialId}" }) },
                        supportingContent = {
                            Text(
                                text = "Requis: ${entry.requiredQuantity} ${entry.materialUnit.abbreviation}" +
                                        " · Dispo: ${entry.effectiveAvailable} ${entry.materialUnit.abbreviation}" +
                                        if (entry.deficit > 0) " · Manque: ${entry.deficit}" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (entry.isCovered) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = coverColor,
                            )
                        }
                    )
                }
            } else {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.List, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("Aucune nomenclature", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            TextButton(onClick = onOpenBom) { Text("Ajouter des matériaux") }
                        }
                    }
                }
            }
        }
    }
}
