package com.stashbase.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.MaterialRepository
import com.stashbase.domain.repository.StorageLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditMaterialUiState(
    val name: String = "",
    val type: MaterialType = MaterialType.FABRIC,
    val brand: String = "",
    val color: String = "",
    val colorCode: String = "",
    val quantity: String = "0",
    val unit: MaterialUnit = MaterialUnit.METER,
    val locationId: Long? = null,
    val purchasePrice: String = "",
    val notes: String = "",
    val tags: String = "",
    val lowStockThreshold: String = "",
    val photoPath: String? = null,
    val locations: List<StorageLocation> = emptyList(),
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: Boolean = false,
)

@HiltViewModel
class AddEditMaterialViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val materialRepository: MaterialRepository,
    private val locationRepository: StorageLocationRepository,
) : ViewModel() {

    private val materialId: Long? = savedStateHandle.get<Long>("materialId")?.takeIf { it != -1L }

    private val _state = MutableStateFlow(AddEditMaterialUiState())
    val uiState: StateFlow<AddEditMaterialUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.getAll().collect { locs ->
                _state.update { it.copy(locations = locs) }
            }
        }
        if (materialId != null) {
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                materialRepository.getById(materialId).firstOrNull()?.let { m ->
                    _state.update { current ->
                        current.copy(
                            name = m.name,
                            type = m.type,
                            brand = m.brand ?: "",
                            color = m.color ?: "",
                            colorCode = m.colorCode ?: "",
                            quantity = m.quantity.toString(),
                            unit = m.unit,
                            locationId = m.locationId,
                            purchasePrice = m.purchasePrice?.toString() ?: "",
                            notes = m.notes ?: "",
                            tags = m.tags.joinToString(", "),
                            lowStockThreshold = m.lowStockThreshold?.toString() ?: "",
                            photoPath = m.photoPath,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun update(block: AddEditMaterialUiState.() -> AddEditMaterialUiState) = _state.update(block)

    fun delete() {
        if (materialId == null) return
        viewModelScope.launch {
            materialRepository.delete(materialId)
            _state.update { it.copy(isDeleted = true) }
        }
    }

    fun save() {
        if (_state.value.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            val s = _state.value
            val material = Material(
                id = materialId ?: 0L,
                name = s.name.trim(),
                type = s.type,
                brand = s.brand.trim().ifBlank { null },
                color = s.color.trim().ifBlank { null },
                colorCode = s.colorCode.trim().ifBlank { null },
                quantity = s.quantity.toDoubleOrNull() ?: 0.0,
                unit = s.unit,
                locationId = s.locationId,
                purchasePrice = s.purchasePrice.toDoubleOrNull(),
                notes = s.notes.trim().ifBlank { null },
                tags = s.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                lowStockThreshold = s.lowStockThreshold.toDoubleOrNull(),
                photoPath = s.photoPath,
            )
            materialRepository.upsert(material)
            _state.update { it.copy(isSaved = true) }
        }
    }
}
