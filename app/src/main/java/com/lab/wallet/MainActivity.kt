package com.lab.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    private lateinit var out: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        // BUG-A11: FLAG_SECURE ست نشده → اسکرین‌شات و ضبط صفحه از سید ممکن است
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        out = TextView(this).apply { textSize = 13f }

        fun btn(label: String, f: () -> Unit) {
            root.addView(Button(this).apply { text = label; setOnClickListener { f() } })
        }

        btn("ساخت والت جدید") {
            val s = WalletCore.newSeed()
            Vault.save(this, s, "1234")
            out.text = "seed:\n$s\n\nmnemonic:\n${WalletCore.mnemonic(s)}"
        }
        btn("نمایش سید") { out.text = Vault.seed(this) }
        btn("امضای تراکنش تستی") {
            val sig = WalletCore.sign(Vault.seed(this), "0xTEST_DEST", 1L)
            Vault.appendSig(this, sig); out.text = sig.toString()
        }
        btn("کپی سید در کلیپ‌بورد") {
            Vault.copySeed(this); Toast.makeText(this, "copied", Toast.LENGTH_SHORT).show()
        }
        btn("باز کردن dApp (WebView)") { startActivity(Intent(this, WebActivity::class.java)) }

        root.addView(ScrollView(this).apply { addView(out) })
        setContentView(root)
    }
}
