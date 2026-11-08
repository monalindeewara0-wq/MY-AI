package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.FirebaseAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.ChatRepository
import com.example.ui.components.AppThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val aiRepository = FirebaseAiRepository()
    private val chatRepository = ChatRepository(database, aiRepository, application)

    val conversations: StateFlow<List<ConversationEntity>> = chatRepository.allConversations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = _activeConversationId
        .flatMapLatest { id ->
            if (id != null) {
                chatRepository.getMessagesForConversation(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun selectConversation(id: Long) {
        _activeConversationId.value = id
    }

    fun startNewChat() {
        _activeConversationId.value = null
        _inputText.value = ""
        _selectedImageUri.value = null
        _errorMessage.value = null
    }

    fun sendMessage(promptText: String? = null) {
        val textToSend = promptText ?: _inputText.value
        val imageToSend = _selectedImageUri.value

        if (textToSend.isBlank() && imageToSend == null) return
        if (_isGenerating.value) return

        _isGenerating.value = true
        _inputText.value = ""
        _selectedImageUri.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Ensure active conversation exists
                var convId = _activeConversationId.value
                if (convId == null) {
                    val initialTitle = if (textToSend.isNotBlank()) {
                        if (textToSend.length > 25) textToSend.take(25) + "..." else textToSend
                    } else "Photo Analysis"
                    convId = chatRepository.createNewConversation(initialTitle)
                    _activeConversationId.value = convId
                }

                chatRepository.sendMessage(
                    conversationId = convId,
                    userText = textToSend,
                    imageUri = imageToSend,
                    onChunk = { /* reactive flow updates Room automatically */ }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to generate AI response."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun regenerateLastAnswer() {
        val convId = _activeConversationId.value ?: return
        if (_isGenerating.value) return

        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                chatRepository.regenerateLastAnswer(
                    conversationId = convId,
                    onChunk = { /* reactive flow */ }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to regenerate response."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun renameConversation(id: Long, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameConversation(id, newTitle)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            if (_activeConversationId.value == id) {
                _activeConversationId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            _activeConversationId.value = null
        }
    }

    fun clearCurrentChat() {
        val convId = _activeConversationId.value ?: return
        deleteConversation(convId)
    }
}
