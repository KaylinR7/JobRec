package com.example.jobrec.chatbot

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Service interface for Hugging Face API
 */
interface HuggingFaceApi {
    @POST("models/{modelId}")
    suspend fun generateResponse(
        @Path("modelId") modelId: String,
        @Header("Authorization") token: String,
        @Body request: ChatbotRequest
    ): Response<List<ChatbotResponse>>
}

/**
 * Request model for Hugging Face API
 */
data class ChatbotRequest(
    val inputs: String,
    val parameters: Parameters = Parameters()
) {
    data class Parameters(
        val max_length: Int = 100,
        val temperature: Double = 0.7,
        @SerializedName("return_full_text") val returnFullText: Boolean = false
    )
}

/**
 * Response model for Hugging Face API
 */
data class ChatbotResponse(
    @SerializedName("generated_text") val generatedText: String
)

/**
 * Service class for Hugging Face API
 */
class HuggingFaceService {
    private val baseUrl = "https://api-inference.huggingface.co/"
    private val defaultModel = "facebook/blenderbot-400M-distill" // More conversational model
    private val TAG = "HuggingFaceService"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(HuggingFaceApi::class.java)

    /**
     * Generate a response from the Hugging Face API
     * @param query The user's query
     * @param token The Hugging Face API token
     * @param modelId The model ID to use (optional)
     * @return The generated response or an error message
     */
    suspend fun generateResponse(
        query: String,
        token: String,
        modelId: String = defaultModel
    ): String {
        return try {
            val authToken = "Bearer $token"
            val request = ChatbotRequest(inputs = query)
            val response = api.generateResponse(modelId, authToken, request)

            if (response.isSuccessful && response.body() != null) {
                val generatedText = response.body()?.firstOrNull()?.generatedText
                if (generatedText.isNullOrBlank()) {
                    "I'm not sure how to answer that. Could you try rephrasing your question?"
                } else {
                    // Clean up the response - remove any special tokens or formatting
                    generatedText.replace("<s>", "")
                        .replace("</s>", "")
                        .replace("<pad>", "")
                        .trim()
                }
            } else {
                android.util.Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                "I'm having trouble understanding that. Could you try asking in a different way?"
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in generateResponse", e)
            "I'm sorry, I'm having trouble connecting to my knowledge base right now. Please try again later."
        }
    }

    /**
     * Fallback models to try if the primary model fails
     */
    private val fallbackModels = listOf(
        "google/flan-t5-base",
        "gpt2",
        "EleutherAI/gpt-neo-125M"
    )
}
