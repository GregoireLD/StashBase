package com.stashbase.ui.blueprints

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
import com.stashbase.domain.model.Blueprint
import com.stashbase.domain.repository.BlueprintRepository
import com.stashbase.ui.components.PhotoPickerField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditBlueprintUiState(
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val nameError: Boolean = false,
    val isLoading: Boolean = false,
    val savedId: Long? = null,
)

@HiltViewModel
class AddEditBlueprintViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: BlueprintRepository,
) : ViewModel() {

    private val blueprintId: Long? = savedStateHandle.get<Long>("blueprintId")?.takeIf { it != -1L }

    private val _state = MutableStateFlow(AddEditBlueprintUiState())
    val uiState: StateFlow<AddEditBlueprintUiState> = _state.asStateFlow()

    init {
        if (blueprintId != null) {
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                repository.getById(blueprintId).firstOrNull()?.let { bp ->
                    _state.update {
                        it.copy(
                            name = bp.name,
                            description = bp.description ?: "",
                            category = bp.category ?: "",
                            notes = bp.notes ?: "",
                            photoPath = bp.photoPath,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun update(block: AddEditBlueprintUiState.() -> AddEditBlueprintUiState) = _state.update(block)

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            val id = repository.upsert(
                Blueprint(
                    id = blueprintId ?: 0L,
                    name = s.name.trim(),
                    description = s.description.trim().ifBlank { null },
                    category = s.category.trim().ifBlank { null },
                    notes = s.notes.trim().ifBlank { null },
                    photoPath = s.photoPath,
                )
            )
            _state.update { it.copy(savedId = id) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBlueprintScreen(
    blueprintId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddEditBlueprintViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedId) {
        state.savedId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (blueprintId == null) "Nouveau modèle" else "Modifier le modèle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Check, contentDescription = "Enregistrer")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.update { copy(name = it, nameError = false) } },
                label = { Text("Nom *") },
                isError = state.nameError,
                supportingText = if (state.nameError) { { Text("Le nom est requis") } } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.category,
                onValueChange = { viewModel.update { copy(category = it) } },
                label = { Text("Catégorie") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.update { copy(description = it) } },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            PhotoPickerField(
                photoPath = state.photoPath,
                onPhotoChanged = { viewModel.update { copy(photoPath = it) } },
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
