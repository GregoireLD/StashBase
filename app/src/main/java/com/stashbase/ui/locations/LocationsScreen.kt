package com.stashbase.ui.locations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.StorageLocation
import com.stashbase.domain.repository.StorageLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val repository: StorageLocationRepository,
) : ViewModel() {

    val locations: StateFlow<List<StorageLocation>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}

@HiltViewModel
class AddEditLocationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StorageLocationRepository,
) : ViewModel() {

    private val locationId: Long? = savedStateHandle.get<Long>("locationId")
    private val _name = MutableStateFlow("")
    private val _description = MutableStateFlow("")
    private val _parentId = MutableStateFlow<Long?>(null)
    private val _isSaved = MutableStateFlow(false)

    val name: StateFlow<String> = _name.asStateFlow()
    val description: StateFlow<String> = _description.asStateFlow()
    val parentId: StateFlow<Long?> = _parentId.asStateFlow()
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()
    val allLocations: StateFlow<List<StorageLocation>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (locationId != null) {
            viewModelScope.launch {
                repository.getById(locationId).firstOrNull()?.let { loc ->
                    _name.value = loc.name
                    _description.value = loc.description ?: ""
                    _parentId.value = loc.parentLocationId
                }
            }
        }
    }

    fun setName(v: String) { _name.value = v }
    fun setDescription(v: String) { _description.value = v }
    fun setParent(id: Long?) { _parentId.value = id }

    fun save() {
        if (_name.value.isBlank()) return
        viewModelScope.launch {
            repository.upsert(
                StorageLocation(
                    id = locationId ?: 0L,
                    name = _name.value.trim(),
                    description = _description.value.trim().ifBlank { null },
                    parentLocationId = _parentId.value,
                )
            )
            _isSaved.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onAddLocation: () -> Unit,
    onEditLocation: (Long) -> Unit,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val roots = locations.filter { it.parentLocationId == null }
    val byParent = locations.groupBy { it.parentLocationId }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lieux de rangement") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLocation) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un lieu")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (locations.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun lieu défini", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            items(roots, key = { it.id }) { root ->
                LocationRow(location = root, indent = 0, onEdit = onEditLocation, onDelete = { viewModel.delete(root.id) })
                byParent[root.id]?.forEach { child ->
                    LocationRow(location = child, indent = 1, onEdit = onEditLocation, onDelete = { viewModel.delete(child.id) })
                    byParent[child.id]?.forEach { grandchild ->
                        LocationRow(location = grandchild, indent = 2, onEdit = onEditLocation, onDelete = { viewModel.delete(grandchild.id) })
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: StorageLocation,
    indent: Int,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.padding(start = (indent * 24).dp),
        leadingContent = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(location.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = location.description?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showMenu = false; onEdit(location.id) })
                    DropdownMenuItem(text = { Text("Supprimer") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLocationScreen(
    locationId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditLocationViewModel = hiltViewModel(),
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val parentId by viewModel.parentId.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()

    LaunchedEffect(isSaved) { if (isSaved) onBack() }

    val eligibleParents = allLocations.filter { it.id != locationId }
    var parentExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (locationId == null) "Nouveau lieu" else "Modifier le lieu") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = viewModel::save) { Icon(Icons.Default.Check, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::setName,
                label = { Text("Nom *") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(expanded = parentExpanded, onExpandedChange = { parentExpanded = it }) {
                OutlinedTextField(
                    value = eligibleParents.find { it.id == parentId }?.name ?: "Aucun (racine)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sous-lieu de") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(parentExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = parentExpanded, onDismissRequest = { parentExpanded = false }) {
                    DropdownMenuItem(text = { Text("Aucun (racine)") }, onClick = { viewModel.setParent(null); parentExpanded = false })
                    eligibleParents.forEach { loc ->
                        DropdownMenuItem(text = { Text(loc.fullPath) }, onClick = { viewModel.setParent(loc.id); parentExpanded = false })
                    }
                }
            }
        }
    }
}
