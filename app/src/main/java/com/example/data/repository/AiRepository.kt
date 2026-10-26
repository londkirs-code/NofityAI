package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.ApiKeyDao
import com.example.data.local.ChatDao
import com.example.data.model.ApiKeyEntity
import com.example.data.model.ChatConversationEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ProviderType
import com.example.data.remote.AiApiClient
import com.example.data.remote.AiCallException
import com.example.data.remote.AiResponse
import com.example.data.remote.AttachmentData
import com.example.data.security.CryptoEngine
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

data class QueryResult(
    val responseText: String,
    val provider: ProviderType,
    val model: String,
    val keyName: String,
    val fallbackOccurred: Boolean,
    val fallbackReason: String? = null,
    val conversationId: Long = 0,
    val tokensUsed: Int = 0
)

class AiRepository(
    private val apiKeyDao: ApiKeyDao,
    private val chatDao: ChatDao,
    private val context: Context,
    private val apiClient: AiApiClient = AiApiClient()
) {
    val allKeysFlow: Flow<List<ApiKeyEntity>> = apiKeyDao.getAllKeysFlow()
    val allConversationsFlow: Flow<List<ChatConversationEntity>> = chatDao.getAllConversationsFlow()

    fun getMessagesForConversationFlow(conversationId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForConversationFlow(conversationId)

    suspend fun getOrCreateActiveConversation(): ChatConversationEntity {
        val latest = chatDao.getLatestConversation()
        if (latest != null && (System.currentTimeMillis() - latest.updatedAt) < 1000 * 60 * 60 * 6) {
            return latest
        }
        val newConv = ChatConversationEntity(
            title = "שיחה מ-NotifAI",
            lastSnippet = "מוכן לשאלות"
        )
        val id = chatDao.insertConversation(newConv)
        return newConv.copy(id = id)
    }

    suspend fun createNewConversation(title: String = "שיחה חדשה"): ChatConversationEntity {
        val newConv = ChatConversationEntity(
            title = title,
            lastSnippet = ""
        )
        val id = chatDao.insertConversation(newConv)
        return newConv.copy(id = id)
    }

    /**
     * Executes AI query with smart key priority and automatic fallback when hitting quotas.
     */
    suspend fun askAi(
        prompt: String,
        attachmentUri: Uri? = null,
        conversationId: Long? = null
    ): QueryResult {
        val allEnabledKeys = apiKeyDao.getEnabledKeys()
        if (allEnabledKeys.isEmpty()) {
            throw IllegalStateException("לא הוגדרו מפתחות API באפליקציה. אנא הכנס מפתח במסך הניהול.")
        }

        // Active conversation
        val conv = if (conversationId != null) {
            chatDao.getConversationById(conversationId) ?: getOrCreateActiveConversation()
        } else {
            getOrCreateActiveConversation()
        }

        // Read attachment if present
        val attachment = attachmentUri?.let { readAttachmentFromUri(it) }

        // Save user message to database
        val userMessage = ChatMessageEntity(
            conversationId = conv.id,
            role = "user",
            content = prompt,
            timestamp = System.currentTimeMillis(),
            attachmentUri = attachmentUri?.toString(),
            attachmentMimeType = attachment?.mimeType,
            attachmentName = attachment?.name
        )
        chatDao.addMessageAndUpdateConversation(conv.id, userMessage, prompt)

        // Find eligible keys (skip those still suspended / cooling down)
        val currentTime = System.currentTimeMillis()
        val sortedKeys = allEnabledKeys.sortedBy { it.priority }

        var fallbackOccurred = false
        var fallbackReason: String? = null
        val errorsEncountered = mutableListOf<String>()

        for (keyEntity in sortedKeys) {
            if (keyEntity.isSuspended) {
                fallbackOccurred = true
                val remainingSec = keyEntity.remainingSuspensionSeconds
                fallbackReason = "המפתח '${keyEntity.name}' מושהה לעוד ${remainingSec} שניות"
                continue
            }

            val rawKey = CryptoEngine.decrypt(keyEntity.encryptedKey)
            if (rawKey.isBlank()) {
                continue
            }

            try {
                val response: AiResponse = apiClient.queryAi(
                    provider = keyEntity.provider,
                    apiKey = rawKey,
                    model = keyEntity.selectedModel,
                    prompt = prompt,
                    attachment = attachment
                )

                // Record success on key
                apiKeyDao.recordSuccess(keyEntity.id)

                // Save assistant message to database
                val assistantMessage = ChatMessageEntity(
                    conversationId = conv.id,
                    role = "assistant",
                    content = response.text,
                    provider = response.provider.displayName,
                    model = response.model,
                    tokens = response.tokensUsed,
                    timestamp = System.currentTimeMillis()
                )
                chatDao.addMessageAndUpdateConversation(conv.id, assistantMessage, response.text)

                return QueryResult(
                    responseText = response.text,
                    provider = response.provider,
                    model = response.model,
                    keyName = keyEntity.name,
                    fallbackOccurred = fallbackOccurred,
                    fallbackReason = fallbackReason,
                    conversationId = conv.id,
                    tokensUsed = response.tokensUsed
                )

            } catch (e: AiCallException.QuotaExceeded) {
                val until = System.currentTimeMillis() + (e.suspensionSeconds * 1000)
                val reason = "מכסה נוצלה. ממתין להתחדשות: ${e.message}"
                apiKeyDao.markSuspended(keyEntity.id, until, reason)
                fallbackOccurred = true
                fallbackReason = "המפתח '${keyEntity.name}' ניצל את המכסה, מנסה את הבא בסדר העדיפויות"
                errorsEncountered.add("${keyEntity.name}: ${e.message}")
            } catch (e: AiCallException.RateLimit) {
                val until = System.currentTimeMillis() + (e.retryAfterSeconds * 1000)
                val reason = "הגבלת קצב בקשות (Rate Limit): ${e.message}"
                apiKeyDao.markSuspended(keyEntity.id, until, reason)
                fallbackOccurred = true
                fallbackReason = "המפתח '${keyEntity.name}' חרג מקצב בקשות, עובר למפתח הבא"
                errorsEncountered.add("${keyEntity.name}: ${e.message}")
            } catch (e: AiCallException.ServerUnavailable) {
                val until = System.currentTimeMillis() + (e.retryAfterSeconds * 1000)
                apiKeyDao.markSuspended(keyEntity.id, until, "שרת לא זמין: ${e.message}")
                fallbackOccurred = true
                fallbackReason = "שרת '${keyEntity.name}' אינו זמין כרגע, מנסה ספק חלופי"
                errorsEncountered.add("${keyEntity.name}: ${e.message}")
            } catch (e: Exception) {
                errorsEncountered.add("${keyEntity.name}: ${e.message}")
                fallbackOccurred = true
                fallbackReason = "שגיאה ב-${keyEntity.name}: ${e.message}"
            }
        }

        // All keys failed or suspended
        val errSummary = if (errorsEncountered.isNotEmpty()) {
            errorsEncountered.joinToString("\n• ")
        } else {
            "כל המפתחות מושהים זמנית עקב מגבלת מכסה. אנא המתן להתחדשות המכסה או הוסף מפתח נוסף."
        }
        val errText = "⚠️ לא התקבל מענה מאף מודל:\n• $errSummary"

        // Save error record
        val errorMsg = ChatMessageEntity(
            conversationId = conv.id,
            role = "error",
            content = errText,
            timestamp = System.currentTimeMillis()
        )
        chatDao.addMessageAndUpdateConversation(conv.id, errorMsg, errText)

        throw IllegalStateException(errText)
    }

    // Key management
    suspend fun saveApiKey(
        id: String? = null,
        provider: ProviderType,
        name: String,
        plainKey: String,
        model: String,
        priority: Int = 1
    ) {
        val encrypted = CryptoEngine.encrypt(plainKey)
        val key = ApiKeyEntity(
            id = id ?: java.util.UUID.randomUUID().toString(),
            provider = provider,
            name = name.ifBlank { "${provider.displayName} #${priority}" },
            encryptedKey = encrypted,
            selectedModel = model.ifBlank { provider.defaultModel },
            priority = priority,
            isEnabled = true
        )
        apiKeyDao.insertOrUpdate(key)
    }

    suspend fun getDecryptedKey(id: String): String {
        val key = apiKeyDao.getKeyById(id) ?: return ""
        return CryptoEngine.decrypt(key.encryptedKey)
    }

    suspend fun updateKeyEntity(entity: ApiKeyEntity) {
        apiKeyDao.update(entity)
    }

    suspend fun deleteKey(entity: ApiKeyEntity) {
        apiKeyDao.delete(entity)
    }

    suspend fun clearKeySuspension(id: String) {
        apiKeyDao.clearSuspension(id)
    }

    suspend fun swapPriorities(key1: ApiKeyEntity, key2: ApiKeyEntity) {
        val p1 = key1.priority
        val p2 = key2.priority
        apiKeyDao.updatePriority(key1.id, p2)
        apiKeyDao.updatePriority(key2.id, p1)
    }

    suspend fun deleteConversation(id: Long) {
        chatDao.deleteConversationById(id)
    }

    suspend fun clearAllHistory() {
        chatDao.clearAllConversations()
    }

    private fun readAttachmentFromUri(uri: Uri): AttachmentData? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            var fileName = "attachment"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            }

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() } ?: return null

            AttachmentData(
                name = fileName,
                mimeType = mimeType,
                bytes = bytes
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
