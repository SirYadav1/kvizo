package com.kvizo.app.util

import android.util.Base64
import com.kvizo.app.data.CommunityKeys
import com.kvizo.app.data.CommunityQuestion
import com.kvizo.app.data.CommunityQuiz
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Downloads community quizzes and refuses to use anything it cannot verify.
 *
 * The transport is untrusted on purpose: GitHub, jsDelivr, a proxy, a hijacked DNS answer or
 * anyone who gets write access to the content repo can all change the bytes in flight. The trust
 * anchor is the Ed25519 public key embedded in the app ([CommunityKeys]), so content the private
 * key did not sign is dropped no matter who serves it or what it claims.
 *
 * Attempt order, first verified result wins:
 *   1. `manifest.json` (+ `.sig`) verified, then each `quizzes/<cat>/<id>.json` checked against
 *      its SHA-256 inside that signed manifest — a single quiz can only exist if the manifest
 *      vouches for it.
 *   2. `community.json` (+ `.sig`) verified — the full bundle, for repos/bundles that predate the
 *      manifest.
 *   3. Both of the above from the mirror CDN.
 *   4. Verified cache: signatures are re-checked on every read, so a tampered cache is ignored
 *      rather than trusted.
 *
 * If none of that verifies, [fetchAll] throws instead of showing unverified quizzes.
 * The Cloudflare Worker that older builds tried first is gone: it was an extra moving part that
 * added no security (it cannot sign), so the app talks to free static CDNs only.
 */
object CommunityFetch {

    const val COMMUNITY_JSON = "community.json"
    private const val MANIFEST_JSON = "manifest.json"
    private const val SIG_SUFFIX = ".sig"
    private const val CONNECT_TIMEOUT = 8_000
    private const val READ_TIMEOUT = 12_000
    private const val FILE_WORKERS = 4
    private const val FILE_TIMEOUT_SECONDS = 20L
    private const val MAX_QUIZZES = 500

    /** Result of the last successful fetch, e.g. `Verified · 8 quizzes · GitHub`. */
    @Volatile
    var lastStatus: String? = null
        private set

    /**
     * Verified community quizzes, questions included.
     *
     * @throws IllegalStateException when nothing could be verified and no verified cache exists.
     * A caller that quietly showed unverified data would defeat the entire point of signing.
     */
    fun fetchAll(): List<CommunityQuiz> {
        val problems = mutableListOf<String>()
        for (base in CommunityKeys.SOURCES) {
            try {
                val fromManifest = fetchViaManifest(base)
                if (fromManifest.isNotEmpty()) return done(fromManifest, base)
            } catch (e: Exception) {
                problems += "${label(base)} (manifest): ${e.message ?: e.javaClass.simpleName}"
                try {
                    val fromBundle = fetchViaBundle(base)
                    if (fromBundle.isNotEmpty()) return done(fromBundle, base)
                } catch (e2: Exception) {
                    problems += "${label(base)} (bundle): ${e2.message ?: e2.javaClass.simpleName}"
                }
            }
        }

        val cached = try { fetchFromVerifiedCache() } catch (_: Exception) { emptyList() }
        if (cached.isNotEmpty()) {
            lastStatus = "Verified · ${cached.size} quizzes · offline cache"
            return cached
        }

        lastStatus = null
        throw IllegalStateException(problems.firstOrNull() ?: "no verified community content available")
    }

    /** Verified quizzes filtered by category (`all` or blank = everything). */
    fun fetchByCategory(category: String = "all"): List<CommunityQuiz> {
        val all = fetchAll()
        if (category.isBlank() || category.equals("all", ignoreCase = true)) return all
        return all.filter { it.category.equals(category, ignoreCase = true) }
    }

    /** One verified quiz, or null when the id is not part of the signed manifest. */
    fun fetchQuizFull(quizId: String): CommunityQuiz? = fetchAll().firstOrNull { it.id == quizId }

    // ------------------------------------------------------------------ verified fetches

    private fun done(quizzes: List<CommunityQuiz>, base: String): List<CommunityQuiz> {
        lastStatus = "Verified · ${quizzes.size} quizzes · ${label(base)}"
        return quizzes
    }

