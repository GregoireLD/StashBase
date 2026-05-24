package com.stashbase.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.Project
import com.stashbase.domain.model.ProjectStatus
import com.stashbase.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditProjectUiState(
    val name: String = "",
    val description: String = "",
    val status: ProjectStatus = ProjectStatus.IDEA,
    val category: String = "",
    val notes: String = "",
    val isSaved: Boolean = false,
    val nameError: Boolean = false,
)

@HiltViewModel
class AddEditProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val projectId: Long? = savedStateHandle.get<Long>("projectId")
    private val _state = MutableStateFlow(AddEditProjectUiState())
    val uiState: StateFlow<AddEditProjectUiState> = _state.asStateFlow()

    init {
        if (projectId != null) {
            viewModelScope.launch {
                projectRepository.getById(projectId).firstOrNull()?.let { p ->
                    _state.update { _ ->
                        AddEditProjectUiState(
                            name = p.name,
                            description = p.description ?: "",
                            status = p.status,
                            category = p.category ?: "",
                            notes = p.notes ?: "",
                        )
                    }
                }
            }
        }
    }

    fun update(block: AddEditProjectUiState.() -> AddEditProjectUiState) = _state.update(block)

    fun save() {
        if (_state.value.name.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        viewModelScope.launch {
            val s = _state.value
            projectRepository.upsert(
                Project(
                    id = projectId ?: 0L,
                    name = s.name.trim(),
                    description = s.description.trim().ifBlank { null },
                    status = s.status,
                    category = s.category.trim().ifBlank { null },
                    notes = s.notes.trim().ifBlank { null },
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProjectScreen(
    projectId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (projectId == null) "Nouveau projet" else "Modifier le projet") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = viewModel::save) { Icon(Icons.Default.Check, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.update { copy(name = it, nameError = false) } },
                label = { Text("Nom du projet *") },
                isError = state.nameError,
                modifier = Modifier.fillMaxWidth(),
            )

            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                OutlinedTextField(
                    value = state.status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Statut") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    ProjectStatus.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label) }, onClick = { viewModel.update { copy(status = s) }; statusExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = state.category,
                onValueChange = { viewModel.update { copy(category = it) } },
                label = { Text("Catégorie (ex: Couture, Menuiserie…)") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.update { copy(description = it) } },
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { copy(notes = it) } },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
