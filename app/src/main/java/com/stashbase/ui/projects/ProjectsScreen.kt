package com.stashbase.ui.projects

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.Project
import com.stashbase.domain.model.ProjectStatus
import com.stashbase.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val filterStatus: ProjectStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _filterStatus = MutableStateFlow<ProjectStatus?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ProjectsUiState> = combine(
        _filterStatus,
        _searchQuery,
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) projectRepository.getAll() else projectRepository.search(query)
        }
    ) { status, query, projects ->
        ProjectsUiState(
            projects = if (status != null) projects.filter { it.status == status } else projects,
            filterStatus = status,
            searchQuery = query,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectsUiState())

    fun setFilter(status: ProjectStatus?) { _filterStatus.value = status }
    fun setSearch(query: String) { _searchQuery.value = query }
    fun delete(id: Long) = viewModelScope.launch { projectRepository.delete(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onAddProject: () -> Unit,
    onOpenProject: (Long) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Projets") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProject) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau projet")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher un projet…") },
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

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = state.filterStatus == null, onClick = { viewModel.setFilter(null) }, label = { Text("Tous") })
                }
                items(ProjectStatus.entries) { status ->
                    FilterChip(
                        selected = state.filterStatus == status,
                        onClick = { viewModel.setFilter(if (state.filterStatus == status) null else status) },
                        label = { Text(status.label) }
                    )
                }
            }

            LazyColumn {
                if (state.projects.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun projet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                items(state.projects, key = { it.id }) { project ->
                    ProjectCard(project = project, onClick = { onOpenProject(project.id) }, onDelete = { viewModel.delete(project.id) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                if (project.description != null) {
                    Text(project.description, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "${project.status.label}${if (project.category != null) " · ${project.category}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Ouvrir") }, leadingIcon = { Icon(Icons.Default.OpenInNew, null) }, onClick = { showMenu = false; onClick() })
                    DropdownMenuItem(text = { Text("Supprimer") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { showMenu = false; onDelete() })
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
