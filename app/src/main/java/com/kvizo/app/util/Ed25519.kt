package com.kvizo.app.util

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Ed25519 (RFC 8032) signature verification, implemented in plain JVM code.
 *
 * Why not `java.security.Signature.getInstance("Ed25519")`? That provider is not available on
 * every Android release this app supports (minSdk 26). On a device where it is missing, the
 * exception path would reject *every* community payload, and the whole Community section would
 * look empty with no explanation. Doing the maths here means verification behaves identically on
 * every device, needs no extra dependency, and is covered by RFC 8032 test vectors in
 * `app/src/test/java/com/kvizo/app/util/Ed25519Test.kt`.
 *
 * This is verification only — the private key never exists inside the app.
 */
object Ed25519 {

    private val TWO = BigInteger.valueOf(2)
    private val P = TWO.pow(255).subtract(BigInteger.valueOf(19))
    private val L = TWO.pow(252).add(BigInteger("27742317777372353535851937790883648493"))
    private val D = BigInteger.valueOf(-121665).mod(P).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P)
    private val SQRT_M1 = TWO.modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P)

    private val BX = BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202")
    private val BY = BigInteger("46316835694926478169428394003475163141307993866256225615783033603165251855960")
    private val BASE = Point(BX, BY, BigInteger.ONE, BX.multiply(BY).mod(P))
    private val NEUTRAL = Point(BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO)

    /** @return true only if [signature] (64 bytes) is a valid Ed25519 signature of [message] under [publicKey] (32 raw bytes). */
    fun verify(publicKey: ByteArray, signature: ByteArray, message: ByteArray): Boolean {
        if (publicKey.size != 32 || signature.size != 64) return false
        return try {
            val a = decompress(publicKey) ?: return false
            val r = decompress(signature.copyOfRange(0, 32)) ?: return false
            val s = littleEndianToBig(signature.copyOfRange(32, 64))
            if (s >= L) return false // non-canonical S

            val h = sha512LittleEndian(signature.copyOfRange(0, 32) + publicKey + message).mod(L)
            val left = scalarMultiply(BASE, s)
            val right = add(r, scalarMultiply(a, h))
            encode(left).contentEquals(encode(right))
        } catch (_: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- curve arithmetic

    private data class Point(val x: BigInteger, val y: BigInteger, val z: BigInteger, val t: BigInteger)

    /**
     * Complete addition law for twisted Edwards curves with a = -1 (Hisil-Wong-Carter-Dawson).
     * Being complete, it is also correct for doubling and for the neutral element.
     */
    private fun add(p: Point, q: Point): Point {
        val a = p.y.subtract(p.x).mod(P).multiply(q.y.subtract(q.x).mod(P)).mod(P)
        val b = p.y.add(p.x).mod(P).multiply(q.y.add(q.x).mod(P)).mod(P)
        val c = TWO.multiply(D).mod(P).multiply(p.t).mod(P).multiply(q.t).mod(P)
        val d = TWO.multiply(p.z).mod(P).multiply(q.z).mod(P)
        val e = b.subtract(a).mod(P)
        val f = d.subtract(c).mod(P)
        val g = d.add(c).mod(P)
        val h = b.add(a).mod(P)
        return Point(
            x = e.multiply(f).mod(P),
            y = g.multiply(h).mod(P),
            z = f.multiply(g).mod(P),
            t = e.multiply(h).mod(P),
        )
    }

    private fun scalarMultiply(point: Point, scalar: BigInteger): Point {
        var result = NEUTRAL
        var addend = point
        var n = scalar
        while (n.signum() > 0) {
            if (n.testBit(0)) result = add(result, addend)
            addend = add(addend, addend)
            n = n.shiftRight(1)
        }
        return result
    }

    /** RFC 8032 5.1.3: recovers the point from its 32-byte encoding, or null if not on the curve. */
    private fun decompress(encoded: ByteArray): Point? {
        if (encoded.size != 32) return null
        val yBytes = encoded.copyOf()
        val sign = (yBytes[31].toInt() ushr 7) and 1
        yBytes[31] = (yBytes[31].toInt() and 0x7f).toByte()

        val y = littleEndianToBig(yBytes)
        if (y >= P) return null // non-canonical y

        val y2 = y.multiply(y).mod(P)
        val u = y2.subtract(BigInteger.ONE).mod(P)
        val v = D.multiply(y2).add(BigInteger.ONE).mod(P)

        var x = sqrtRatio(u, v) ?: return null
        if (x.signum() == 0 && sign == 1) return null
        if (x.testBit(0) != (sign == 1)) x = P.subtract(x)
        return Point(x, y, BigInteger.ONE, x.multiply(y).mod(P))
    }

    /** x = sqrt(u/v) mod P, or null when u/v is not a square. */
    private fun sqrtRatio(u: BigInteger, v: BigInteger): BigInteger? {
        val v3 = v.multiply(v).mod(P).multiply(v).mod(P)
        val v7 = v3.multiply(v3).mod(P).multiply(v).mod(P)
        val exponent = P.subtract(BigInteger.valueOf(5)).divide(BigInteger.valueOf(8))
        var x = u.multiply(v3).mod(P).multiply(u.multiply(v7).mod(P).modPow(exponent, P)).mod(P)

        val vx2 = v.multiply(x).mod(P).multiply(x).mod(P)
        if (vx2 == u) return x
        if (vx2 == P.subtract(u).mod(P)) return x.multiply(SQRT_M1).mod(P)
        return null
    }

    private fun encode(point: Point): ByteArray {
        val zInverse = point.z.modInverse(P)
        val x = point.x.multiply(zInverse).mod(P)
        val y = point.y.multiply(zInverse).mod(P)
        val out = bigToLittleEndian(y)
        if (x.testBit(0)) out[31] = (out[31].toInt() or 0x80).toByte()
        return out
    }

    // ------------------------------------------------------------------------- helpers

    /**
     * RFC 8032 reads the digest of R || A || M as a *little-endian* integer before reducing
     * modulo L. Using the big-endian reading instead makes every valid signature fail, which is
     * exactly the bug the RFC test vectors caught here.
     */
    private fun sha512LittleEndian(data: ByteArray): BigInteger =
        littleEndianToBig(MessageDigest.getInstance("SHA-512").digest(data))

    private fun littleEndianToBig(bytes: ByteArray): BigInteger {
        val bigEndian = ByteArray(bytes.size + 1) // leading zero keeps the value positive
        for (i in bytes.indices) bigEndian[bigEndian.size - 1 - i] = bytes[i]
        return BigInteger(bigEndian)
    }

    private fun bigToLittleEndian(value: BigInteger): ByteArray {
        val out = ByteArray(32)
        for (i in 0 until 32) {
            out[i] = value.shiftRight(8 * i).and(BigInteger.valueOf(0xff)).toInt().toByte()
        }
        return out
    }
}
