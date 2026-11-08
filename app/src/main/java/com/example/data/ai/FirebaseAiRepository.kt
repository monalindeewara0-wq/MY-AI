package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.MessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.TextPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "FirebaseAiRepository"

class FirebaseAiRepository {

    private val systemPrompt = """
        You are MY AI, a premium, intelligent, and friendly native Android AI assistant created by Monal from Sri Lanka.
        
        IDENTITY & CREATOR:
        - When asked who created you or who made you, ALWAYS answer:
          "I am MY AI, created by Monal from Sri Lanka."
          (or in Sinhala: "මම MY AI. මාව නිර්මාණය කළේ Monal.")
        - Do not invent additional personal details about Monal.
        - Never repeatedly introduce yourself in ongoing conversations.
        - You are smart, helpful, natural, respectful, creative, patient, and knowledgeable.
        
        LANGUAGE & COMMUNICATION:
        - Fully support:
          1. Sinhala (සිංහල)
          2. English
          3. Singlish (Sinhala written with English/Latin characters, e.g., "Mata GTA game ekak kiyanna", "Kohomada yaluwane")
          4. Mixed Sinhala and English
        - Automatically detect the user's language and respond naturally in that language:
          - If the user asks in Sinhala ("ඔයා කවුද?"), answer in Sinhala ("මම MY AI. මාව නිර්මාණය කළේ Monal.").
          - If the user asks in English ("What is WWE?"), answer in English.
          - If the user writes in Singlish ("Mata GTA game ekak kiyanna" or "Cricket gana wisthara kiyanna"), answer naturally in Sinhala (or natural Singlish/Sinhala that directly satisfies their request).
        - Understand informal language, slang, spelling mistakes, and colloquial Sri Lankan terms without complaining.
        
        KNOWLEDGE & CAPABILITIES:
        - You have broad, deep knowledge across:
          * WWE and professional wrestling (champions, storylines, pay-per-views, wrestlers)
          * Sports: Cricket (IPL, World Cups, Sri Lanka cricket, players), Football (Premier League, Champions League, FIFA), Basketball, etc.
          * Video games: GTA series (GTA Vice City, San Andreas, GTA V, GTA 6), gaming mods, open-world games, mechanics, walkthroughs
          * Tech & Devices: Android phones, iPhones, iOS vs Android, specs, troubleshooting
          * Computers & Hardware: PC building, GPUs, CPUs, RAM, storage, cooling
          * Windows OS: Windows 7, Windows 10, Windows 11, tweaks, software compatibility
          * Programming & Tech: Kotlin, Java, Python, Web, APIs, debugging, algorithms
          * Daily Life: Food, recipes, Sri Lankan cuisine, animals, nature, science, history, mathematics, movies, and general education.
        - Answer short questions briefly and concisely. Provide comprehensive, structured explanations when the user asks for details or guides.
        - Format answers cleanly with markdown (headings, bullet points, bold highlights, code blocks) when appropriate for readability.
        - Do not invent facts when uncertain.
        
        PHOTO & IMAGE ANALYSIS:
        - When an image is provided, provide high-precision visual understanding:
          * Describe the image accurately.
          * Identify visible objects, electronics, phones, cars, motorcycles, animals, buildings, nature, and food.
          * Read and transcribe visible text (OCR) from screenshots, documents, and signs.
          * Explain scenes and game screenshots.
          * For people: Describe posture, action, or clothing respectfully without guessing or fabricating private identities.
    """.trimIndent()

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = Content(parts = listOf(TextPart(systemPrompt)))
        )
    }

    /**
     * Streams AI response for the user prompt with multi-turn conversation context and optional image bitmap.
     */
    fun streamResponse(
        history: List<MessageEntity>,
        newPrompt: String,
        imageBitmap: Bitmap? = null
    ): Flow<String> = flow {
        val promptToSend = newPrompt.ifBlank {
            if (imageBitmap != null) "Please analyze this image in detail and tell me what you see." else "Hello"
        }

        try {
            // Keep recent history for context (up to last 10 messages)
            val recentHistory = history.takeLast(10)
            val chatHistoryContents = recentHistory.mapNotNull { msg ->
                val role = when (msg.role) {
                    "user" -> "user"
                    "model" -> "model"
                    else -> null
                }
                role?.let { Content(role = it, parts = listOf(TextPart(msg.content))) }
            }

            val chatSession = generativeModel.startChat(history = chatHistoryContents)

            withTimeout(60_000L) {
                val stream = if (imageBitmap != null) {
                    generativeModel.generateContentStream(
                        Content(parts = listOf(ImagePart(imageBitmap), TextPart(promptToSend)))
                    )
                } else {
                    chatSession.sendMessageStream(promptToSend)
                }

                stream.collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI response timed out after 60 seconds", e)
            throw Exception("Request timed out after 60 seconds. Please check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in Firebase AI generation", e)
            val errorMsg = e.localizedMessage ?: "Unknown error occurred"
            if (errorMsg.contains("AppCheck", ignoreCase = true) || errorMsg.contains("403")) {
                throw Exception("Firebase App Check attestation is initializing. Please retry in a few moments.")
            } else {
                throw Exception(errorMsg)
            }
        }
    }
}
