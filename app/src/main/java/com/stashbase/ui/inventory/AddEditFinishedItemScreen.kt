package com.stashbase.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stashbase.domain.model.FinishedItemStatus
import com.stashbase.domain.model.MaterialUnit
import com.stashbase.ui.components.PhotoPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFinishedItemScreen(
    itemId: Long?,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {},
    viewModel: AddEditFinishedItemViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onDeleted() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette réalisation ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "Nouvelle réalisation" else "Modifier la réalisation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (itemId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
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

            // Run selector (optional)
            if (state.runs.isNotEmpty()) {
                var runExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = runExpanded,
                    onExpandedChange = { runExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.runs.find { it.id == state.runId }?.name ?: "Aucun projet lié",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Projet lié (optionnel)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(runExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = runExpanded,
                        onDismissRequest = { runExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aucun") },
                            onClick = {
                                viewModel.update { copy(runId = null) }
                                runExpanded = false
                            }
                        )
                        state.runs.forEach { run ->
                            DropdownMenuItem(
                                text = { Text(run.name) },
                                onClick = {
                                    viewModel.update { copy(runId = run.id) }
                                    runExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.update { copy(quantity = it) } },
                    label = { Text("Quantité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                EnumDropdownFinished(
                    label = "Unité",
                    options = MaterialUnit.entries,
                    selected = state.unit,
                    labelOf = { it.abbreviation },
                    onSelect = { viewModel.update { copy(unit = it) } },
                    modifier = Modifier.weight(1f),
                )
            }

            EnumDropdownFinished(
                label = "Statut",
                options = FinishedItemStatus.entries,
                selected = state.status,
                labelOf = { it.label },
                onSelect = { viewModel.update { copy(status = it) } },
            )

            if (state.status == FinishedItemStatus.FOR_SALE || state.status == FinishedItemStatus.SOLD) {
                OutlinedTextField(
                    value = state.sellingPrice,
                    onValueChange = { viewModel.update { copy(sellingPrice = it) } },
                    label = { Text("Prix de vente (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdownFinished(
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
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
