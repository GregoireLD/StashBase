package com.stashbase.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.MaterialRepository
import com.stashbase.domain.repository.ProjectRepository
import com.stashbase.domain.repository.ShoppingListRepository
import com.stashbase.ui.theme.CoveredGreen
import com.stashbase.ui.theme.LowStockRed
import com.stashbase.ui.theme.PartialOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BomEditorUiState(
    val bomEntries: List<BomEntry> = emptyList(),
    val allMaterials: List<Material> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class BomEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val materialRepository: MaterialRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {

    val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    val uiState: StateFlow<BomEditorUiState> = combine(
        projectRepository.getBomEntries(projectId),
        materialRepository.getAll(),
    ) { bomEntities, materials ->
        val matById = materials.associateBy { it.id }
        val entries = bomEntities.map { entry ->
            val mat = matById[entry.materialId]
            entry.copy(
                materialName = mat?.name ?: "",
                materialUnit = mat?.unit ?: MaterialUnit.PIECE,
                availableQuantity = mat?.quantity ?: 0.0,
            )
        }
        BomEditorUiState(bomEntries = entries, allMaterials = materials, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BomEditorUiState())

    fun addEntry(materialId: Long, quantity: Double) = viewModelScope.launch {
        projectRepository.upsertBomEntry(
            BomEntry(projectId = projectId, materialId = materialId, requiredQuantity = quantity)
        )
    }

    fun deleteEntry(id: Long) = viewModelScope.launch { projectRepository.deleteBomEntry(id) }

    fun generateShoppingList() = viewModelScope.launch {
        shoppingListRepository.generateFromProjectDeficits(projectId)
    }
}

@Composable
fun BomEditorScreen(
    onBack: () -> Unit,
    onGenerateShopping: () -> Unit,
    viewModel: BomEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nomenclature") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { viewModel.generateShoppingList(); onGenerateShopping() }) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Générer liste de courses")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un matériau")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (state.bomEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun matériau dans la nomenclature", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            items(state.bomEntries, key = { it.id }) { entry ->
                val coverColor = when {
                    entry.isCovered -> CoveredGreen
                    entry.effectiveAvailable > 0 -> PartialOrange
                    else -> LowStockRed
                }
                ListItem(
                    headlineContent = { Text(entry.materialName.ifBlank { "Matériau #${entry.materialId}" }) },
                    supportingContent = {
                        Column {
                            Text("Requis: ${entry.requiredQuantity} ${entry.materialUnit.abbreviation}", style = MaterialTheme.typography.bodySmall)
                            Text("En stock: ${entry.availableQuantity} ${entry.materialUnit.abbreviation}", style = MaterialTheme.typography.bodySmall)
                            if (entry.deficit > 0) {
                                Text("Manque: ${entry.deficit} ${entry.materialUnit.abbreviation}", style = MaterialTheme.typography.bodySmall, color = LowStockRed)
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            if (entry.isCovered) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = coverColor,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showAddDialog) {
        AddBomEntryDialog(
            materials = state.allMaterials,
            existingIds = state.bomEntries.map { it.materialId }.toSet(),
            onConfirm = { materialId, qty -> viewModel.addEntry(materialId, qty); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun AddBomEntryDialog(
    materials: List<Material>,
    existingIds: Set<Long>,
    onConfirm: (Long, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val available = materials.filter { it.id !in existingIds }
    var selectedMaterial by remember { mutableStateOf(available.firstOrNull()) }
    var quantity by remember { mutableStateOf("1") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un matériau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (available.isEmpty()) {
                    Text("Tous les matériaux sont déjà dans la nomenclature.")
                } else {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedMaterial?.name ?: "Choisir…",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Matériau") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            available.forEach { mat ->
                                DropdownMenuItem(
                                    text = { Text("${mat.name} (${mat.quantity} ${mat.unit.abbreviation})") },
                                    onClick = { selectedMaterial = mat; expanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantité requise (${selectedMaterial?.unit?.abbreviation ?: ""})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mat = selectedMaterial ?: return@TextButton
                    val qty = quantity.toDoubleOrNull() ?: return@TextButton
                    onConfirm(mat.id, qty)
                },
                enabled = selectedMaterial != null && available.isNotEmpty()
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
