package com.stashbase.ui.runs

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.MaterialRepository
import com.stashbase.domain.repository.RunRepository
import com.stashbase.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunDetailUiState(
    val run: Run? = null,
    val materials: List<Material> = emptyList(),
    val isLoading: Boolean = true,
)

data class RunStepNode(val step: RunStep, val depth: Int)

fun buildRunStepTree(steps: List<RunStep>, parentId: Long? = null, depth: Int = 0): List<RunStepNode> =
    steps.filter { it.parentStepId == parentId }
        .sortedBy { it.stepOrder }
        .flatMap { step -> listOf(RunStepNode(step, depth)) + buildRunStepTree(steps, step.id, depth + 1) }

@HiltViewModel
class RunDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val runRepository: RunRepository,
    private val materialRepository: MaterialRepository,
    private val shoppingListRepository: ShoppingListRepository,
) : ViewModel() {

    private val runId: Long = savedStateHandle["runId"]!!

    private val _state = MutableStateFlow(RunDetailUiState())
    val uiState: StateFlow<RunDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runRepository.getById(runId).collect { run ->
                _state.update { it.copy(run = run, isLoading = false) }
            }
        }
        viewModelScope.launch {
            materialRepository.getAll().collect { mats ->
                _state.update { it.copy(materials = mats) }
            }
        }
    }

    fun toggleStep(stepId: Long, isCompleted: Boolean) =
        viewModelScope.launch { runRepository.toggleStep(stepId, isCompleted, null) }

    // BOM
    fun upsertBomEntry(id: Long, materialId: Long, plannedQty: Double, notes: String?) =
        viewModelScope.launch {
            runRepository.upsertBomEntry(
                RunBomEntry(
                    id = id,
                    runId = runId,
                    originalMaterialId = materialId,
                    actualMaterialId = materialId,
                    plannedQuantity = plannedQty,
                    notes = notes?.ifBlank { null },
                )
            )
        }

    fun deleteBomEntry(id: Long) = viewModelScope.launch { runRepository.deleteBomEntry(id) }

    // Steps
    fun addStep(parentStepId: Long?, title: String, description: String?) = viewModelScope.launch {
        val steps = _state.value.run?.steps ?: emptyList()
        val siblings = steps.filter { it.parentStepId == parentStepId }
        val nextOrder = (siblings.maxOfOrNull { it.stepOrder } ?: 0) + 1
        runRepository.upsertStep(
            RunStep(
                runId = runId,
                parentStepId = parentStepId,
                stepOrder = nextOrder,
                title = title.trim(),
                description = description?.ifBlank { null },
            )
        )
    }

    fun editStep(step: RunStep, title: String, description: String?) = viewModelScope.launch {
        runRepository.upsertStep(step.copy(title = title.trim(), description = description?.ifBlank { null }))
    }

    fun deleteStep(id: Long) = viewModelScope.launch { runRepository.deleteStep(id) }

    fun moveStepUp(step: RunStep) = viewModelScope.launch {
        val steps = _state.value.run?.steps ?: return@launch
        val siblings = steps.filter { it.parentStepId == step.parentStepId }.sortedBy { it.stepOrder }
        val idx = siblings.indexOfFirst { it.id == step.id }
        if (idx <= 0) return@launch
        val above = siblings[idx - 1]
        runRepository.upsertStep(step.copy(stepOrder = above.stepOrder))
        runRepository.upsertStep(above.copy(stepOrder = step.stepOrder))
    }

    fun moveStepDown(step: RunStep) = viewModelScope.launch {
        val steps = _state.value.run?.steps ?: return@launch
        val siblings = steps.filter { it.parentStepId == step.parentStepId }.sortedBy { it.stepOrder }
        val idx = siblings.indexOfFirst { it.id == step.id }
        if (idx >= siblings.size - 1) return@launch
        val below = siblings[idx + 1]
        runRepository.upsertStep(step.copy(stepOrder = below.stepOrder))
        runRepository.upsertStep(below.copy(stepOrder = step.stepOrder))
    }

    fun completeRun(consumptions: Map<Long, Double>) =
        viewModelScope.launch { runRepository.completeRun(runId, consumptions) }

    fun generateShoppingList() =
        viewModelScope.launch { shoppingListRepository.generateFromRunDeficits(runId) }
}

