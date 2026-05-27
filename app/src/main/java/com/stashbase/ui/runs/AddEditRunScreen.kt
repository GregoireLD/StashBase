package com.stashbase.ui.runs

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
import com.stashbase.domain.model.Run
import com.stashbase.domain.repository.RunRepository
import com.stashbase.ui.components.PhotoPickerField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditRunUiState(
    val name: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val nameError: Boolean = false,
    val isLoading: Boolean = false,
    val savedId: Long? = null,
)

@HiltViewModel
class AddEditRunViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val runRepository: RunRepository,
) : ViewModel() {

    private val runId: Long? = savedStateHandle.get<Long>("runId")?.takeIf { it != -1L }

    private val _state = MutableStateFlow(AddEditRunUiState())
    val uiState: StateFlow<AddEditRunUiState> = _state.asStateFlow()

    init {
        if (runId != null) {
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                runRepository.getById(runId).firstOrNull()?.let { run ->
                    _state.update {
                        it.copy(
                            name = run.name,
                            notes = run.notes ?: "",
                            photoPath = run.photoPath,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun update(block: AddEditRunUiState.() -> AddEditRunUiState) = _state.update(block)

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            val existing = if (runId != null) runRepository.getById(runId).firstOrNull() else null
            val id = runRepository.upsert(
                Run(
                    id = runId ?: 0L,
                    blueprintId = existing?.blueprintId,
                    name = s.name.trim(),
                    status = existing?.status ?: com.stashbase.domain.model.RunStatus.IN_PROGRESS,
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
fun AddEditRunScreen(
    runId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddEditRunViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedId) {
        state.savedId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (runId == null) "Nouveau projet" else "Modifier le projet") },
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
