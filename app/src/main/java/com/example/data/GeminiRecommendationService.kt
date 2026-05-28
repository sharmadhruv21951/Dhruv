package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRecommendationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getRecommendations(
        mood: String,
        currentSong: Song?,
        allCatalog: List<Song>
    ): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Graceful check for placeholder or empty API keys
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            Log.w("GeminiService", "Gemini API key is placeholder or empty. Using fallback recommendations.")
            return@withContext getLocalBackupRecommendations(mood, currentSong, allCatalog)
        }

        val catalogJsonArray = JSONArray()
        allCatalog.forEach {
            val s = JSONObject()
            s.put("id", it.id)
            s.put("title", it.title)
            s.put("artist", it.artist)
            s.put("genre", it.genre)
            catalogJsonArray.put(s)
        }

        val prompt = """
            You are Symphony AI, a brilliant personal music assistant. 
            Recommend 4 songs from this catalog based on the user's current mood of: "$mood".
            Currently playing song: ${currentSong?.let { "'${it.title}' by ${it.artist} [Genre: ${it.genre}]" } ?: "None"}.
            
            Catalog of available songs:
            $catalogJsonArray
            
            Return ONLY a raw JSON array containing the recommended 'id' strings, without any markdown formatting, backticks, or text explanation. Example target output format:
            ["broke_night_owl", "lakey_warm_nights"]
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiService", "API call failed with response code: ${response.code}")
                    return@withContext getLocalBackupRecommendations(mood, currentSong, allCatalog)
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var responseText = parts.getJSONObject(0).optString("text") ?: ""
                            
                            // Clean markdown blocks if returned by model
                            responseText = responseText.replace("```json", "")
                            responseText = responseText.replace("```", "")
                            responseText = responseText.trim()

                            Log.d("GeminiService", "Response received from model: $responseText")

                            val listResponse = mutableListOf<String>()
                            try {
                                val jsonArr = JSONArray(responseText)
                                for (i in 0 until jsonArr.length()) {
                                    listResponse.add(jsonArr.getString(i))
                                }
                                if (listResponse.isNotEmpty()) {
                                    return@withContext listResponse
                                }
                            } catch (e: Exception) {
                                // Match inside string via regex/scanning
                                val idList = allCatalog.map { it.id }
                                val found = idList.filter { responseText.contains(it) }
                                if (found.isNotEmpty()) {
                                    return@withContext found
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to contact Gemini endpoint: ${e.message}")
        }

        return@withContext getLocalBackupRecommendations(mood, currentSong, allCatalog)
    }

    private fun getLocalBackupRecommendations(
        mood: String,
        currentSong: Song?,
        allCatalog: List<Song>
    ): List<String> {
        val lowercaseMood = mood.lowercase()
        return allCatalog.filter {
            val genreMatch = it.genre.lowercase().contains(lowercaseMood)
            val artistMatch = it.artist.lowercase().contains(lowercaseMood)
            val titleMatch = it.title.lowercase().contains(lowercaseMood)
            val activeSongGenreMatch = (currentSong != null && it.genre.equals(currentSong.genre, ignoreCase = true))
            
            genreMatch || artistMatch || titleMatch || activeSongGenreMatch ||
            (lowercaseMood.contains("party") && (it.genre.contains("Pop") || it.genre.contains("Synth"))) ||
            (lowercaseMood.contains("chill") && it.genre.contains("Beats")) ||
            (lowercaseMood.contains("lofi") && it.genre.contains("Beats")) ||
            (lowercaseMood.contains("bollywood") && it.genre.contains("Bollywood"))
        }.map { it.id }.shuffled().take(4).ifEmpty {
            // General random pool if no matches found
            allCatalog.shuffled().take(4).map { it.id }
        }
    }
}
