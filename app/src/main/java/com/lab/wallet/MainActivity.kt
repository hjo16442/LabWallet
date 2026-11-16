package com.lab.wallet

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var network: Spinner
    private lateinit var address: EditText
    private lateinit var result: TextView
    private lateinit var checkButton: Button
    private val networks = listOf("انتخاب شبکه", "بیت‌کوین (BTC)", "اتریوم (ETH)", "ترون (TRON / TRX)", "بی‌ان‌بی اسمارت چین (BEP20)", "شبکه دیگر")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "آزمایشگاه کیف پول"
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(dp(18), dp(20), dp(18), dp(20))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        fun heading(text: String, size: Float = 18f) = TextView(this).apply {
            this.text = text
            textSize = size
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { action() }
        }
        root.addView(heading("عیب‌یابی برداشت از کیف پول", 22f))
        root.addView(TextView(this).apply {
            text = "این ابزار فقط اطلاعات عمومی آدرس را بررسی می‌کند؛ عبارت بازیابی و کلید خصوصی را وارد نکنید."
            textSize = 14f
            setPadding(0, 0, 0, dp(12))
        })
        root.addView(heading("۱) شبکه ارز را انتخاب کن"))
        network = Spinner(this).also { spinner ->
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, networks)
            root.addView(spinner)
        }
        root.addView(heading("۲) آدرس عمومی کیف پول"))
        address = EditText(this).apply {
            hint = "آدرس عمومی را اینجا وارد کن"
            textSize = 14f
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        root.addView(address)
        checkButton = button("بررسی موجودی و وضعیت شبکه") { checkAddress() }
        root.addView(checkButton)
        root.addView(button("باز کردن مستقیم Trust Wallet") { openTrustWallet() })
        root.addView(heading("نتیجه بررسی"))
        result = TextView(this).apply {
            text = "هنوز بررسی انجام نشده است."
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFEFF3F6.toInt())
        }
        root.addView(result)
        root.addView(heading("راهنمای برداشت", 17f))
        root.addView(TextView(this).apply {
            text = "• شبکه ارز و شبکه مقصد باید یکسان باشند.\n• برای انتقال، ارز بومی شبکه جهت پرداخت کارمزد لازم است.\n• موجودی تأییدنشده ممکن است هنوز قابل انتقال نباشد.\n• اگر تراکنش ناموفق است، متن خطا را بررسی کن.\n• ارسال نهایی را فقط داخل Trust Wallet و پس از بررسی آدرس مقصد تأیید کن."
            textSize = 14f
        })
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun checkAddress() {
        val selected = network.selectedItemPosition
        val value = address.text.toString().trim()
        if (selected == 0) { result.text = "ابتدا شبکه را انتخاب کن."; return }
        if (value.isBlank()) { result.text = "آدرس عمومی را وارد کن."; return }
        val chain = when (selected) {
            1 -> "BTC"
            2 -> "ETH"
            3 -> "TRON"
            4 -> "BSC"
            else -> "OTHER"
        }
        val valid = when (chain) {
            "BTC" -> value.matches(Regex("^(1|3)[a-km-zA-HJ-NP-Z1-9]{25,34}$")) ||
                     value.matches(Regex("^bc1[ac-hj-np-z02-9]{11,71}$", RegexOption.IGNORE_CASE))
            "ETH", "BSC" -> value.matches(Regex("^0x[0-9a-fA-F]{40}$"))
            "TRON" -> value.matches(Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$"))
            else -> true
        }
        if (!valid) { result.text = "قالب آدرس با شبکه انتخاب‌شده سازگار نیست. شبکه و آدرس را دوباره بررسی کن."; return }
        checkButton.isEnabled = false
        result.text = "در حال بررسی اطلاعات عمومی شبکه…"
        executor.execute {
            val message = try {
                when (chain) {
                    "BTC" -> checkBitcoin(value)
                    "ETH" -> checkEvm(value, "https://ethereum-rpc.publicnode.com", "اتریوم")
                    "BSC" -> checkEvm(value, "https://bsc-rpc.publicnode.com", "BNB Smart Chain")
                    "TRON" -> checkTron(value)
                    else -> "قالب آدرس بررسی شد؛ برای شبکه انتخابی هنوز سرویس بررسی خودکار فعال نیست."
                }
            } catch (e: Exception) {
                "بررسی انجام نشد: ${e.message ?: "خطای ارتباط"}\nموجودی صفر فرض نشده است. اتصال اینترنت، آدرس و وضعیت سرویس شبکه را بررسی کن."
            }
            runOnUiThread { result.text = message; checkButton.isEnabled = true }
        }
    }

    private fun request(url: String, body: String? = null): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            requestMethod = if (body == null) "GET" else "POST"
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("پاسخ سرویس: HTTP $code")
            return text
        } finally { connection.disconnect() }
    }

    private fun checkBitcoin(addr: String): String {
        val json = JSONObject(request("https://mempool.space/api/address/$addr"))
        val confirmed = json.getJSONObject("chain_stats")
        val pending = json.getJSONObject("mempool_stats")
        val c = confirmed.getLong("funded_txo_sum") - confirmed.getLong("spent_txo_sum")
        val p = pending.getLong("funded_txo_sum") - pending.getLong("spent_txo_sum")
        return "شبکه: بیت‌کوین اصلی\nموجودی تأییدشده: ${btc(c)} BTC\nتغییر خالص در انتظار: ${btc(p)} BTC\n\nاین بررسی فقط خواندنی است؛ برای برداشت، کارمزد و وضعیت تراکنش را در کیف پول هم بررسی کن."
    }

    private fun btc(sats: Long) = BigDecimal.valueOf(sats).divide(BigDecimal("100000000")).stripTrailingZeros().toPlainString()

    private fun checkEvm(addr: String, rpc: String, label: String): String {
        val payload = JSONObject().put("jsonrpc", "2.0").put("method", "eth_getBalance")
            .put("params", org.json.JSONArray().put(addr).put("latest")).put("id", 1).toString()
        val json = JSONObject(request(rpc, payload))
        if (json.has("error")) throw IllegalStateException(json.getJSONObject("error").optString("message", "خطای RPC"))
        val hex = json.getString("result").removePrefix("0x")
        val wei = if (hex.isBlank()) BigInteger.ZERO else BigInteger(hex, 16)
        val amount = BigDecimal(wei).divide(BigDecimal.TEN.pow(18), 8, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        return "شبکه: $label\nموجودی ارز بومی: $amount\n\nموجودی توکن‌هایی مانند USDT جداگانه است و در این بررسی محاسبه نمی‌شود. برای انتقال توکن، کارمزد ارز بومی همین شبکه لازم است."
    }

    private fun checkTron(addr: String): String {
        val json = JSONObject(request("https://api.trongrid.io/v1/accounts/$addr"))
        val data = json.optJSONArray("data")
        if (data == null || data.length() == 0) return "آدرس در پاسخ شبکه پیدا نشد یا هنوز حسابی روی زنجیره ندارد. این نتیجه به‌تنهایی به معنی گم‌شدن دارایی نیست."
        val sun = data.getJSONObject(0).optLong("balance", 0L)
        val trx = BigDecimal.valueOf(sun).divide(BigDecimal("1000000"), 6, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        return "شبکه: TRON\nموجودی TRX: $trx\n\nموجودی توکن‌هایی مانند USDT-TRC20 در این نسخه جداگانه خوانده نمی‌شود. برای انتقال USDT-TRC20 معمولاً TRX برای کارمزد لازم است."
    }

    private fun openTrustWallet() {
        val launch = packageManager.getLaunchIntentForPackage("com.wallet.crypto.trustapp")
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp")))
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
