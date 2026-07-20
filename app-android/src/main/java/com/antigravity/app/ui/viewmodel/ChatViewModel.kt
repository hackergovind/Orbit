package com.antigravity.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = ""
)

class ChatViewModel(private val targetNodeId: String) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun updateInput(text: String) {
        _state.value = _state.value.copy(currentInput = text)
    }

    fun sendMessage() {
        val text = _state.value.currentInput
        if (text.isBlank()) return

        val newMessage = ChatMessage(text = text, isFromMe = true)
        val updatedMessages = _state.value.messages + newMessage
        
        _state.value = _state.value.copy(
            messages = updatedMessages,
            currentInput = ""
        )
        
        // In a real integration, we'd call AntigravityClient here:
        // client.getSession(targetNodeId).sendMessage(text.toByteArray())
    }
    
    // Simulate receiving a message over the mesh
    fun receiveMessage(text: String) {
        val newMessage = ChatMessage(text = text, isFromMe = false)
        val updatedMessages = _state.value.messages + newMessage
        _state.value = _state.value.copy(messages = updatedMessages)
    }
}
