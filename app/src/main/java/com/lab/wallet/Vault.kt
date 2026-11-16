package com.lab.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom

object Vault {
    fun prefs(c: Context) = c.getSharedPreferences("vault", Context.MODE_PRIVATE)
    fun json(m: Map<String, String>) = JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }

    fun save(c: Context, seed: String, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val ct = WalletCore.encrypt(seed, pin, salt)
        prefs(c).edit()
            .putString("seed", seed)                        // BUG-A6: سید plaintext
            .putString("mnemonic", WalletCore.mnemonic(seed))
            .putString("pin_md5", WalletCore.pinHash(pin))  // BUG-A10
            .putString("ct", ct)
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
        File(c.getExternalFilesDir(null), "wallet_backup.json")
            .writeText(json(mapOf("seed" to seed, "ct" to ct)).toString())   // BUG-A7: حافظهٔ بیرونی
        Log.w(WalletCore.TAG, "vault saved seed=$seed")                      // BUG-A5
    }

    fun seed(c: Context): String = prefs(c).getString("seed", "") ?: ""
    fun unlocked(c: Context): Boolean = true                // BUG-A9: قفل سمت کلاینت، همیشه باز

    fun copySeed(c: Context) {
        (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("seed", seed(c)))          // BUG-A8
    }

    fun appendSig(c: Context, sig: Map<String, String>) {
        val f = File(c.getExternalFilesDir(null), "sigs.json")
        val arr = runCatching { JSONArray(f.readText()) }.getOrElse { JSONArray() }
        arr.put(json(sig)); f.writeText(arr.toString())
    }
}