private sealed class RunStepSheetMode {
    data class Add(val parentStepId: Long?) : RunStepSheetMode()
    data class Edit(val step: RunStep) : RunStepSheetMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: RunDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val run = state.run

    var showCompletionSheet by remember { mutableStateOf(false) }
    var showBomSheet by remember { mutableStateOf(false) }
    var editingBomEntry by remember { mutableStateOf<RunBomEntry?>(null) }
    var stepSheetMode by remember { mutableStateOf<RunStepSheetMode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(run?.name ?: "Projet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    run?.let {
                        IconButton(onClick = { viewModel.generateShoppingList() }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Générer les courses")
                        }
                        IconButton(onClick = { onEdit(it.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (run != null && run.status != RunStatus.COMPLETED) {
                ExtendedFloatingActionButton(
                    onClick = { showCompletionSheet = true },
                    icon = { Icon(Icons.Default.Check, null) },
                    text = { Text("Terminer le projet") },
                )
            }
        },
    ) { padding ->
        if (state.isLoading || run == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val stepNodes = remember(run.steps) { buildRunStepTree(run.steps) }
        val totalLeafSteps = stepNodes.count { node -> run.steps.none { it.parentStepId == node.step.id } }
        val completedLeafSteps = stepNodes.count { node ->
            node.step.isCompleted && run.steps.none { it.parentStepId == node.step.id }
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Run info
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(onClick = {}, label = { Text(run.status.label) })
                    if (run.blueprintName.isNotBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Modèle : ${run.blueprintName}") },
                            leadingIcon = { Icon(Icons.Default.AutoStories, null, Modifier.size(16.dp)) },
                        )
                    }
                }
                if (!run.notes.isNullOrBlank()) {
                    Text(
                        run.notes,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }

            // ── BOM Section ────────────────────────────────────────────────────
            item {
                RunSectionHeader(
                    title = "Matières",
                    onAdd = { editingBomEntry = null; showBomSheet = true },
                )
            }

            if (run.bomEntries.isEmpty()) {
                item { RunEmptyHint("Aucune matière") }
            } else {
                items(run.bomEntries, key = { "bom_${it.id}" }) { entry ->
                    RunBomEntryRow(
                        entry = entry,
                        onEdit = { editingBomEntry = entry; showBomSheet = true },
                        onDelete = { viewModel.deleteBomEntry(entry.id) },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Steps Section ──────────────────────────────────────────────────
            item {
                RunSectionHeader(
                    title = if (totalLeafSteps > 0) "Étapes — $completedLeafSteps/$totalLeafSteps" else "Étapes",
                    onAdd = { stepSheetMode = RunStepSheetMode.Add(parentStepId = null) },
                )
            }

            if (stepNodes.isEmpty()) {
                item { RunEmptyHint("Aucune étape") }
            } else {
                items(stepNodes, key = { "step_${it.step.id}" }) { node ->
                    RunStepRow(
                        node = node,
                        allSteps = run.steps,
                        onToggle = { viewModel.toggleStep(node.step.id, !node.step.isCompleted) },
                        onAddSibling = { stepSheetMode = RunStepSheetMode.Add(parentStepId = node.step.parentStepId) },
                        onAddChild = { stepSheetMode = RunStepSheetMode.Add(parentStepId = node.step.id) },
                        onEdit = { stepSheetMode = RunStepSheetMode.Edit(node.step) },
                        onDelete = { viewModel.deleteStep(node.step.id) },
                        onMoveUp = { viewModel.moveStepUp(node.step) },
                        onMoveDown = { viewModel.moveStepDown(node.step) },
                    )
                    HorizontalDivider(Modifier.padding(start = (16 + node.depth * 16).dp, end = 16.dp))
                }
            }
        }
    }

    // Completion sheet
    if (showCompletionSheet && run != null) {
        RecapitulatifSheet(
            run = run,
            onDismiss = { showCompletionSheet = false },
            onConfirm = { consumptions ->
                viewModel.completeRun(consumptions)
                showCompletionSheet = false
                onBack()
            },
        )
    }

    // BOM sheet
    if (showBomSheet) {
        RunBomEntrySheet(
            entry = editingBomEntry,
            materials = state.materials,
            onDismiss = { showBomSheet = false },
            onConfirm = { materialId, qty, notes ->
                viewModel.upsertBomEntry(
                    id = editingBomEntry?.id ?: 0L,
                    materialId = materialId,
                    plannedQty = qty,
                    notes = notes,
                )
                showBomSheet = false
            },
        )
    }

    // Step sheet
    stepSheetMode?.let { mode ->
        RunStepEditSheet(
            initialTitle = if (mode is RunStepSheetMode.Edit) mode.step.title else "",
            initialDescription = if (mode is RunStepSheetMode.Edit) mode.step.description ?: "" else "",
            sheetTitle = when (mode) {
                is RunStepSheetMode.Add -> if (mode.parentStepId == null) "Nouvelle étape" else "Nouvelle sous-étape"
                is RunStepSheetMode.Edit -> "Modifier l'étape"
            },
            onDismiss = { stepSheetMode = null },
            onConfirm = { title, desc ->
                when (mode) {
                    is RunStepSheetMode.Add -> viewModel.addStep(mode.parentStepId, title, desc)
                    is RunStepSheetMode.Edit -> viewModel.editStep(mode.step, title, desc)
                }
                stepSheetMode = null
            },
        )
    }
}

@Composable
private fun RunSectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Ajouter") }
    }
}

@Composable
private fun RunEmptyHint(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun RunBomEntryRow(
    entry: RunBomEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(entry.materialName) },
        supportingContent = {
            if (entry.deficit > 0)
                Text("Manque ${"%.2f".format(entry.deficit)} ${entry.materialUnit.abbreviation} (dispo : ${"%.2f".format(entry.effectiveAvailable)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            else
                Text("Couvert — ${"%.2f".format(entry.effectiveAvailable)} dispo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${"%.2f".format(entry.plannedQuantity)} ${entry.materialUnit.abbreviation}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, "Modifier", Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "Supprimer", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
            }
        },
    )
}

@Composable
private fun RunStepRow(
    node: RunStepNode,
    allSteps: List<RunStep>,
    onToggle: () -> Unit,
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
    val hasChildren = allSteps.any { it.parentStepId == node.step.id }

    var showAddMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + node.depth * 16).dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.depth > 0) {
            Icon(Icons.Default.SubdirectoryArrowRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(4.dp))
        }

        // Checkbox only for leaf steps (no children), or for any step
        if (!hasChildren) {
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (node.step.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (node.step.isCompleted) "Marquer incomplet" else "Marquer terminé",
                    tint = if (node.step.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.size(32.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                node.step.title,
                style = if (node.depth == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                textDecoration = if (node.step.isCompleted && !hasChildren) TextDecoration.LineThrough else null,
                color = if (node.step.isCompleted && !hasChildren) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
            if (!node.step.description.isNullOrBlank()) {
                Text(
                    node.step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecapitulatifSheet(
    run: Run,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Double>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantities by remember {
        mutableStateOf(
            run.bomEntries.associate { it.id to (it.actualQuantity ?: it.plannedQuantity).toString() }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Récapitulatif de fin de projet", style = MaterialTheme.typography.titleMedium)
            Text("Indiquez les quantités réellement consommées.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()

            run.bomEntries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.materialName, style = MaterialTheme.typography.bodyMedium)
                        Text("Prévu : ${"%.2f".format(entry.plannedQuantity)} ${entry.materialUnit.abbreviation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = quantities[entry.id] ?: "",
                        onValueChange = { quantities = quantities + (entry.id to it) },
                        label = { Text(entry.materialUnit.abbreviation) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                    )
                }
            }

            if (run.bomEntries.isEmpty()) {
                Text("Aucune matière à confirmer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            HorizontalDivider()

            Button(
                onClick = {
                    val consumptions = quantities
                        .mapValues { (_, v) -> v.toDoubleOrNull() ?: 0.0 }
                        .filter { (id, _) -> run.bomEntries.any { it.id == id } }
                    onConfirm(consumptions)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Confirmer la fin du projet") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunBomEntrySheet(
    entry: RunBomEntry?,
    materials: List<Material>,
    onDismiss: () -> Unit,
    onConfirm: (materialId: Long, qty: Double, notes: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMaterialId by remember { mutableStateOf(entry?.actualMaterialId ?: materials.firstOrNull()?.id ?: 0L) }
    var qty by remember { mutableStateOf(entry?.plannedQuantity?.toString() ?: "") }
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
                label = { Text("Quantité prévue *") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunStepEditSheet(
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
