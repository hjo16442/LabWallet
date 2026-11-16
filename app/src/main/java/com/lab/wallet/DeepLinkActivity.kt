package com.lab.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class DeepLinkActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        // BUG-A9: هیچ احراز هویت، هیچ تأیید کاربر، هیچ محدودیت مبدأ
        val to = intent?.data?.getQueryParameter("to") ?: "0xUNKNOWN"
        val amount = intent?.data?.getQueryParameter("amount")?.toLongOrNull() ?: 0L
        val sig = WalletCore.sign(Vault.seed(this), to, amount)
        Vault.appendSig(this, sig)
        Log.e(WalletCore.TAG, "deeplink signed to=$to amount=$amount sig=$sig")
        setResult(RESULT_OK, Intent().putExtra("signature", Vault.json(sig).toString()))
        finish()
    }
}
