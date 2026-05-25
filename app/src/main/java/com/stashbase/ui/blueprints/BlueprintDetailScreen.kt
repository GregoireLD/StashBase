package com.stashbase.ui.blueprints

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.stashbase.domain.repository.BlueprintRepository
import com.stashbase.domain.repository.MaterialRepository
import com.stashbase.domain.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlueprintDetailUiState(
    val blueprint: Blueprint? = null,
    val materials: List<Material> = emptyList(),
    val isLoading: Boolean = true,
    val createdRunId: Long? = null,
)

// Helper to flatten blueprint steps into a depth-annotated list for display
data class StepNode(val step: BlueprintStep, val depth: Int)

fun buildStepTree(steps: List<BlueprintStep>, parentId: Long? = null, depth: Int = 0): List<StepNode> =
    steps.filter { it.parentStepId == parentId }
        .sortedBy { it.stepOrder }
        .flatMap { step -> listOf(StepNode(step, depth)) + buildStepTree(steps, step.id, depth + 1) }

@HiltViewModel
class BlueprintDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val blueprintRepository: BlueprintRepository,
    private val materialRepository: MaterialRepository,
    private val runRepository: RunRepository,
) : ViewModel() {

    private val blueprintId: Long = savedStateHandle["blueprintId"]!!

    private val _state = MutableStateFlow(BlueprintDetailUiState())
    val uiState: StateFlow<BlueprintDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            blueprintRepository.getById(blueprintId).collect { bp ->
                _state.update { it.copy(blueprint = bp, isLoading = false) }
            }
        }
        viewModelScope.launch {
            materialRepository.getAll().collect { mats ->
                _state.update { it.copy(materials = mats) }
            }
        }
    }

    // BOM entry
    fun upsertBomEntry(id: Long, materialId: Long, requiredQty: Double, notes: String?) =
        viewModelScope.launch {
            blueprintRepository.upsertBomEntry(
                BlueprintBomEntry(
                    id = id,
                    blueprintId = blueprintId,
                    materialId = materialId,
                    requiredQuantity = requiredQty,
                    notes = notes?.ifBlank { null },
                )
            )
        }

    fun deleteBomEntry(id: Long) = viewModelScope.launch { blueprintRepository.deleteBomEntry(id) }

    // Steps
    fun addStep(parentStepId: Long?, title: String, description: String?) = viewModelScope.launch {
        val steps = _state.value.blueprint?.steps ?: emptyList()
        val siblings = steps.filter { it.parentStepId == parentStepId }
        val nextOrder = (siblings.maxOfOrNull { it.stepOrder } ?: 0) + 1
        blueprintRepository.upsertStep(
            BlueprintStep(
                blueprintId = blueprintId,
                parentStepId = parentStepId,
                stepOrder = nextOrder,
                title = title.trim(),
                description = description?.ifBlank { null },
            )
        )
    }

    fun editStep(step: BlueprintStep, title: String, description: String?) = viewModelScope.launch {
        blueprintRepository.upsertStep(
            step.copy(title = title.trim(), description = description?.ifBlank { null })
        )
    }

    fun deleteStep(id: Long) = viewModelScope.launch { blueprintRepository.deleteStep(id) }

    fun moveStepUp(step: BlueprintStep) = viewModelScope.launch {
        val steps = _state.value.blueprint?.steps ?: return@launch
        val siblings = steps.filter { it.parentStepId == step.parentStepId }.sortedBy { it.stepOrder }
        val idx = siblings.indexOfFirst { it.id == step.id }
        if (idx <= 0) return@launch
        val above = siblings[idx - 1]
        blueprintRepository.upsertStep(step.copy(stepOrder = above.stepOrder))
        blueprintRepository.upsertStep(above.copy(stepOrder = step.stepOrder))
    }

    fun moveStepDown(step: BlueprintStep) = viewModelScope.launch {
        val steps = _state.value.blueprint?.steps ?: return@launch
        val siblings = steps.filter { it.parentStepId == step.parentStepId }.sortedBy { it.stepOrder }
        val idx = siblings.indexOfFirst { it.id == step.id }
        if (idx >= siblings.size - 1) return@launch
        val below = siblings[idx + 1]
        blueprintRepository.upsertStep(step.copy(stepOrder = below.stepOrder))
        blueprintRepository.upsertStep(below.copy(stepOrder = step.stepOrder))
    }

    // Create run from blueprint
    fun createRun(name: String) = viewModelScope.launch {
        val bp = _state.value.blueprint ?: return@launch
        val runId = runRepository.upsert(Run(blueprintId = bp.id, name = name.trim()))
        bp.bomEntries.forEach { bom ->
            runRepository.upsertBomEntry(
                RunBomEntry(
                    runId = runId,
                    originalMaterialId = bom.materialId,
                    actualMaterialId = bom.materialId,
                    plannedQuantity = bom.requiredQuantity,
                    notes = bom.notes,
                )
            )
        }
        bp.steps.forEach { step ->
            runRepository.upsertStep(
                RunStep(
                    runId = runId,
                    blueprintStepId = step.id,
                    parentStepId = step.parentStepId,
                    stepOrder = step.stepOrder,
                    title = step.title,
                    description = step.description,
                )
            )
        }
        _state.update { it.copy(createdRunId = runId) }
    }

    fun consumeNavigation() = _state.update { it.copy(createdRunId = null) }
}

