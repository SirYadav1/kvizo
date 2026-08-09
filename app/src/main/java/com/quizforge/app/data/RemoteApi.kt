package com.quizforge.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal HTTP client for the QuizForge community server.
 * Uses plain HttpURLConnection so no extra dependencies are needed.
 */
class RemoteApi(private val baseUrl: String = DEFAULT_BASE_URL) {

    suspend fun fetchQuizzes(): List<RemoteQuiz> = withContext(Dispatchers.IO) {
        val conn = open("GET", "$baseUrl/api/quizzes")
        try {
            checkResponse(conn)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val arr = root.getJSONArray("quizzes")
            val out = mutableListOf<RemoteQuiz>()
            for (i in 0 until arr.length()) {
                val j = arr.getJSONObject(i)
                val qs = j.getJSONArray("questions")
                val questions = mutableListOf<RemoteQuestion>()
                for (k in 0 until qs.length()) {
                    val q = qs.getJSONObject(k)
                    questions.add(
                        RemoteQuestion(
                            questionText = q.optString("questionText", ""),
                            optionA = q.optString("optionA", ""),
                            optionB = q.optString("optionB", ""),
                            optionC = q.optString("optionC", ""),
                            optionD = q.optString("optionD", ""),
                            correctOption = q.optString("correctOption", "").lowercase()
                        )
                    )
                }
                val time = j.optLong("timeLimitSeconds", -1)
                out.add(
                    RemoteQuiz(
                        id = j.optString("id", ""),
                        title = j.optString("title", ""),
                        category = j.optString("category", ""),
                        difficulty = j.optString("difficulty", "Easy"),
                        tags = j.optString("tags", ""),
                        timeLimitSeconds = if (time > 0) time.toInt() else null,
                        createdAt = j.optLong("createdAt", 0L),
                        description = j.optString("description", ""),
                        questions = questions
                    )
                )
            }
            out
        } finally {
            conn.disconnect()
        }
    }

    /** Sends a heartbeat; returns true on success. Never throws. */
    suspend fun sendHeartbeat(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = open("POST", "$baseUrl/api/heartbeat")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.write(JSONObject().put("deviceId", deviceId).toString().toByteArray())
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }

    /** Fetches community announcements. Never throws (empty list on failure). */
    suspend fun fetchNotifications(): List<RemoteNotification> = withContext(Dispatchers.IO) {
        try {
            val conn = open("GET", "$baseUrl/api/notifications")
            checkResponse(conn)
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val arr = root.optJSONArray("notifications") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val j = arr.getJSONObject(i)
                    add(
                        RemoteNotification(
                            id = j.optString("id", ""),
                            title = j.optString("title", ""),
                            body = j.optString("body", ""),
                            createdAt = j.optLong("createdAt", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun open(method: String, url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        return conn
    }

    private fun checkResponse(conn: HttpURLConnection) {
        if (conn.responseCode !in 200..299) {
            throw java.io.IOException("Server error ${conn.responseCode}")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://ocean.13.60.45.157.nip.io"
    }
}