    private fun fetchViaManifest(base: String): List<CommunityQuiz> {
        val manifestBytes = downloadSigned(base, MANIFEST_JSON)
        val root = JSONObject(String(manifestBytes, Charsets.UTF_8))
        val arr = root.optJSONArray("quizzes") ?: return emptyList()
        val entries = (0 until minOf(arr.length(), MAX_QUIZZES)).map { arr.getJSONObject(it) }
        if (entries.isEmpty()) return emptyList()
        val quizzes = downloadQuizFiles(base, entries)
        if (quizzes.isEmpty()) throw IllegalStateException("no quiz file matched its manifest hash")
        return quizzes
    }

    private fun fetchViaBundle(base: String): List<CommunityQuiz> =
        parseAll(downloadSigned(base, COMMUNITY_JSON))

    private fun downloadQuizFiles(base: String, entries: List<JSONObject>): List<CommunityQuiz> {
        val pool = Executors.newFixedThreadPool(FILE_WORKERS)
        return try {
            entries
                .map { entry -> pool.submit(Callable { downloadVerifiedQuiz(base, entry) }) }
                .mapNotNull { task ->
                    try { task.get(FILE_TIMEOUT_SECONDS, TimeUnit.SECONDS) } catch (_: Exception) { null }
                }
        } finally {
            pool.shutdownNow()
        }
    }

    /** Downloads one quiz file and only returns it when its SHA-256 matches the signed manifest. */
    private fun downloadVerifiedQuiz(base: String, entry: JSONObject): CommunityQuiz? {
        val file = entry.optString("file")
        val expected = entry.optString("sha256").lowercase()
        if (file.isBlank() || expected.length != 64) return null
        val bytes = downloadBytes("$base/$file")
        if (sha256Hex(bytes) != expected) return null // tampered with or stale -> drop it
        val quiz = parseQuiz(bytes) ?: return null
        if (quiz.questions.isEmpty()) return null
        cache("quiz-${quiz.id}.json", bytes)
        return quiz
    }

    /** Downloads `<name>` and `<name>.sig`, and refuses to return bytes the trusted key did not sign. */
    private fun downloadSigned(base: String, name: String): ByteArray {
        val data = downloadBytes("$base/$name")
        val signature = downloadText("$base/$name$SIG_SUFFIX")
        verifyOrThrow(signature, data)
        cache(name, data)
        cache(name + SIG_SUFFIX, signature.toByteArray(Charsets.UTF_8))
        return data
    }

    private fun verifyOrThrow(signature: String, data: ByteArray) {
        val keyId = try { JSONObject(String(data, Charsets.UTF_8)).optString("key_id", "") } catch (_: Exception) { "" }
        val candidates: List<String> = if (keyId.isNotBlank()) {
            listOf(CommunityKeys.TRUSTED_KEYS[keyId] ?: throw SecurityException("signed with unknown key '$keyId'"))
        } else {
            CommunityKeys.TRUSTED_KEYS.values.toList()
        }
        val signatureBytes = decodeBase64(signature) ?: throw SecurityException("signature is not valid base64")
        for (key in candidates) {
            val raw = decodePublicKey(key)
            if (raw != null && Ed25519.verify(raw, signatureBytes, data)) return
        }
        throw SecurityException("signature does not verify against the key this app trusts")
    }

    /** Offline path. Everything read from disk is re-verified before it is used. */
    private fun fetchFromVerifiedCache(): List<CommunityQuiz> {
        val manifestBytes = readCache(MANIFEST_JSON)
        val manifestSignature = readCache(MANIFEST_JSON + SIG_SUFFIX)?.toString(Charsets.UTF_8)
        if (manifestBytes != null && manifestSignature != null) {
            try {
                verifyOrThrow(manifestSignature, manifestBytes)
                val arr = JSONObject(String(manifestBytes, Charsets.UTF_8)).optJSONArray("quizzes")
                if (arr != null) {
                    val quizzes = (0 until minOf(arr.length(), MAX_QUIZZES))
                        .mapNotNull { cachedQuiz(arr.getJSONObject(it)) }
                    if (quizzes.isNotEmpty()) return quizzes
                }
            } catch (_: Exception) {
                // fall through to the bundle cache
            }
        }

        val bundleBytes = readCache(COMMUNITY_JSON)
        val bundleSignature = readCache(COMMUNITY_JSON + SIG_SUFFIX)?.toString(Charsets.UTF_8)
        if (bundleBytes != null && bundleSignature != null) {
            verifyOrThrow(bundleSignature, bundleBytes) // a tampered cache throws, it is not shown
            return parseAll(bundleBytes)
        }
        return emptyList()
    }

