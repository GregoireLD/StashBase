package com.stashbase.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── Material Detail ──────────────────────────────────────────────────────────

data class MaterialDetailUiState(
    val material: Material? = null,
    val stockQuantity: String = "",
    val stockUnit: MaterialUnit = MaterialUnit.METER,
    val stockLocationId: Long? = null,
    val locations: List<StorageLocation> = emptyList(),
)

@HiltViewModel
class MaterialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val materialRepository: MaterialRepository,
    private val locationRepository: StorageLocationRepository,
) : ViewModel() {
    private val materialId: Long = checkNotNull(savedStateHandle["materialId"])
    private val _state = MutableStateFlow(MaterialDetailUiState())
    val uiState: StateFlow<MaterialDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            materialRepository.getById(materialId).collect { m ->
                _state.update { current ->
                    if (current.material == null && m != null) {
                        current.copy(
                            material = m,
                            stockQuantity = m.quantity.toString(),
                            stockUnit = m.unit,
                            stockLocationId = m.locationId,
                        )
                    } else {
                        current.copy(material = m)
                    }
                }
            }
        }
        viewModelScope.launch {
            locationRepository.getAll().collect { locs ->
                _state.update { it.copy(locations = locs) }
            }
        }
    }

    fun setStockQuantity(v: String) = _state.update { it.copy(stockQuantity = v) }
    fun setStockUnit(v: MaterialUnit) = _state.update { it.copy(stockUnit = v) }
    fun setStockLocation(id: Long?) = _state.update { it.copy(stockLocationId = id) }

    fun saveStock() {
        val s = _state.value
        val material = s.material ?: return
        viewModelScope.launch {
            materialRepository.upsert(
                material.copy(
                    quantity = s.stockQuantity.toDoubleOrNull() ?: material.quantity,
                    unit = s.stockUnit,
                    locationId = s.stockLocationId,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: MaterialDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val material = state.material

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(material?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (material != null) {
                        IconButton(onClick = { onEdit(material.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier la fiche")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (material == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (material.photoPath != null) {
                AsyncImage(
                    model = File(material.photoPath),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            StockCard(
                quantityLabel = "Quantité en stock",
                quantity = state.stockQuantity,
                onQuantityChange = viewModel::setStockQuantity,
                unitSlot = {
                    DetailEnumDropdown(
                        label = "Unité",
                        options = MaterialUnit.entries,
                        selected = state.stockUnit,
                        labelOf = { it.abbreviation },
                        onSelect = viewModel::setStockUnit,
                        modifier = Modifier.weight(1f),
                    )
                },
                locationSlot = {
                    if (state.locations.isNotEmpty()) {
                        DetailLocationDropdown(
                            locations = state.locations,
                            selectedId = state.stockLocationId,
                            onSelect = viewModel::setStockLocation,
                        )
                    }
                },
                onSave = viewModel::saveStock,
            )

            DetailSection("Caractéristiques") {
                DetailRow("Type", material.type.label)
                if (!material.brand.isNullOrBlank()) DetailRow("Marque", material.brand)
                if (!material.color.isNullOrBlank()) {
                    val colorLabel = buildString {
                        append(material.color)
                        if (!material.colorCode.isNullOrBlank()) append(" (${material.colorCode})")
                    }
                    DetailRow("Couleur", colorLabel)
                }
                if (material.purchasePrice != null) DetailRow("Prix d'achat", "${material.purchasePrice} €")
                if (material.lowStockThreshold != null) {
                    DetailRow("Seuil alerte stock bas", "${material.lowStockThreshold} ${material.unit.abbreviation}")
                }
                if (material.tags.isNotEmpty()) DetailRow("Tags", material.tags.joinToString(", "))
            }

            if (!material.notes.isNullOrBlank()) {
                DetailSection("Notes") {
                    Text(material.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ── Tool Detail ──────────────────────────────────────────────────────────────

data class ToolDetailUiState(
    val tool: Tool? = null,
    val stockLocationId: Long? = null,
    val locations: List<StorageLocation> = emptyList(),
)

@HiltViewModel
class ToolDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val toolRepository: ToolRepository,
    private val locationRepository: StorageLocationRepository,
) : ViewModel() {
    private val toolId: Long = checkNotNull(savedStateHandle["toolId"])
    private val _state = MutableStateFlow(ToolDetailUiState())
    val uiState: StateFlow<ToolDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            toolRepository.getById(toolId).collect { t ->
                _state.update { current ->
                    if (current.tool == null && t != null) {
                        current.copy(tool = t, stockLocationId = t.locationId)
                    } else {
                        current.copy(tool = t)
                    }
                }
            }
        }
        viewModelScope.launch {
            locationRepository.getAll().collect { locs ->
                _state.update { it.copy(locations = locs) }
            }
        }
    }

    fun setStockLocation(id: Long?) = _state.update { it.copy(stockLocationId = id) }

    fun saveStock() {
        val s = _state.value
        val tool = s.tool ?: return
        viewModelScope.launch {
            toolRepository.upsert(tool.copy(locationId = s.stockLocationId))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ToolDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tool = state.tool

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (tool != null) {
                        IconButton(onClick = { onEdit(tool.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier la fiche")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (tool == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (tool.photoPath != null) {
                AsyncImage(
                    model = File(tool.photoPath),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            if (state.locations.isNotEmpty()) {
                RangementCard(
                    locations = state.locations,
                    selectedId = state.stockLocationId,
                    onSelect = viewModel::setStockLocation,
                    onSave = viewModel::saveStock,
                )
            }

            DetailSection("Caractéristiques") {
                DetailRow("Type", tool.type.label)
                DetailRow("État", tool.condition.label)
                if (!tool.brand.isNullOrBlank()) DetailRow("Marque", tool.brand)
                if (!tool.size.isNullOrBlank()) DetailRow("Taille / Spéc.", tool.size)
            }

            if (!tool.notes.isNullOrBlank()) {
                DetailSection("Notes") {
                    Text(tool.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ── FinishedItem Detail ──────────────────────────────────────────────────────

data class FinishedItemDetailUiState(
    val item: FinishedItem? = null,
    val stockQuantity: String = "",
    val stockUnit: MaterialUnit = MaterialUnit.PIECE,
)

@HiltViewModel
class FinishedItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val finishedItemRepository: FinishedItemRepository,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val _state = MutableStateFlow(FinishedItemDetailUiState())
    val uiState: StateFlow<FinishedItemDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            finishedItemRepository.getAll().collect { list ->
                val item = list.firstOrNull { it.id == itemId }
                _state.update { current ->
                    if (current.item == null && item != null) {
                        current.copy(
                            item = item,
                            stockQuantity = item.quantity.toString(),
                            stockUnit = item.unit,
                        )
                    } else {
                        current.copy(item = item)
                    }
                }
            }
        }
    }

    fun setStockQuantity(v: String) = _state.update { it.copy(stockQuantity = v) }
    fun setStockUnit(v: MaterialUnit) = _state.update { it.copy(stockUnit = v) }

    fun saveStock() {
        val s = _state.value
        val item = s.item ?: return
        viewModelScope.launch {
            finishedItemRepository.upsert(
                item.copy(
                    quantity = s.stockQuantity.toDoubleOrNull() ?: item.quantity,
                    unit = s.stockUnit,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishedItemDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: FinishedItemDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val item = state.item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { onEdit(item.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier la fiche")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (item.photoPath != null) {
                AsyncImage(
                    model = File(item.photoPath),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            StockCard(
                quantityLabel = "Quantité",
                quantity = state.stockQuantity,
                onQuantityChange = viewModel::setStockQuantity,
                unitSlot = {
                    DetailEnumDropdown(
                        label = "Unité",
                        options = MaterialUnit.entries,
                        selected = state.stockUnit,
                        labelOf = { it.abbreviation },
                        onSelect = viewModel::setStockUnit,
                        modifier = Modifier.weight(1f),
                    )
                },
                locationSlot = null,
                onSave = viewModel::saveStock,
            )

            DetailSection("Informations") {
                DetailRow("Statut", item.status.label)
                if (!item.runName.isNullOrBlank()) DetailRow("Projet lié", item.runName)
                if (item.sellingPrice != null) DetailRow("Prix de vente", "${item.sellingPrice} €")
            }

            if (!item.notes.isNullOrBlank()) {
                DetailSection("Notes") {
                    Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ── Composables de stock éditables ───────────────────────────────────────────

@Composable
private fun StockCard(
    quantityLabel: String,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unitSlot: @Composable RowScope.() -> Unit,
    locationSlot: (@Composable () -> Unit)?,
    onSave: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Stock & Rangement",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = { Text(quantityLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                unitSlot()
            }
            locationSlot?.invoke()
            Button(
                onClick = onSave,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun RangementCard(
    locations: List<StorageLocation>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onSave: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Rangement",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            DetailLocationDropdown(
                locations = locations,
                selectedId = selectedId,
                onSelect = onSelect,
            )
            Button(
                onClick = onSave,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailLocationDropdown(
    locations: List<StorageLocation>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = locations.find { it.id == selectedId }?.fullPath ?: "Aucun",
            onValueChange = {},
            readOnly = true,
            label = { Text("Lieu de rangement") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Aucun") },
                onClick = { onSelect(null); expanded = false }
            )
            locations.forEach { loc ->
                DropdownMenuItem(
                    text = { Text(loc.fullPath) },
                    onClick = { onSelect(loc.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DetailEnumDropdown(
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
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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

// ── Composables read-only partagés ───────────────────────────────────────────

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
