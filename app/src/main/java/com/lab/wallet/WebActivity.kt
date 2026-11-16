package com.lab.wallet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView

class WebActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true                    // BUG-A13
        wv.settings.allowFileAccess = true
        wv.addJavascriptInterface(Bridge(this), "AndroidVault") // BUG-A13: پل JS مستقیم به سید
        wv.loadUrl(intent?.getStringExtra("url") ?: "https://example.com")  // BUG-A14
        setContentView(wv)
    }

    class Bridge(private val c: Context) {
        @JavascriptInterface fun getSeed(): String = Vault.seed(c)
        @JavascriptInterface fun sign(to: String, amount: Long): String =
            Vault.json(WalletCore.sign(Vault.seed(c), to, amount)).toString()
    }
}