    private fun cachedQuiz(entry: JSONObject): CommunityQuiz? {
        val id = entry.optString("id")
        val expected = entry.optString("sha256").lowercase()
        if (id.isBlank() || expected.length != 64) return null
        val bytes = readCache("quiz-$id.json") ?: return null
        if (sha256Hex(bytes) != expected) return null
        val quiz = parseQuiz(bytes) ?: return null
        return if (quiz.questions.isEmpty()) null else quiz
    }

    // ---------------------------------------------------------------------------- parsing

    private fun parseAll(data: ByteArray): List<CommunityQuiz> {
        val json = JSONObject(String(data, Charsets.UTF_8))
        val arr = json.optJSONArray("quizzes") ?: return emptyList()
        return (0 until minOf(arr.length(), MAX_QUIZZES)).mapNotNull { parseQuiz(arr.getJSONObject(it)) }
    }

    private fun parseQuiz(data: ByteArray): CommunityQuiz? = try {
        parseQuiz(JSONObject(String(data, Charsets.UTF_8)))
    } catch (_: Exception) {
        null
    }

    private fun parseQuiz(q: JSONObject): CommunityQuiz? {
        val id = q.optString("id")
        val title = q.optString("title")
        if (id.isBlank() || title.isBlank()) return null
        val questionsArr = q.optJSONArray("questions") ?: return null
        val questions = (0 until questionsArr.length()).mapNotNull { i ->
            val ques = questionsArr.optJSONObject(i) ?: return@mapNotNull null
            val opts = ques.optJSONArray("options") ?: return@mapNotNull null
            val options = (0 until opts.length()).map { opts.optString(it) }.filter { it.isNotBlank() }
            val correct = ques.optInt("correct_index", -1)
            val text = ques.optString("question")
            if (text.isBlank() || options.size < 2 || correct !in options.indices) return@mapNotNull null
            CommunityQuestion(
                question = text,
                options = options,
                correctIndex = correct,
                explanation = ques.optString("explanation", "")
            )
        }
        if (questions.isEmpty()) return null
        return CommunityQuiz(
            id = id,
            title = title,
            category = q.optString("category", "General"),
            difficulty = q.optString("difficulty", "Medium"),
            author = q.optString("author", "kvizo-community"),
            questions = questions
        )
    }

    // --------------------------------------------------------------------------- plumbing

    private fun downloadBytes(url: String): ByteArray {
        val conn = open(url)
        try {
            requireOk(conn)
            val input = conn.inputStream
            val out = ByteArrayOutputStream(maxOf(8192, input.available()))
            input.copyTo(out)
            return out.toByteArray()
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadText(url: String): String {
        val conn = open(url)
        try {
            requireOk(conn)
            return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
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

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        val out = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xff
            out.append(HEX[v ushr 4]).append(HEX[v and 0x0f])
        }
        return out.toString()
    }

    /** Accepts the raw 32-byte public key either as hex (what `public_key.hex` holds) or base64. */
    private fun decodePublicKey(key: String): ByteArray? {
        val trimmed = key.trim()
        return try {
            if (trimmed.length == 64 && trimmed.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                ByteArray(32) { ((trimmed[it * 2].digitToInt(16) shl 4) or trimmed[it * 2 + 1].digitToInt(16)).toByte() }
            } else {
                decodeBase64(trimmed)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBase64(value: String): ByteArray? = try {
        Base64.decode(value.trim(), Base64.DEFAULT)
    } catch (_: Exception) {
        null
    }

    private fun label(base: String): String = when {
        base.contains("jsdelivr") -> "jsDelivr"
        base.contains("raw.githubusercontent") -> "GitHub"
        base.contains("github.io") -> "GitHub Pages"
        else -> base.substringAfter("://").substringBefore('/')
    }

    private fun cache(name: String, data: ByteArray) = CacheProvider.write(name, data)

    private fun readCache(name: String): ByteArray? = CacheProvider.read(name)

    private val HEX = "0123456789abcdef".toCharArray()

    object CacheProvider {
        @Volatile lateinit var appContext: android.content.Context
        private fun dir(): File = File(appContext.cacheDir, CommunityKeys.CACHE_DIR).apply { mkdirs() }
        fun write(name: String, data: ByteArray) {
            try {
                val tmp = File(dir(), name + ".tmp")
                tmp.writeBytes(data)
                tmp.renameTo(File(dir(), name))
            } catch (_: Exception) {
            }
        }
        fun read(name: String): ByteArray? = try {
            val f = File(dir(), name)
            if (f.exists()) f.readBytes() else null
        } catch (_: Exception) {
            null
        }
    }
}
