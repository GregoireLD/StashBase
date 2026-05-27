package com.stashbase.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.MaterialRepository
import com.stashbase.domain.repository.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: ShoppingListRepository,
    private val materialRepository: MaterialRepository,
) : ViewModel() {

    val items: StateFlow<List<ShoppingListItem>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val materials: StateFlow<List<Material>> = materialRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setChecked(id: Long, checked: Boolean) = viewModelScope.launch { repository.setChecked(id, checked) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
    fun deleteChecked() = viewModelScope.launch { repository.deleteChecked() }
    fun validateCheckedItems() = viewModelScope.launch { repository.validateCheckedItems() }

    fun addItem(name: String, quantity: Double? = null) = viewModelScope.launch {
        repository.upsert(ShoppingListItem(name = name, quantity = quantity))
    }

    fun validateItemToMaterial(item: ShoppingListItem, materialId: Long, quantity: Double) =
        viewModelScope.launch { repository.validateItemToMaterial(item.id, materialId, quantity) }

    fun validateItemAsNewMaterial(item: ShoppingListItem, type: MaterialType, quantity: Double) =
        viewModelScope.launch { repository.validateItemAsNewMaterial(item, type, quantity) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onNavigateToRun: (Long) -> Unit = {},
    viewModel: ShoppingListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToValidate by remember { mutableStateOf<ShoppingListItem?>(null) }

    val checkedCount = items.count { it.isChecked }
    val linkedCheckedCount = items.count { it.isChecked && it.sourceMaterialId != null }

    val runGroups = remember(items) {
        items
            .filter { it.sourceRunId != null }
            .groupBy { it.sourceRunId!! }
            .entries
            .sortedBy { (_, v) -> v.first().sourceRunName ?: "" }
    }
    val manualItems = remember(items) {
        items.filter { it.sourceRunId == null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liste de courses") },
                actions = {
                    if (linkedCheckedCount > 0) {
                        TextButton(onClick = viewModel::validateCheckedItems) {
                            Text("Valider ($linkedCheckedCount)")
                        }
                    }
                    if (checkedCount > 0) {
                        TextButton(onClick = viewModel::deleteChecked) {
                            Text("Effacer ($checkedCount)")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un article")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Text("Liste vide", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            runGroups.forEach { (runId, groupItems) ->
                val runName = groupItems.first().sourceRunName ?: "Projet #$runId"
                item(key = "header_$runId") {
                    RunGroupHeader(runName = runName, onClick = { onNavigateToRun(runId) })
                }
                items(groupItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onChecked = { viewModel.setChecked(item.id, it) },
                        onDelete = { viewModel.delete(item.id) },
                        onValidate = { itemToValidate = item },
                        showRunName = false,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }

            if (manualItems.isNotEmpty()) {
                item(key = "header_manual") {
                    RunGroupHeader(runName = "Courses libres", onClick = null)
                }
                items(manualItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onChecked = { viewModel.setChecked(item.id, it) },
                        onDelete = { viewModel.delete(item.id) },
                        onValidate = { itemToValidate = item },
                        showRunName = false,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddShoppingItemDialog(
            onConfirm = { name -> viewModel.addItem(name); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }

    itemToValidate?.let { item ->
        if (item.sourceMaterialId != null) {
            ValidateLinkedItemDialog(
                item = item,
                onConfirm = { qty ->
                    viewModel.validateItemToMaterial(item, item.sourceMaterialId, qty)
                    itemToValidate = null
                },
                onDismiss = { itemToValidate = null },
            )
        } else {
            ValidateFreeItemDialog(
                item = item,
                materials = materials,
                onConfirmExisting = { materialId, qty ->
                    viewModel.validateItemToMaterial(item, materialId, qty)
                    itemToValidate = null
                },
                onConfirmNew = { type, qty ->
                    viewModel.validateItemAsNewMaterial(item, type, qty)
                    itemToValidate = null
                },
                onDismiss = { itemToValidate = null },
            )
        }
    }
}

@Composable
private fun RunGroupHeader(runName: String, onClick: (() -> Unit)?) {
    val modifier = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
    else
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = runName,
            style = MaterialTheme.typography.titleSmall,
            color = if (onClick != null)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Aller au projet",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    onChecked: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onValidate: () -> Unit,
    showRunName: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                if (item.quantity != null && item.unit != null) {
                    Text(
                        "${item.quantity.toDisplayString()} ${item.unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (showRunName && item.sourceRunName != null) {
                    Text(
                        item.sourceRunName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        leadingContent = {
            Checkbox(checked = item.isChecked, onCheckedChange = onChecked)
        },
        trailingContent = {
            Row {
                if (item.isChecked) {
                    IconButton(onClick = onValidate) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = "Valider la réception",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    )
}

@Composable
private fun ValidateLinkedItemDialog(
    item: ShoppingListItem,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var quantityStr by remember {
        mutableStateOf(item.quantity?.toDisplayString() ?: "1")
    }
    val quantity = quantityStr.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réceptionner l'article") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = {
                        Text("Quantité reçue${item.unit?.let { " (${it.abbreviation})" } ?: ""}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { quantity?.let(onConfirm) },
                enabled = quantity != null && quantity > 0,
            ) { Text("Valider → Inventaire") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private enum class FreeValidateStep { SEARCH, CONFIRM_EXISTING, CREATE_NEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValidateFreeItemDialog(
    item: ShoppingListItem,
    materials: List<Material>,
    onConfirmExisting: (materialId: Long, quantity: Double) -> Unit,
    onConfirmNew: (type: MaterialType, quantity: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(FreeValidateStep.SEARCH) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMaterial by remember { mutableStateOf<Material?>(null) }
    var quantityStr by remember { mutableStateOf(item.quantity?.toDisplayString() ?: "1") }
    var selectedType by remember { mutableStateOf(MaterialType.OTHER) }
    var typeExpanded by remember { mutableStateOf(false) }

    val filteredMaterials = remember(searchQuery, materials) {
        if (searchQuery.isBlank()) materials.take(20)
        else materials.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(20)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    FreeValidateStep.SEARCH -> "Ajouter à l'inventaire"
                    FreeValidateStep.CONFIRM_EXISTING -> "Réceptionner"
                    FreeValidateStep.CREATE_NEW -> "Nouveau matériau"
                }
            )
        },
        text = {
            when (step) {
                FreeValidateStep.SEARCH -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Lier «${item.name}» à un matériau existant :",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Rechercher") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (filteredMaterials.isEmpty()) {
                                Text(
                                    "Aucun matériau trouvé",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(8.dp),
                                )
                            } else {
                                filteredMaterials.forEach { mat ->
                                    ListItem(
                                        headlineContent = { Text(mat.name, maxLines = 1) },
                                        supportingContent = {
                                            Text(
                                                "${mat.quantity.toDisplayString()} ${mat.unit.abbreviation}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            selectedMaterial = mat
                                            quantityStr = item.quantity?.toDisplayString() ?: "1"
                                            step = FreeValidateStep.CONFIRM_EXISTING
                                        },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                FreeValidateStep.CONFIRM_EXISTING -> {
                    val mat = selectedMaterial!!
                    val qty = quantityStr.replace(',', '.').toDoubleOrNull()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Ajouter à «${mat.name}»\nStock actuel : ${mat.quantity.toDisplayString()} ${mat.unit.abbreviation}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Quantité reçue (${mat.unit.abbreviation})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                FreeValidateStep.CREATE_NEW -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedType.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type de matériau") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false },
                            ) {
                                MaterialType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = { selectedType = type; typeExpanded = false },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = {
                                Text("Quantité${item.unit?.let { " (${it.abbreviation})" } ?: ""}")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                FreeValidateStep.SEARCH -> {
                    TextButton(onClick = {
                        quantityStr = item.quantity?.toDisplayString() ?: "1"
                        step = FreeValidateStep.CREATE_NEW
                    }) {
                        Text("Créer un nouveau matériau")
                    }
                }
                FreeValidateStep.CONFIRM_EXISTING -> {
                    val qty = quantityStr.replace(',', '.').toDoubleOrNull()
                    TextButton(
                        onClick = { qty?.let { onConfirmExisting(selectedMaterial!!.id, it) } },
                        enabled = qty != null && qty > 0,
                    ) { Text("Valider → Inventaire") }
                }
                FreeValidateStep.CREATE_NEW -> {
                    val qty = quantityStr.replace(',', '.').toDoubleOrNull()
                    TextButton(
                        onClick = { qty?.let { onConfirmNew(selectedType, it) } },
                        enabled = qty != null && qty > 0,
                    ) { Text("Créer dans l'inventaire") }
                }
            }
        },
        dismissButton = {
            when (step) {
                FreeValidateStep.SEARCH -> TextButton(onClick = onDismiss) { Text("Annuler") }
                else -> TextButton(onClick = { step = FreeValidateStep.SEARCH }) { Text("Retour") }
            }
        }
    )
}

@Composable
private fun AddShoppingItemDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un article") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom de l'article") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun Double.toDisplayString(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()
