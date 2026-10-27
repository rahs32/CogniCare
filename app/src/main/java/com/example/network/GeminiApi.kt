package com.example.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

suspend fun askGemini(prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        // Provide mock fallback responses for the demo if API key isn't configured
        if (prompt.contains("JSON", ignoreCase = true)) {
            return@withContext """
                ```json
                [
                  {"name": "Wake Up", "icon": "☀️", "time": "08:00"},
                  {"name": "Breakfast", "icon": "🍳", "time": "08:30"},
                  {"name": "Activity based on prompt", "icon": "🧩", "time": "10:00"},
                  {"name": "Lunch Break", "icon": "🥪", "time": "12:30"},
                  {"name": "Relaxation", "icon": "🧘", "time": "15:00"}
                ]
                ```
            """.trimIndent()
        } else if (prompt.contains("loud", ignoreCase = true)) {
            return@withContext "Try using your noise-canceling headphones or finding a quieter corner to help reduce the sensory load."
        } else if (prompt.contains("bright", ignoreCase = true)) {
            return@withContext "Consider putting on sunglasses or finding a spot with softer lighting until you feel more comfortable."
        }
        return@withContext "API key not configured."
    }
    
    val request = GenerateContentRequest(
        contents = listOf(Content(
            parts = listOf(Part(text = prompt))
        )),
        systemInstruction = systemPrompt?.let { Content(listOf(Part(it))) }
    )
    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from AI."
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
