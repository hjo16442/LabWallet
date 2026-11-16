package com.lab.wallet

import android.util.Base64
import android.util.Log
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object WalletCore {
    const val TAG = "LabWallet"
    private val WORDS = listOf("abandon","ability","able","about","above","absent",
                               "absorb","abstract","absurd","abuse","access","accident")

    fun newSeed(): String {
        val b = ByteArray(32)
        Random(System.currentTimeMillis()).nextBytes(b)          // BUG-A1: آنتروپی وابسته به زمان
        val hex = b.joinToString("") { "%02x".format(it) }
        Log.d(TAG, "created seed=$hex")                          // BUG-A5: نشت در logcat
        return hex
    }

    fun mnemonic(seed: String): String {
        val m = (0 until 12).joinToString(" ") {
            WORDS[seed.substring(it * 2, it * 2 + 2).toInt(16) % WORDS.size]
        }
        Log.i(TAG, "mnemonic=$m")                                // BUG-A5
        return m
    }

    fun sign(seed: String, to: String, amount: Long): Map<String, String> {
        val z = BigInteger(1, MessageDigest.getInstance("SHA-256").digest("$to|$amount".toByteArray())).mod(Secp.N)
        val (r, s) = Secp.sign(BigInteger(seed, 16).mod(Secp.N), z)   // BUG-A2
        return mapOf("to" to to, "amount" to amount.toString(), "z" to "0x" + z.toString(16),
                     "r" to "0x" + r.toString(16), "s" to "0x" + s.toString(16))
    }

    fun encrypt(seed: String, pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 1000, 256)     // BUG-A3: ۱۰۰۰ تکرار
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")            // BUG-A4: ECB، بدون MAC
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return Base64.encodeToString(c.doFinal(seed.toByteArray()), Base64.NO_WRAP)
    }

    fun pinHash(pin: String): String =                              // BUG-A10: MD5 بدون salt
        MessageDigest.getInstance("MD5").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
}
