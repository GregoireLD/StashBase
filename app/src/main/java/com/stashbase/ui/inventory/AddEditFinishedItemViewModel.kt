package com.stashbase.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stashbase.domain.model.*
import com.stashbase.domain.repository.FinishedItemRepository
import com.stashbase.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditFinishedItemUiState(
    val name: String = "",
    val projectId: Long? = null,
    val quantity: String = "1",
    val unit: MaterialUnit = MaterialUnit.PIECE,
    val status: FinishedItemStatus = FinishedItemStatus.PERSONAL_USE,
    val sellingPrice: String = "",
    val notes: String = "",
    val projects: List<Project> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: Boolean = false,
    val projectError: Boolean = false,
)

@HiltViewModel
class AddEditFinishedItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val finishedItemRepository: FinishedItemRepository,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val itemId: Long? = savedStateHandle.get<Long>("itemId")?.takeIf { it != -1L }

    private val _state = MutableStateFlow(AddEditFinishedItemUiState())
    val uiState: StateFlow<AddEditFinishedItemUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.getAll().collect { projects ->
                _state.update { it.copy(projects = projects) }
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
                                projectId = item.projectId,
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
        if (s.projectId == null) {
            _state.update { it.copy(projectError = true) }
            return
        }
        viewModelScope.launch {
            finishedItemRepository.upsert(
                FinishedItem(
                    id = itemId ?: 0L,
                    projectId = s.projectId,
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
