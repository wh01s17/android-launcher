package com.wh01s17.launcher.feature.settings.style

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wh01s17.launcher.core.config.ConfigRepository
import com.wh01s17.launcher.core.config.EditResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class StyleViewModel(
    private val repository: ConfigRepository,
) : ViewModel() {
    val config = repository.state
    private val messageState = MutableStateFlow<String?>(null)
    val errors = messageState.asStateFlow()

    fun change(
        section: String,
        key: String,
        value: Any,
    ) {
        val primitive =
            when (value) {
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
        viewModelScope.launch {
            report(repository.editValues(JsonObject(mapOf(section to JsonObject(mapOf(key to primitive))))))
        }
    }

    fun report(result: EditResult) {
        messageState.value = (result as? EditResult.Refused)?.errors?.joinToString("\n")
    }
}
