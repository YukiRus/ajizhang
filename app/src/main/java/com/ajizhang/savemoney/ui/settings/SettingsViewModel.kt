package com.ajizhang.savemoney.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajizhang.savemoney.data.repository.LlmSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: LlmSettingsRepository,
) : ViewModel() {
    private val formState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> =
        formState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                formState.update {
                    it.copy(
                        apiBaseUrl = settings.apiBaseUrl,
                        apiKey = settings.apiKey,
                        modelName = settings.modelName,
                    )
                }
            }
        }
    }

    fun onApiBaseUrlChange(value: String) {
        formState.update { it.copy(apiBaseUrl = value) }
    }

    fun onApiKeyChange(value: String) {
        formState.update { it.copy(apiKey = value) }
    }

    fun onModelNameChange(value: String) {
        formState.update { it.copy(modelName = value) }
    }

    fun save() {
        val state = uiState.value
        viewModelScope.launch {
            settingsRepository.saveSettings(
                apiBaseUrl = state.apiBaseUrl,
                apiKey = state.apiKey,
                modelName = state.modelName,
            )
        }
    }
}
