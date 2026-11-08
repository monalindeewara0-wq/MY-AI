package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.ai.FirebaseAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.InputStream

class ChatRepository(
    private val database: AppDatabase,
    private val aiRepository: FirebaseAiRepository,
    private val context: Context
) {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    val allConversations: Flow<List<ConversationEntity>> =
        conversationDao.getAllConversations().flowOn(Dispatchers.IO)

    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId).flowOn(Dispatchers.IO)

    suspend fun createNewConversation(initialTitle: String = "New Chat"): Long = withContext(Dispatchers.IO) {
        val conv = ConversationEntity(title = initialTitle)
        conversationDao.insertConversation(conv)
    }

    suspend fun renameConversation(conversationId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        conversationDao.updateConversationTitle(conversationId, newTitle)
    }

    suspend fun deleteConversation(conversationId: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        messageDao.clearAllMessages()
        conversationDao.clearAllConversations()
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(messageId)
    }

    /**
     * Converts a content Uri to an in-memory Bitmap scaled appropriately (under 20MB / max 1600px).
     */
    suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var inSampleSize = 1
            val maxDim = 1600
            val width = options.outWidth
            val height = options.outHeight

            if (width > maxDim || height > maxDim) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sends user message and streams the AI response chunk by chunk, keeping Room database in sync.
     */
    suspend fun sendMessage(
        conversationId: Long,
        userText: String,
        imageUri: Uri?,
        onChunk: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        // 1. Insert user message
        val userMessage = MessageEntity(
            conversationId = conversationId,
            role = "user",
            content = userText,
            imageUri = imageUri?.toString(),
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMessage)

        // 2. Auto-generate title if this is the first message or titled "New Chat"
        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null && (conv.title == "New Chat" || conv.title.isBlank())) {
            val titleCandidate = if (userText.isNotBlank()) {
                if (userText.length > 28) userText.take(28) + "..." else userText
            } else if (imageUri != null) {
                "Photo Analysis"
            } else {
                "Conversation"
            }
            conversationDao.updateConversationTitle(conversationId, titleCandidate)
        } else {
            conversationDao.touchConversation(conversationId)
        }

        // 3. Prepare AI message slot
        val aiMessageId = messageDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = "model",
                content = "",
                timestamp = System.currentTimeMillis()
            )
        )

        // 4. Load bitmap if an image was attached
        val bitmap = imageUri?.let { loadBitmapFromUri(it) }

        // 5. Fetch prior conversation history
        val allMessages = messageDao.getMessagesList(conversationId)
        // Pass history prior to current query
        val historyForContext = allMessages.filter { it.id != userMessage.id && it.id != aiMessageId }

        val accumulated = StringBuilder()
        try {
            aiRepository.streamResponse(
                history = historyForContext,
                newPrompt = userText,
                imageBitmap = bitmap
            ).collect { chunk ->
                accumulated.append(chunk)
                messageDao.updateMessageContent(aiMessageId, accumulated.toString())
                withContext(Dispatchers.Main) {
                    onChunk(accumulated.toString())
                }
            }
        } catch (e: Exception) {
            val errorText = e.localizedMessage ?: "Failed to generate response. Please try again."
            val finalContent = if (accumulated.isNotEmpty()) {
                "$accumulated\n\n⚠️ [$errorText]"
            } else {
                "⚠️ $errorText"
            }
            messageDao.updateMessageContent(aiMessageId, finalContent)
            throw e
        }
    }

    /**
     * Regenerates the last assistant answer by deleting the last model message and re-running the last user query.
     */
    suspend fun regenerateLastAnswer(
        conversationId: Long,
        onChunk: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val messages = messageDao.getMessagesList(conversationId)
        if (messages.isEmpty()) return@withContext

        val lastModelMessage = messages.lastOrNull { it.role == "model" || it.role == "error" }
        val lastUserMessage = messages.lastOrNull { it.role == "user" } ?: return@withContext

        if (lastModelMessage != null) {
            messageDao.deleteMessage(lastModelMessage.id)
        }

        val aiMessageId = messageDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = "model",
                content = "",
                timestamp = System.currentTimeMillis()
            )
        )

        val imageUri = lastUserMessage.imageUri?.let { Uri.parse(it) }
        val bitmap = imageUri?.let { loadBitmapFromUri(it) }

        val historyForContext = messages.filter { it.id < lastUserMessage.id }
        val accumulated = StringBuilder()

        try {
            aiRepository.streamResponse(
                history = historyForContext,
                newPrompt = lastUserMessage.content,
                imageBitmap = bitmap
            ).collect { chunk ->
                accumulated.append(chunk)
                messageDao.updateMessageContent(aiMessageId, accumulated.toString())
                withContext(Dispatchers.Main) {
                    onChunk(accumulated.toString())
                }
            }
        } catch (e: Exception) {
            val errorText = e.localizedMessage ?: "Failed to regenerate response. Please try again."
            val finalContent = if (accumulated.isNotEmpty()) {
                "$accumulated\n\n⚠️ [$errorText]"
            } else {
                "⚠️ $errorText"
            }
            messageDao.updateMessageContent(aiMessageId, finalContent)
            throw e
        }
    }
}
