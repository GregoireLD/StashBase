package com.stashbase.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.Material
import com.stashbase.domain.model.MaterialType
import com.stashbase.domain.model.Tool
import com.stashbase.domain.repository.MaterialRepository
import com.stashbase.domain.repository.ToolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InventoryTab { MATERIALS, TOOLS }

data class InventoryUiState(
    val tab: InventoryTab = InventoryTab.MATERIALS,
    val searchQuery: String = "",
    val selectedType: MaterialType? = null,
    val materials: List<Material> = emptyList(),
    val tools: List<Tool> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val materialRepository: MaterialRepository,
    private val toolRepository: ToolRepository,
) : ViewModel() {

    private val _tab = MutableStateFlow(InventoryTab.MATERIALS)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<MaterialType?>(null)

    val uiState: StateFlow<InventoryUiState> = combine(
        _tab,
        _searchQuery,
        _selectedType,
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) materialRepository.getAll()
            else materialRepository.search(query)
        },
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) toolRepository.getAll()
            else toolRepository.search(query)
        },
    ) { tab, query, selectedType, materials, tools ->
        val filteredMaterials = if (selectedType != null)
            materials.filter { it.type == selectedType }
        else materials
        InventoryUiState(
            tab = tab,
            searchQuery = query,
            selectedType = selectedType,
            materials = filteredMaterials,
            tools = tools,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun setTab(tab: InventoryTab) { _tab.value = tab }
    fun setSearch(query: String) { _searchQuery.value = query }
    fun setTypeFilter(type: MaterialType?) { _selectedType.value = type }

    fun deleteMaterial(id: Long) = viewModelScope.launch { materialRepository.delete(id) }
    fun deleteTool(id: Long) = viewModelScope.launch { toolRepository.delete(id) }
}
