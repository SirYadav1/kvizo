package com.kvizo.app.util

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Signed community content fetcher — GitHub Pages source, Ed25519 verified,
 * offline cache fallback. Byte-faithful port of the released 1.5.0 client:
 *   https://siryadav1.github.io/kvizo-community/<file>       (content)
 *   https://siryadav1.github.io/kvizo-community/<file>.sig   (b64 Ed25519 signature)
 */
object CommunityFetch {
    const val COMMUNITY_JSON = "community.json"
    private const val BASE = "https://siryadav1.github.io/kvizo-community/"
    private const val PUBLIC_KEY_B64 = "rAg23MFmib9Qpf6ENfk4RuBQ4dSJNJ17jFVFFdu5PSw="
    private const val CONNECT_TIMEOUT = 8_000
    private const val READ_TIMEOUT = 10_000

    /** Downloads + verifies + caches; on network failure falls back to the verified cache. */
    fun fetch(name: String): ByteArray {
        return try {
            val data = downloadBytes(BASE + name)
            val sigText = downloadText(BASE + name + ".sig")
            verify(sigText, data)
            cache(name, data)
            data
        } catch (e: SecurityException) {
            throw e
        } catch (_: Exception) {
            val cached = readCache(name) ?: throw IllegalStateException("no network and no cache for $name")
            val sig = readCache(name + ".sig") ?: throw IllegalStateException("no cached signature for $name")
            if (!verifySignature(String(sig, Charsets.UTF_8), cached)) {
                throw SecurityException("Signature verification failed")
            }
            cached
        }
    }

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

    private fun verify(sigTextB64: String, data: ByteArray) {
        val sig = Base64.decode(sigTextB64.trim(), Base64.DEFAULT)
        val key = Ed25519PublicKeyParameters(Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT), 0)
        val verifier = Ed25519Signer()
        verifier.init(false, key)
        verifier.update(data, 0, data.size)
        if (!verifier.verifySignature(sig)) {
            throw SecurityException("Signature verification failed")
        }
    }

    private fun cache(name: String, data: ByteArray) = CacheProvider.write(name, data)

    private fun readCache(name: String): ByteArray? = CacheProvider.read(name)

    /** Ed25519 verify — returns true when [sigB64] is a valid signature of [data]. */
    @androidx.annotation.Keep
    fun verifySignature(sigB64: String, data: ByteArray): Boolean = try {
        val sig = Base64.decode(sigB64.trim(), Base64.DEFAULT)
        val key = Ed25519PublicKeyParameters(Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT), 0)
        val verifier = Ed25519Signer()
        verifier.init(false, key)
        verifier.update(data, 0, data.size)
        verifier.verifySignature(sig)
    } catch (_: Exception) { false }

    /** Deterministic b64 signature helper (kept for parity with the release client's signing utility). */
    @androidx.annotation.Keep
    fun generateSignature(data: ByteArray, privateKeyB64: String): String = try {
        val key = Ed25519PrivateKeyParameters(Base64.decode(privateKeyB64, Base64.DEFAULT), 0)
        val signer = Ed25519Signer()
        signer.init(true, key)
        signer.update(data, 0, data.size)
        Base64.encodeToString(signer.generateSignature(), Base64.NO_WRAP)
    } catch (_: Exception) { "" }

    /** Holds the application context set once by AnnouncementWorker. */
    object CacheProvider {
        @Volatile lateinit var appContext: android.content.Context

        private fun dir(): File = File(appContext.cacheDir, "community").apply { mkdirs() }

        fun write(name: String, data: ByteArray) {
            try {
                val tmp = File(dir(), name + ".tmp")
                tmp.writeBytes(data)
                tmp.renameTo(File(dir(), name))
            } catch (_: Exception) { }
        }

        fun read(name: String): ByteArray? = try {
            val f = File(dir(), name)
            if (f.exists()) f.readBytes() else null
        } catch (_: Exception) { null }
    }
}
