package com.stashbase.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stashbase.domain.model.MaterialType
import com.stashbase.domain.model.MaterialUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMaterialScreen(
    materialId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditMaterialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (materialId == null) "Nouveau matériau" else "Modifier le matériau") },
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
                supportingText = if (state.nameError) ({ Text("Le nom est requis") }) else null,
                modifier = Modifier.fillMaxWidth(),
            )

            // Type dropdown
            EnumDropdown(
                label = "Type",
                options = MaterialType.entries,
                selected = state.type,
                labelOf = { it.label },
                onSelect = { viewModel.update { copy(type = it) } }
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.update { copy(quantity = it) } },
                    label = { Text("Quantité") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                EnumDropdown(
                    label = "Unité",
                    options = MaterialUnit.entries,
                    selected = state.unit,
                    labelOf = { it.abbreviation },
                    onSelect = { viewModel.update { copy(unit = it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.brand,
                onValueChange = { viewModel.update { copy(brand = it) } },
                label = { Text("Marque / Fabricant") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.color,
                    onValueChange = { viewModel.update { copy(color = it) } },
                    label = { Text("Couleur") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.colorCode,
                    onValueChange = { viewModel.update { copy(colorCode = it) } },
                    label = { Text("Code couleur") },
                    modifier = Modifier.weight(1f),
                )
            }

            // Location dropdown
            if (state.locations.isNotEmpty()) {
                var locationExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = it }
                ) {
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
                value = state.purchasePrice,
                onValueChange = { viewModel.update { copy(purchasePrice = it) } },
                label = { Text("Prix d'achat (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.lowStockThreshold,
                onValueChange = { viewModel.update { copy(lowStockThreshold = it) } },
                label = { Text("Seuil alerte stock bas") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.tags,
                onValueChange = { viewModel.update { copy(tags = it) } },
                label = { Text("Tags (séparés par des virgules)") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
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
