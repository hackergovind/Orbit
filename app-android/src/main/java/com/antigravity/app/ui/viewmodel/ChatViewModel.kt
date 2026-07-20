package com.antigravity.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bmtp.transport.BleTransportManager
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
    val peerId: String = "",   // target MAC address
    val peerName: String = "", // display name
    val messages: List<ChatMessage> = emptyList(),
    val isVoiceRecording: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        // Collect incoming BLE messages and add to chat
        viewModelScope.launch {
            BleTransportManager.messageFlow.collect { bleMsg ->
                val newMsg = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    senderName = if (bleMsg.isFromMe) "Me" else bleMsg.senderId,
                    text = bleMsg.text,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = bleMsg.isFromMe
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + newMsg
                )
            }
        }
    }

    fun openChat(peerId: String, peerName: String = peerId) {
        _state.value = _state.value.copy(peerId = peerId, peerName = peerName, messages = emptyList())
    }

    fun sendMessage(text: String) {
        val peer = _state.value.peerId
        if (peer.isEmpty() || text.isBlank()) return

        viewModelScope.launch {
            BleTransportManager.sendMessage(getApplication(), peer, text)
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
