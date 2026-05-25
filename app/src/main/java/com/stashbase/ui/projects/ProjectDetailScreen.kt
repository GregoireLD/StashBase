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

// ── ViewModel ──────────────────────────────────────────────────────────────────

data class ProjectDetailUiState(
    val project: Project? = null,
    val allMaterials: List<Material> = emptyList(),
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val materialRepository: MaterialRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {

    val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        projectRepository.getById(projectId),
        materialRepository.getAll(),
    ) { project, materials ->
        val matById = materials.associateBy { it.id }
        val enrichedBom = project?.bomEntries?.map { entry ->
            val mat = matById[entry.materialId]
            entry.copy(
                materialName = mat?.name ?: "",
                materialUnit = mat?.unit ?: entry.materialUnit,
                availableQuantity = mat?.quantity ?: 0.0,
            )
        } ?: emptyList()
        ProjectDetailUiState(
            project = project?.copy(bomEntries = enrichedBom),
            allMaterials = materials,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailUiState())

    fun addEntry(materialId: Long, quantity: Double) = viewModelScope.launch {
        projectRepository.upsertBomEntry(
            BomEntry(projectId = projectId, materialId = materialId, requiredQuantity = quantity)
        )
    }

    fun updateEntry(entry: BomEntry, quantity: Double, notes: String?) = viewModelScope.launch {
        projectRepository.upsertBomEntry(entry.copy(requiredQuantity = quantity, notes = notes))
    }

    fun deleteEntry(id: Long) = viewModelScope.launch { projectRepository.deleteBomEntry(id) }

    fun generateShoppingList() = viewModelScope.launch {
        shoppingListRepository.generateFromProjectDeficits(projectId)
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onGenerateShopping: () -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<BomEntry?>(null) }

    val project = state.project
    val missingCount = project?.missingMaterials?.size ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Projet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                    BadgedBox(
                        badge = {
                            if (missingCount > 0) Badge { Text("$missingCount") }
                        }
                    ) {
                        IconButton(onClick = { viewModel.generateShoppingList(); onGenerateShopping() }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Générer liste de courses")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (project == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // ── Project info card ──────────────────────────────────────────────
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AssistChip(onClick = {}, label = { Text(project.status.label) })
                            if (project.category != null) {
                                SuggestionChip(onClick = {}, label = { Text(project.category) })
                            }
                        }
                        if (project.description != null) {
                            Text(project.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (project.deadlineMillis != null) {
                            val formatted = remember(project.deadlineMillis) {
                                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
                                    .format(java.util.Date(project.deadlineMillis))
                            }
                            Text(
                                "Échéance : $formatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (project.notes != null) {
                            HorizontalDivider()
                            Text("Notes", style = MaterialTheme.typography.labelLarge)
                            Text(project.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ── BOM section header ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Nomenclature (${project.bomEntries.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter un matériau")
                    }
                }
            }

            // ── BOM entries ────────────────────────────────────────────────────
            if (project.bomEntries.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Aucun matériau",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            } else {
                items(project.bomEntries, key = { it.id }) { entry ->
                    val coverColor = when {
                        entry.isCovered -> CoveredGreen
                        entry.effectiveAvailable > 0 -> PartialOrange
                        else -> LowStockRed
                    }
                    ListItem(
                        headlineContent = {
                            Text(entry.materialName.ifBlank { "Matériau #${entry.materialId}" })
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    "Requis : ${entry.requiredQuantity} ${entry.materialUnit.abbreviation}" +
                                            " · Dispo : ${entry.effectiveAvailable} ${entry.materialUnit.abbreviation}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (entry.deficit > 0) {
                                    Text(
                                        "Manque : ${entry.deficit} ${entry.materialUnit.abbreviation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LowStockRed,
                                    )
                                }
                                if (entry.notes != null) {
                                    Text(
                                        entry.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
                            Row {
                                IconButton(onClick = { editingEntry = entry }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifier")
                                }
                                IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Documents section header ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Documents", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { /* TODO: document picker */ }, enabled = false) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter un document")
                    }
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Bientôt disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }

    // ── Add BOM entry dialog ───────────────────────────────────────────────────
    if (showAddDialog) {
        AddBomEntryDialog(
            materials = state.allMaterials,
            existingIds = project?.bomEntries?.map { it.materialId }?.toSet() ?: emptySet(),
            onConfirm = { materialId, qty ->
                viewModel.addEntry(materialId, qty)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    // ── Edit BOM entry bottom sheet ────────────────────────────────────────────
    val entry = editingEntry
    if (entry != null) {
        EditBomEntryBottomSheet(
            entry = entry,
            onSave = { qty, notes -> viewModel.updateEntry(entry, qty, notes) },
            onDismiss = { editingEntry = null },
        )
    }
}

// ── Add dialog (unchanged from BomEditorScreen) ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedMaterial?.name ?: "Choisir…",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Matériau") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
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
                        label = {
                            Text("Quantité (${selectedMaterial?.unit?.abbreviation ?: ""})")
                        },
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
                enabled = selectedMaterial != null && available.isNotEmpty(),
            ) { Text("Ajouter") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// ── Edit bottom sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBomEntryBottomSheet(
    entry: BomEntry,
    onSave: (quantity: Double, notes: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var quantity by remember(entry.id) { mutableStateOf(entry.requiredQuantity.toString()) }
    var notes by remember(entry.id) { mutableStateOf(entry.notes ?: "") }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                entry.materialName.ifBlank { "Matériau #${entry.materialId}" },
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantité (${entry.materialUnit.abbreviation})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: return@Button
                    onSave(qty, notes.ifBlank { null })
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enregistrer")
            }
        }
    }
}
