package com.stashbase.ui.inventory

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
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.StorageLocationRepository
import com.stashbase.domain.repository.ToolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditToolUiState(
    val name: String = "",
    val type: ToolType = ToolType.NEEDLE,
    val brand: String = "",
    val size: String = "",
    val condition: ToolCondition = ToolCondition.GOOD,
    val locationId: Long? = null,
    val notes: String = "",
    val locations: List<StorageLocation> = emptyList(),
    val isSaved: Boolean = false,
    val nameError: Boolean = false,
)

@HiltViewModel
class AddEditToolViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val toolRepository: ToolRepository,
    private val locationRepository: StorageLocationRepository,
) : ViewModel() {

    private val toolId: Long? = savedStateHandle.get<Long>("toolId")
    private val _state = MutableStateFlow(AddEditToolUiState())
    val uiState: StateFlow<AddEditToolUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.getAll().collect { locs ->
                _state.update { it.copy(locations = locs) }
            }
        }
        if (toolId != null) {
            viewModelScope.launch {
                toolRepository.getById(toolId).firstOrNull()?.let { t ->
                    _state.update { _ ->
                        AddEditToolUiState(
                            name = t.name,
                            type = t.type,
                            brand = t.brand ?: "",
                            size = t.size ?: "",
                            condition = t.condition,
                            locationId = t.locationId,
                            notes = t.notes ?: "",
                        )
                    }
                }
            }
        }
    }

    fun update(block: AddEditToolUiState.() -> AddEditToolUiState) = _state.update(block)

    fun save() {
        if (_state.value.name.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        viewModelScope.launch {
            val s = _state.value
            toolRepository.upsert(
                Tool(
                    id = toolId ?: 0L,
                    name = s.name.trim(),
                    type = s.type,
                    brand = s.brand.trim().ifBlank { null },
                    size = s.size.trim().ifBlank { null },
                    condition = s.condition,
                    locationId = s.locationId,
                    notes = s.notes.trim().ifBlank { null },
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditToolScreen(
    toolId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditToolViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (toolId == null) "Nouvel outil" else "Modifier l'outil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Check, contentDescription = "Enregistrer")
                    }
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
                label = { Text("Nom *") },
                isError = state.nameError,
                modifier = Modifier.fillMaxWidth(),
            )

            EnumDropdownTool(
                label = "Type",
                options = ToolType.entries,
                selected = state.type,
                labelOf = { it.label },
                onSelect = { viewModel.update { copy(type = it) } }
            )

            EnumDropdownTool(
                label = "État",
                options = ToolCondition.entries,
                selected = state.condition,
                labelOf = { it.label },
                onSelect = { viewModel.update { copy(condition = it) } }
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = { viewModel.update { copy(brand = it) } },
                    label = { Text("Marque") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.size,
                    onValueChange = { viewModel.update { copy(size = it) } },
                    label = { Text("Taille / Spéc.") },
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.locations.isNotEmpty()) {
                var locationExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = locationExpanded, onExpandedChange = { locationExpanded = it }) {
                    OutlinedTextField(
                        value = state.locations.find { it.id == state.locationId }?.fullPath ?: "Aucun",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lieu de rangement") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = locationExpanded, onDismissRequest = { locationExpanded = false }) {
                        DropdownMenuItem(text = { Text("Aucun") }, onClick = { viewModel.update { copy(locationId = null) }; locationExpanded = false })
                        state.locations.forEach { loc ->
                            DropdownMenuItem(text = { Text(loc.fullPath) }, onClick = { viewModel.update { copy(locationId = loc.id) }; locationExpanded = false })
                        }
                    }
                }
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdownTool(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(labelOf(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