// Represents the pending step add/edit action driving the bottom sheet
private sealed class StepSheetMode {
    data class Add(val parentStepId: Long?) : StepSheetMode()
    data class Edit(val step: BlueprintStep) : StepSheetMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onNavigateToRun: (Long) -> Unit,
    viewModel: BlueprintDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val blueprint = state.blueprint

    LaunchedEffect(state.createdRunId) {
        state.createdRunId?.let { onNavigateToRun(it); viewModel.consumeNavigation() }
    }

    var showBomSheet by remember { mutableStateOf(false) }
    var editingBomEntry by remember { mutableStateOf<BlueprintBomEntry?>(null) }
    var stepSheetMode by remember { mutableStateOf<StepSheetMode?>(null) }
    var showStartRunDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(blueprint?.name ?: "Modèle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    blueprint?.let {
                        IconButton(onClick = { onEdit(it.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (blueprint != null) {
                ExtendedFloatingActionButton(
                    onClick = { showStartRunDialog = true },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("Lancer un projet") },
                )
            }
        },
    ) { padding ->
        if (state.isLoading || blueprint == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val stepNodes = remember(blueprint.steps) { buildStepTree(blueprint.steps) }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Info section
            if (!blueprint.category.isNullOrBlank() || !blueprint.description.isNullOrBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        if (!blueprint.category.isNullOrBlank()) {
                            AssistChip(onClick = {}, label = { Text(blueprint.category) })
                            Spacer(Modifier.height(4.dp))
                        }
                        if (!blueprint.description.isNullOrBlank()) {
                            Text(
                                blueprint.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }

            if (!blueprint.notes.isNullOrBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(2.dp))
                        Text(blueprint.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }

            // ── BOM Section ────────────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Matières nécessaires",
                    onAdd = { editingBomEntry = null; showBomSheet = true },
                )
            }

            if (blueprint.bomEntries.isEmpty()) {
                item { EmptyHint("Aucune matière") }
            } else {
                items(blueprint.bomEntries, key = { "bom_${it.id}" }) { entry ->
                    BomEntryRow(
                        entry = entry,
                        onEdit = { editingBomEntry = entry; showBomSheet = true },
                        onDelete = { viewModel.deleteBomEntry(entry.id) },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Steps Section ──────────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Étapes",
                    onAdd = { stepSheetMode = StepSheetMode.Add(parentStepId = null) },
                )
            }

            if (stepNodes.isEmpty()) {
                item { EmptyHint("Aucune étape") }
            } else {
                items(stepNodes, key = { "step_${it.step.id}" }) { node ->
                    BlueprintStepRow(
                        node = node,
                        allSteps = blueprint.steps,
                        onAddSibling = { stepSheetMode = StepSheetMode.Add(parentStepId = node.step.parentStepId) },
                        onAddChild = { stepSheetMode = StepSheetMode.Add(parentStepId = node.step.id) },
                        onEdit = { stepSheetMode = StepSheetMode.Edit(node.step) },
                        onDelete = { viewModel.deleteStep(node.step.id) },
                        onMoveUp = { viewModel.moveStepUp(node.step) },
                        onMoveDown = { viewModel.moveStepDown(node.step) },
                    )
                    HorizontalDivider(Modifier.padding(start = (16 + node.depth * 16).dp, end = 16.dp))
                }
            }
        }
    }

    // BOM sheet
    if (showBomSheet) {
        BomEntrySheet(
            entry = editingBomEntry,
            materials = state.materials,
            onDismiss = { showBomSheet = false },
            onConfirm = { materialId, qty, notes ->
                viewModel.upsertBomEntry(
                    id = editingBomEntry?.id ?: 0L,
                    materialId = materialId,
                    requiredQty = qty,
                    notes = notes,
                )
                showBomSheet = false
            },
        )
    }

    // Step sheet (add or edit)
    stepSheetMode?.let { mode ->
        StepEditSheet(
            initialTitle = if (mode is StepSheetMode.Edit) mode.step.title else "",
            initialDescription = if (mode is StepSheetMode.Edit) mode.step.description ?: "" else "",
            sheetTitle = when (mode) {
                is StepSheetMode.Add -> if (mode.parentStepId == null) "Nouvelle étape" else "Nouvelle sous-étape"
                is StepSheetMode.Edit -> "Modifier l'étape"
            },
            onDismiss = { stepSheetMode = null },
            onConfirm = { title, desc ->
                when (mode) {
                    is StepSheetMode.Add -> viewModel.addStep(mode.parentStepId, title, desc)
                    is StepSheetMode.Edit -> viewModel.editStep(mode.step, title, desc)
                }
                stepSheetMode = null
            },
        )
    }

    // Start run dialog
    if (showStartRunDialog) {
        StartRunDialog(
            defaultName = blueprint?.name ?: "",
            onDismiss = { showStartRunDialog = false },
            onConfirm = { name -> viewModel.createRun(name); showStartRunDialog = false },
        )
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Ajouter") }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun BomEntryRow(
    entry: BlueprintBomEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(entry.materialName) },
        supportingContent = {
            if (entry.deficit > 0)
                Text("Manque ${entry.deficit} ${entry.materialUnit.abbreviation}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            else
                Text("Couvert", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${entry.requiredQuantity} ${entry.materialUnit.abbreviation}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, "Modifier", Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "Supprimer", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
            }
        },
    )
}

@Composable
private fun BlueprintStepRow(
    node: StepNode,
    allSteps: List<BlueprintStep>,
    onAddSibling: () -> Unit,
    onAddChild: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val siblings = allSteps.filter { it.parentStepId == node.step.parentStepId }.sortedBy { it.stepOrder }
    val isFirst = siblings.firstOrNull()?.id == node.step.id
    val isLast = siblings.lastOrNull()?.id == node.step.id

    var showAddMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + node.depth * 16).dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Indent indicator
        if (node.depth > 0) {
            Icon(Icons.Default.SubdirectoryArrowRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(4.dp))
        }

        Text(
            node.step.title,
            modifier = Modifier.weight(1f),
            style = if (node.depth == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
        )

        // + button with dropdown
        Box {
            IconButton(onClick = { showAddMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "Ajouter", Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Étape suivante") },
                    onClick = { showAddMenu = false; onAddSibling() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp)) },
                )
                DropdownMenuItem(
                    text = { Text("Sous-étape") },
                    onClick = { showAddMenu = false; onAddChild() },
                    leadingIcon = { Icon(Icons.Default.SubdirectoryArrowRight, null, Modifier.size(16.dp)) },
                )
            }
        }
        IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp), enabled = !isFirst) {
            Icon(Icons.Default.KeyboardArrowUp, "Monter", Modifier.size(16.dp))
        }
        IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp), enabled = !isLast) {
            Icon(Icons.Default.KeyboardArrowDown, "Descendre", Modifier.size(16.dp))
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, "Modifier", Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, "Supprimer", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
    if (!node.step.description.isNullOrBlank()) {
        Text(
            node.step.description,
            modifier = Modifier.padding(start = (16 + node.depth * 16 + if (node.depth > 0) 18 else 0).dp, end = 16.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepEditSheet(
    initialTitle: String,
    initialDescription: String,
    sheetTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(sheetTitle, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre *") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, description.ifBlank { null }) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BomEntrySheet(
    entry: BlueprintBomEntry?,
    materials: List<Material>,
    onDismiss: () -> Unit,
    onConfirm: (materialId: Long, qty: Double, notes: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMaterialId by remember { mutableStateOf(entry?.materialId ?: materials.firstOrNull()?.id ?: 0L) }
    var qty by remember { mutableStateOf(entry?.requiredQuantity?.toString() ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var materialExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (entry == null) "Ajouter une matière" else "Modifier la matière", style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(expanded = materialExpanded, onExpandedChange = { materialExpanded = it }) {
                OutlinedTextField(
                    value = materials.find { it.id == selectedMaterialId }?.name ?: "Choisir une matière",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Matière *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(materialExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                    materials.forEach { mat ->
                        DropdownMenuItem(text = { Text(mat.name) }, onClick = { selectedMaterialId = mat.id; materialExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it },
                label = { Text("Quantité requise *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            val parsedQty = qty.toDoubleOrNull()
            Button(
                onClick = { if (selectedMaterialId != 0L && parsedQty != null) onConfirm(selectedMaterialId, parsedQty, notes.ifBlank { null }) },
                enabled = selectedMaterialId != 0L && parsedQty != null && parsedQty > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }
    }
}

@Composable
private fun StartRunDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lancer un projet") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du projet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Démarrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
