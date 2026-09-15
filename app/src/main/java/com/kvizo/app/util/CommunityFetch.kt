package com.kvizo.app.util

import android.util.Base64
import com.kvizo.app.data.CommunityQuiz
import com.kvizo.app.data.CommunityQuestion
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object CommunityFetch {
    const val COMMUNITY_JSON = "community.json"
    private const val GITHUB_RAW = "https://raw.githubusercontent.com/SirYadav1/kvizo-community/master"
    private const val WORKER_URL = "https://kvizo-api.sundramy807.workers.dev/v1"
    private const val PUBLIC_KEY_B64 = "2pNaP8tVsRA5zAkwvYQ7CGDMWcydDsx67QmqJ5HhMD0="
    private const val CONNECT_TIMEOUT = 8_000
    private const val READ_TIMEOUT = 10_000

    // Simple fetch — no signature blocking, just get quizzes
    fun fetchQuizzesByCategory(category: String = "all"): List<CommunityQuiz> {
        // Try Worker first (fast), fallback to GitHub raw
        return try {
            val catParam = if (category == "all") "" else "?category=$category"
            val data = downloadBytes("$WORKER_URL/quizzes$catParam")
            val json = JSONObject(String(data, Charsets.UTF_8))
            val arr = json.getJSONArray("quizzes")
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CommunityQuiz(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    category = obj.getString("category"),
                    difficulty = obj.optString("difficulty", "Medium"),
                    author = obj.optString("author", "Unknown"),
                    questions = emptyList()
                )
            }
        } catch (_: Exception) {
            fetchLegacy()
        }
    }

    fun fetchQuizFull(quizId: String): CommunityQuiz? {
        return try {
            val data = downloadBytes("$WORKER_URL/quiz/$quizId")
            val q = JSONObject(String(data, Charsets.UTF_8))
            parseQuiz(q)
        } catch (_: Exception) {
            fetchLegacy().find { it.id == quizId }
        }
    }

    // Legacy: download full community.json from GitHub raw
    fun fetchLegacy(): List<CommunityQuiz> {
        return try {
            val data = downloadBytes("$GITHUB_RAW/community.json")
            cache(COMMUNITY_JSON, data)
            parseAll(data)
        } catch (_: Exception) {
            val cached = readCache(COMMUNITY_JSON) ?: return emptyList()
            parseAll(cached)
        }
    }

    fun fetch(name: String): ByteArray {
        return try {
            val data = downloadBytes("$GITHUB_RAW/$name")
            cache(name, data)
            data
        } catch (_: Exception) {
            readCache(name) ?: throw IllegalStateException("no network and no cache for $name")
        }
    }

    private fun parseAll(data: ByteArray): List<CommunityQuiz> {
        val json = JSONObject(String(data, Charsets.UTF_8))
        val arr = json.getJSONArray("quizzes")
        return (0 until arr.length()).mapNotNull { parseQuiz(arr.getJSONObject(it)) }
    }

    private fun parseQuiz(q: JSONObject): CommunityQuiz {
        val questionsArr = q.optJSONArray("questions")
        val questions = if (questionsArr != null) {
            (0 until questionsArr.length()).map { i ->
                val ques = questionsArr.getJSONObject(i)
                val opts = ques.getJSONArray("options")
                CommunityQuestion(
                    question = ques.getString("question"),
                    options = (0 until opts.length()).map { opts.getString(it) },
                    correctIndex = ques.getInt("correct_index"),
                    explanation = ques.optString("explanation", "")
                )
            }
        } else emptyList()

        return CommunityQuiz(
            id = q.getString("id"),
            title = q.getString("title"),
            category = q.getString("category"),
            difficulty = q.optString("difficulty", "Medium"),
            author = q.optString("author", "Unknown"),
            questions = questions
        )
    }

    private fun downloadBytes(url: String): ByteArray {
        val conn = open(url)
        try {
            requireOk(conn)
            val input = conn.inputStream
            val out = ByteArrayOutputStream(maxOf(8192, input.available()))
            input.copyTo(out)
            return out.toByteArray()
        } finally { conn.disconnect() }
    }

    private fun downloadText(url: String): String {
        val conn = open(url)
        try {
            requireOk(conn)
            return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } finally { conn.disconnect() }
    }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.instanceFollowRedirects = true
        return conn
    }

    private fun requireOk(conn: HttpURLConnection) {
        val code = conn.responseCode
        if (code < 200 || code >= 300) throw java.io.IOException("HTTP $code")
    }

    private fun cache(name: String, data: ByteArray) = CacheProvider.write(name, data)
    private fun readCache(name: String): ByteArray? = CacheProvider.read(name)

    object CacheProvider {
        @Volatile lateinit var appContext: android.content.Context
        private fun dir(): File = File(appContext.cacheDir, "community").apply { mkdirs() }
        fun write(name: String, data: ByteArray) {
            try { val tmp = File(dir(), name + ".tmp"); tmp.writeBytes(data); tmp.renameTo(File(dir(), name)) } catch (_: Exception) { }
        }
        fun read(name: String): ByteArray? = try { val f = File(dir(), name); if (f.exists()) f.readBytes() else null } catch (_: Exception) { null }
    }
}
