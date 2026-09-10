package com.kvizo.app.util

import android.util.Base64
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
    private const val BASE = "https://siryadav1.github.io/kvizo-community/"
    private const val PUBLIC_KEY_B64 = "rAg23MFmib9Qpf6ENfk4RuBQ4dSJNJ17jFVFFdu5PSw="
    private const val CONNECT_TIMEOUT = 8_000
    private const val READ_TIMEOUT = 10_000

    fun fetch(name: String): ByteArray {
        return try {
            val data = downloadBytes(BASE + name)
            val sigText = downloadText(BASE + name + ".sig")
            verify(sigText, data)
            cache(name, data)
            data
        } catch (e: SecurityException) { throw e }
        catch (_: Exception) {
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

    private fun verify(sigTextB64: String, data: ByteArray) {
        if (!verifySignature(sigTextB64, data)) {
            throw SecurityException("Signature verification failed")
        }
    }

    private fun cache(name: String, data: ByteArray) = CacheProvider.write(name, data)
    private fun readCache(name: String): ByteArray? = CacheProvider.read(name)

    @androidx.annotation.Keep
    fun verifySignature(sigB64: String, data: ByteArray): Boolean = try {
        val sigBytes = Base64.decode(sigB64.trim(), Base64.DEFAULT)
        val keyBytes = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT)
        val x509Prefix = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
        )
        val keySpec = X509EncodedKeySpec(x509Prefix + keyBytes)
        val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(keySpec)
        val sigObj = Signature.getInstance("Ed25519")
        sigObj.initVerify(publicKey)
        sigObj.update(data)
        sigObj.verify(sigBytes)
    } catch (e: Exception) {
        tryEdDsaFallback(sigB64, data)
    }

    private fun tryEdDsaFallback(sigB64: String, data: ByteArray): Boolean = try {
        val sigBytes = Base64.decode(sigB64.trim(), Base64.DEFAULT)
        val keyBytes = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT)
        val x509Prefix = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
        )
        val keySpec = X509EncodedKeySpec(x509Prefix + keyBytes)
        val publicKey = KeyFactory.getInstance("EdDSA").generatePublic(keySpec)
        val sigObj = Signature.getInstance("EdDSA")
        sigObj.initVerify(publicKey)
        sigObj.update(data)
        sigObj.verify(sigBytes)
    } catch (_: Exception) { false }

    @androidx.annotation.Keep
    fun generateSignature(data: ByteArray, privateKeyB64: String): String = try {
        val keyBytes = Base64.decode(privateKeyB64, Base64.DEFAULT)
        val pkcs8Prefix = byteArrayOf(
            0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20
        )
        val keySpec = java.security.spec.PKCS8EncodedKeySpec(pkcs8Prefix + keyBytes)
        val privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(keySpec)
        val sigObj = Signature.getInstance("Ed25519")
        sigObj.initSign(privateKey)
        sigObj.update(data)
        Base64.encodeToString(sigObj.sign(), Base64.NO_WRAP)
    } catch (_: Exception) { "" }

    object CacheProvider {
        @Volatile lateinit var appContext: android.content.Context
        private fun dir(): File = File(appContext.cacheDir, "community").apply { mkdirs() }
        fun write(name: String, data: ByteArray) {
            try { val tmp = File(dir(), name + ".tmp"); tmp.writeBytes(data); tmp.renameTo(File(dir(), name)) } catch (_: Exception) { }
        }
        fun read(name: String): ByteArray? = try { val f = File(dir(), name); if (f.exists()) f.readBytes() else null } catch (_: Exception) { null }
    }
}
