package com.stashbase.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.FinishedItemRepository
import com.stashbase.domain.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditFinishedItemUiState(
    val name: String = "",
    val runId: Long? = null,
    val quantity: String = "1",
    val unit: MaterialUnit = MaterialUnit.PIECE,
    val status: FinishedItemStatus = FinishedItemStatus.PERSONAL_USE,
    val sellingPrice: String = "",
    val notes: String = "",
    val runs: List<Run> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: Boolean = false,
)

@HiltViewModel
class AddEditFinishedItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val finishedItemRepository: FinishedItemRepository,
    private val runRepository: RunRepository,
) : ViewModel() {

    private val itemId: Long? = savedStateHandle.get<Long>("itemId")?.takeIf { it != -1L }

    private val _state = MutableStateFlow(AddEditFinishedItemUiState())
    val uiState: StateFlow<AddEditFinishedItemUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runRepository.getAll().collect { runs ->
                _state.update { it.copy(runs = runs) }
            }
        }
        if (itemId != null) {
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                finishedItemRepository.getAll().firstOrNull()
                    ?.firstOrNull { it.id == itemId }
                    ?.let { item ->
                        _state.update { current ->
                            current.copy(
                                name = item.name,
                                runId = item.runId,
                                quantity = item.quantity.toString(),
                                unit = item.unit,
                                status = item.status,
                                sellingPrice = item.sellingPrice?.toString() ?: "",
                                notes = item.notes ?: "",
                                isLoading = false,
                            )
                        }
                    }
            }
        }
    }

    fun update(block: AddEditFinishedItemUiState.() -> AddEditFinishedItemUiState) =
        _state.update(block)

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            finishedItemRepository.upsert(
                FinishedItem(
                    id = itemId ?: 0L,
                    runId = s.runId,
                    name = s.name.trim(),
                    quantity = s.quantity.toDoubleOrNull() ?: 1.0,
                    unit = s.unit,
                    status = s.status,
                    sellingPrice = s.sellingPrice.toDoubleOrNull(),
                    notes = s.notes.trim().ifBlank { null },
                )
            )
            _state.update { it.copy(isSaved = true) }
        }
    }
}
