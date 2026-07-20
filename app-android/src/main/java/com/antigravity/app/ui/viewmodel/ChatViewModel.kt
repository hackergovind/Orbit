package com.antigravity.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmtp.transport.TransportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val attachmentType: AttachmentType? = null
)

enum class AttachmentType {
    FILE, VOICE
}

data class ChatState(
    val peerId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isVoiceRecording: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            TransportManager.messageFlow.collect { netMsg ->
                // Ensure we only show messages for the current peer (or a global group)
                // For simplicity, we just add it to the chat
                val newMsg = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    senderName = if (netMsg.isFromMe) "Me" else netMsg.senderId,
                    text = netMsg.text,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = netMsg.isFromMe
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + newMsg
                )
            }
        }
    }

    fun openChat(peerId: String) {
        _state.value = _state.value.copy(peerId = peerId, messages = emptyList())
    }

    fun sendMessage(text: String) {
        val currentPeer = _state.value.peerId
        if (currentPeer.isEmpty()) return
        
        viewModelScope.launch {
            TransportManager.sendMessage(currentPeer, text)
        }
    }

    fun setVoiceRecording(isRecording: Boolean) {
        _state.value = _state.value.copy(isVoiceRecording = isRecording)
    }

    fun attachFile() {
        val stubMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderName = "Me",
            text = "Sent a file attachment.",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            attachmentType = AttachmentType.FILE
        )
        _state.value = _state.value.copy(messages = _state.value.messages + stubMsg)
    }
}
