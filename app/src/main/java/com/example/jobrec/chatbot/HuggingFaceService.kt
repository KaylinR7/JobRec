package com.example.jobrec.chatbot
import android.util.Log
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
import java.util.concurrent.TimeUnit
interface HuggingFaceApi {
    @POST("models/{modelId}")
    suspend fun generateResponse(
        @Path("modelId") modelId: String,
        @Header("Authorization") token: String,
        @Body request: ChatbotRequest
    ): Response<List<ChatbotResponse>>
}
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
data class ChatbotResponse(
    @SerializedName("generated_text") val generatedText: String
)
class HuggingFaceService {
    private val baseUrl = "https://api-inference.huggingface.co/"
    private val defaultModel = "facebook/blenderbot-400M-distill"
    private val TAG = "HuggingFaceService"
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val api = retrofit.create(HuggingFaceApi::class.java)
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
                    generatedText.replace("<s>", "")
                        .replace("</s>", "")
                        .replace("<pad>", "")
                        .trim()
                }
            } else {
                Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                "I'm having trouble understanding that. Could you try asking in a different way?"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in generateResponse", e)
            "I'm sorry, I'm having trouble connecting to my knowledge base right now. Please try again later."
        }
    }
    private val fallbackModels = listOf(
        "google/flan-t5-base",
        "gpt2",
        "EleutherAI/gpt-neo-125M"
    )
}
