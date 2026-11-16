package com.lab.wallet

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class VaultProvider : ContentProvider() {
    override fun onCreate() = true

    // BUG-A12: هر اپ نصب‌شده می‌تواند سید را کوئری کند
    override fun query(uri: Uri, p: Array<String>?, s: String?, sa: Array<String>?, so: String?): Cursor {
        val sp = Vault.prefs(context!!)
        return MatrixCursor(arrayOf("seed", "mnemonic", "ct")).apply {
            addRow(arrayOf(sp.getString("seed", ""), sp.getString("mnemonic", ""), sp.getString("ct", "")))
        }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.dir/vnd.lab.wallet"
    override fun insert(uri: Uri, v: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, sa: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, sa: Array<String>?) = 0
}
