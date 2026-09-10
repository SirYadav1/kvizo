package com.kvizo.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

@androidx.annotation.Keep
object CommunityFileNameHolder {
    @androidx.annotation.Keep
    val FILE: String = CommunityKeys.COMMUNITY_FILE
}

class CommunityQuizManager(private val context: Context) {

    companion object {
        private const val CONNECT_TIMEOUT = 10_000
        private const val READ_TIMEOUT = 15_000
        private const val MAX_QUIZ_SIZE = 50
        private const val MAX_OPTIONS = 6
        private const val MIN_OPTIONS = 2
    }

    suspend fun fetchAndImport(): Result<List<CommunityQuiz>> = withContext(Dispatchers.IO) {
        try {
            val json = fetchUrl(CommunityKeys.COMMUNITY_JSON_URL)
            val data = JSONObject(json)
            verifyFreshness(data)

            val quizzes = parseQuizzes(data)

            try {
                val sigBase64 = fetchUrlText(CommunityKeys.COMMUNITY_SIG_URL)
                val keyId = data.getString("key_id")
                val publicKeyHex = CommunityKeys.TRUSTED_KEYS[keyId]
                if (publicKeyHex != null) {
                    val sigBytes = Base64.decode(sigBase64.trim(), Base64.DEFAULT)
                    verifyEd25519Signature(json.toByteArray(), sigBytes, publicKeyHex)
                }
            } catch (_: Exception) { }

            cacheVerified(json, "")
            Result.success(quizzes)
        } catch (e: Exception) {
            val cached = getCached()
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    suspend fun importToDatabase(
        quizzes: List<CommunityQuiz>,
        repository: QuizRepository,
        profileId: Long
    ): Int = withContext(Dispatchers.IO) {
        var imported = 0
        for (quiz in quizzes) {
            try {
                val existing = repository.getQuizzes(profileId).find {
                    it.title == quiz.title && it.isRemote
                }
                if (existing != null) continue

                val quizId = java.util.UUID.randomUUID().toString()
                val localQuiz = Quiz(
                    id = quizId,
                    profileId = profileId,
                    title = sanitizeText(quiz.title),
                    category = sanitizeText(quiz.category),
                    difficulty = quiz.difficulty,
                    tags = "community",
                    timeLimitSeconds = null,
                    status = "published",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    attemptsCount = 0,
                    averageScore = 0.0,
                    description = "Imported from community",
                    isRemote = true
                )
                repository.insertQuiz(localQuiz)

                val questions = mutableListOf<Question>()
                for ((idx, q) in quiz.questions.withIndex()) {
                    val opts = q.options.map { sanitizeText(it) }
                    val correctLetter = listOf("a", "b", "c", "d")[q.correctIndex.coerceIn(0, 3)]
                    questions.add(Question(
                        id = java.util.UUID.randomUUID().toString(),
                        quizId = quizId,
                        questionText = sanitizeText(q.question),
                        optionA = opts.getOrElse(0) { "" },
                        optionB = opts.getOrElse(1) { "" },
                        optionC = opts.getOrElse(2) { "" },
                        optionD = opts.getOrElse(3) { "" },
                        correctOption = correctLetter,
                        position = idx,
                        isBookmarked = false
                    ))
                }
                repository.insertQuestions(questions)
                imported++
            } catch (_: Exception) { }
        }
        imported
    }

    private fun fetchUrl(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", "Kvizo/1.6.1")
        try {
            check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchUrlText(urlStr: String): String {
        return fetchUrl(urlStr)
    }

    private fun verifyFreshness(data: JSONObject) {
        val expiresAt = data.optString("expires_at", "")
        if (expiresAt.isNotEmpty()) {
            try {
                val expiry = java.time.Instant.parse(expiresAt)
                if (expiry.isBefore(java.time.Instant.now())) {
                    throw SecurityException("Community quizzes expired at $expiresAt")
                }
            } catch (_: java.time.format.DateTimeParseException) { }
        }
    }

    private fun verifyEd25519Signature(data: ByteArray, signature: ByteArray, publicKeyHex: String): Boolean {
        return tryEd25519Verify(data, signature, publicKeyHex, "Ed25519") ||
               tryEd25519Verify(data, signature, publicKeyHex, "EdDSA")
    }

    private fun tryEd25519Verify(data: ByteArray, signature: ByteArray, publicKeyHex: String, algorithm: String): Boolean = try {
        val pubBytes = hexToBytes(publicKeyHex)
        val x509Prefix = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
        )
        val spec = X509EncodedKeySpec(x509Prefix + pubBytes)
        val kf = KeyFactory.getInstance(algorithm)
        val publicKey = kf.generatePublic(spec)
        val sig = Signature.getInstance(algorithm)
        sig.initVerify(publicKey)
        sig.update(data)
        sig.verify(signature)
    } catch (_: Exception) { false }

    private fun parseQuizzes(data: JSONObject): List<CommunityQuiz> {
        val quizzes = mutableListOf<CommunityQuiz>()
        val arr = data.getJSONArray("quizzes")

        for (i in 0 until arr.length()) {
            val jq = arr.getJSONObject(i)
            val questions = mutableListOf<CommunityQuestion>()

            val qArr = jq.getJSONArray("questions")
            if (qArr.length() > MAX_QUIZ_SIZE) continue

            for (k in 0 until qArr.length()) {
                val q = qArr.getJSONObject(k)
                val opts = q.getJSONArray("options")
                if (opts.length() < MIN_OPTIONS || opts.length() > MAX_OPTIONS) continue

                val options = mutableListOf<String>()
                for (j in 0 until opts.length()) {
                    options.add(opts.getString(j))
                }

                questions.add(
                    CommunityQuestion(
                        question = q.getString("question"),
                        options = options,
                        correctIndex = q.getInt("correct_index"),
                        explanation = q.optString("explanation", "")
                    )
                )
            }

            if (questions.isEmpty()) continue

            quizzes.add(
                CommunityQuiz(
                    id = jq.optString("id", "comm-$i"),
                    title = jq.getString("title"),
                    category = jq.optString("category", "General"),
                    difficulty = jq.optString("difficulty", "Medium"),
                    questions = questions,
                    author = jq.optString("author", "Unknown"),
                    createdAt = jq.optString("created_at", "")
                )
            )
        }
        return quizzes
    }

    private fun sanitizeText(text: String): String {
        return text
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("[\\x00-\\x1F]"), "")
            .trim()
    }

    private fun cacheVerified(json: String, sigBase64: String) {
        try {
            val cacheDir = File(context.cacheDir, "community")
            cacheDir.mkdirs()
            File(cacheDir, CommunityKeys.CACHE_FILE).writeText(json)
            File(cacheDir, CommunityKeys.CACHE_SIG_FILE).writeText(sigBase64)
        } catch (_: Exception) { }
    }

    private fun getCached(): List<CommunityQuiz>? {
        return try {
            val cacheDir = File(context.cacheDir, "community")
            val jsonFile = File(cacheDir, CommunityKeys.CACHE_FILE)
            val sigFile = File(cacheDir, CommunityKeys.CACHE_SIG_FILE)

            if (!jsonFile.exists() || !sigFile.exists()) return null

            val age = System.currentTimeMillis() - jsonFile.lastModified()
            if (age > CommunityKeys.MAX_CACHE_AGE_MS) return null

            val json = jsonFile.readText()
            val sigBase64 = sigFile.readText()
            val data = JSONObject(json)

            val keyId = data.getString("key_id")
            val publicKeyHex = CommunityKeys.TRUSTED_KEYS[keyId] ?: return null

            val sigBytes = Base64.decode(sigBase64.trim(), Base64.DEFAULT)
            if (!verifyEd25519Signature(json.toByteArray(), sigBytes, publicKeyHex)) return null

            parseQuizzes(data)
        } catch (_: Exception) {
            null
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            bytes[i] = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16).toByte()
        }
        return bytes
    }
}

data class CommunityQuiz(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String,
    val questions: List<CommunityQuestion>,
    val author: String,
    val createdAt: String
)

data class CommunityQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)
