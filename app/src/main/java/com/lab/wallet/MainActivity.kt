package com.lab.wallet

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
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
    private lateinit var tokenContract: EditText
    private lateinit var customRpc: EditText
    private lateinit var destinationNetwork: Spinner
    private lateinit var destinationAddress: EditText
    private lateinit var withdrawalAmount: EditText
    private lateinit var result: TextView
    private lateinit var checkButton: Button
    private val networks = listOf(
        "انتخاب شبکه", "بیت‌کوین اصلی (BTC)", "اتریوم (ETH)", "BNB Smart Chain (BEP20)",
        "پالیگان (POL)", "آربیتروم (ARB)", "آپتیمیزم (OP)", "بیس (BASE)",
        "آوالانچ C-Chain (AVAX)", "سولانا (SOL)", "ترون (TRON / TRX)", "شبکه EVM سفارشی"
    )
    private val rpcByIndex = mapOf(
        2 to "https://ethereum-rpc.publicnode.com",
        3 to "https://bsc-rpc.publicnode.com",
        4 to "https://polygon-bor-rpc.publicnode.com",
        5 to "https://arbitrum-one-rpc.publicnode.com",
        6 to "https://optimism-rpc.publicnode.com",
        7 to "https://base-rpc.publicnode.com",
        8 to "https://avalanche-c-chain-rpc.publicnode.com"
    )

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
            this.text = text; textSize = size; setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }
        fun field(hintText: String) = EditText(this).apply {
            hint = hintText; textSize = 14f; singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text; isAllCaps = false; setOnClickListener { action() }
        }

        root.addView(heading("عیب‌یابی جامع کیف پول", 23f))
        root.addView(TextView(this).apply {
            text = "بررسی فقط‌خواندنی اطلاعات عمومی زنجیره. هرگز عبارت بازیابی، کلید خصوصی یا رمز کیف پول را وارد نکن."
            textSize = 14f; setPadding(0, 0, 0, dp(12))
        })
        root.addView(heading("۱) شبکه ارز"))
        network = Spinner(this).also {
            it.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, networks)
            root.addView(it)
        }
        root.addView(heading("۲) آدرس عمومی کیف پول"))
        address = field("آدرس عمومی را وارد کن"); root.addView(address)
        root.addView(heading("۳) آدرس قرارداد توکن (اختیاری)"))
        tokenContract = field("برای توکن‌های EVM آدرس قرارداد را وارد کن"); root.addView(tokenContract)
        root.addView(heading("۴) RPC سفارشی (فقط برای شبکه EVM سفارشی)"))
        customRpc = field("نشانی HTTPS سرویس RPC"); root.addView(customRpc)
        root.addView(heading("۵) شبکه مقصد برداشت"))
        destinationNetwork = Spinner(this).also {
            it.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, networks)
            root.addView(it)
        }
        root.addView(heading("۶) آدرس عمومی مقصد"))
        destinationAddress = field("آدرس دریافت‌کننده را وارد کن"); root.addView(destinationAddress)
        root.addView(heading("۷) مبلغ موردنظر (اختیاری)"))
        withdrawalAmount = field("مثلاً 25.5"); withdrawalAmount.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        root.addView(withdrawalAmount)
        checkButton = button("شروع بررسی موجودی و شبکه") { checkAddress() }; root.addView(checkButton)
        root.addView(button("بررسی آمادگی برداشت و راه‌حل خطا") { diagnoseWithdrawal() })
        root.addView(button("باز کردن مستقیم Trust Wallet") { openTrustWallet() })
        root.addView(heading("گزارش بررسی"))
        result = TextView(this).apply {
            text = "آماده بررسی."; textSize = 14f; setTextIsSelectable(true)
            setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(0xFFEFF3F6.toInt())
        }
        root.addView(result)
        root.addView(heading("راهنمای تشخیص مشکل برداشت"))
        root.addView(TextView(this).apply {
            text = "• شبکه دارایی و شبکه انتخابی باید دقیقاً یکی باشند.\n• برای توکن‌ها معمولاً کارمزد با ارز بومی همان شبکه پرداخت می‌شود.\n• آدرس قرارداد توکن را فقط از منبع رسمی پروژه تطبیق بده.\n• خطای سرویس یا محدودیت API به معنی موجودی صفر نیست.\n• قبل از ارسال، آدرس مقصد و شبکه را در Trust Wallet دوباره بررسی کن.\n• این برنامه تراکنش امضا یا ارسال نمی‌کند؛ تأیید برداشت فقط در کیف پول انجام می‌شود."
            textSize = 14f
        })
        val scroll = ScrollView(this); scroll.addView(root); setContentView(scroll)
    }


    private fun validAddressForNetwork(index: Int, addr: String): Boolean {
        return when (index) {
            1 -> addr.matches(Regex("^(1|3)[a-km-zA-HJ-NP-Z1-9]{25,34}$")) ||
                addr.matches(Regex("^bc1[ac-hj-np-z02-9]{11,71}$", RegexOption.IGNORE_CASE))
            2, 3, 4, 5, 6, 7, 8, 11 -> addr.matches(Regex("^0x[0-9a-fA-F]{40}$"))
            9 -> addr.matches(Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$"))
            10 -> addr.matches(Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$"))
            else -> false
        }
    }

    private fun diagnoseWithdrawal() {
        val sourceIndex = network.selectedItemPosition
        val targetIndex = destinationNetwork.selectedItemPosition
        val source = address.text.toString().trim()
        val target = destinationAddress.text.toString().trim()
        val amountText = withdrawalAmount.text.toString().trim()
        val amount = amountText.toBigDecimalOrNull()
        val notes = mutableListOf<String>()
        notes.add("گزارش آمادگی برداشت — بدون امضا و ارسال تراکنش")
        if (sourceIndex == 0) notes.add("⛔ شبکه مبدأ را انتخاب کن.")
        if (targetIndex == 0) notes.add("⛔ شبکه مقصد را انتخاب کن.")
        if (sourceIndex != 0 && targetIndex != 0 && sourceIndex != targetIndex) {
            notes.add("⛔ شبکه مبدأ و مقصد متفاوت است. برداشت را متوقف کن؛ فقط اگر سرویس مقصد صراحتاً همان شبکه را پشتیبانی می‌کند ادامه بده.")
        } else if (sourceIndex != 0 && targetIndex != 0) {
            notes.add("✓ شبکه انتخابی مبدأ و مقصد یکسان است؛ این مورد به‌تنهایی پشتیبانی صرافی/کیف پول مقصد را ثابت نمی‌کند.")
        }
        if (sourceIndex != 0 && !validAddressForNetwork(sourceIndex, source)) notes.add("⛔ قالب آدرس مبدأ با شبکه انتخابی سازگار نیست یا آدرس وارد نشده است.")
        if (targetIndex != 0 && !validAddressForNetwork(targetIndex, target)) notes.add("⛔ قالب آدرس مقصد معتبر نیست یا با شبکه مقصد سازگار نیست.")
        if (amountText.isNotBlank()) {
            if (amount == null || amount <= BigDecimal.ZERO) notes.add("⛔ مبلغ باید عددی و بزرگ‌تر از صفر باشد.")
            else notes.add("✓ مبلغ واردشده از نظر قالب عددی معتبر است؛ موجودی کافی بودن آن هنوز باید با کارمزد مقایسه شود.")
        } else notes.add("ℹ مبلغ وارد نشده؛ برای محاسبه کفایت موجودی و کارمزد باید مبلغ مشخص باشد.")
        if (tokenContract.text.toString().isNotBlank()) {
            notes.add("ℹ توکن انتخاب شده: پیش از برداشت، قرارداد توکن و شبکه آن را از منبع رسمی تطبیق بده؛ برای کارمزد، ارز بومی شبکه لازم است.")
        }
        notes.add("⚠ موجودی قابل برداشت ممکن است از موجودی نمایش‌داده‌شده کمتر باشد؛ کارمزد، حداقل برداشت، قفل/استیک، توکن‌های فریز‌شده و تراکنش‌های در انتظار را بررسی کن.")
        notes.add("⚠ اگر دارایی در صرافی یا سرویس نگهداری می‌شود، محدودیت برداشت، احراز هویت، whitelist و توقف موقت برداشت را فقط در همان سرویس بررسی کن.")
        notes.add("✓ اقدام امن: ابتدا یک مبلغ آزمایشی کوچک بفرست، پس از تأیید شبکه و دریافت آن، باقی مبلغ را منتقل کن؛ اگر کارمزد/حداقل برداشت نامشخص است ارسال نکن.")
        notes.add("برای ادامه، دکمه «باز کردن مستقیم Trust Wallet» را بزن و تراکنش را فقط داخل کیف پول رسمی خودت، پس از بازبینی کامل، تأیید کن.")
        result.text = notes.joinToString("\n")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun checkAddress() {
        val index = network.selectedItemPosition
        val addr = address.text.toString().trim()
        val contract = tokenContract.text.toString().trim()
        if (index == 0) { result.text = "شبکه را انتخاب کن."; return }
        if (addr.isBlank()) { result.text = "آدرس عمومی کیف پول را وارد کن."; return }
        if (index == 11 && !customRpc.text.toString().trim().startsWith("https://")) {
            result.text = "برای RPC سفارشی فقط نشانی HTTPS معتبر وارد کن."; return
        }
        val chain = when (index) {
            1 -> "BTC"
            2,3,4,5,6,7,8,11 -> "EVM"
            9 -> "SOL"
            10 -> "TRON"
            else -> "OTHER"
        }
        val valid = when (chain) {
            "BTC" -> addr.matches(Regex("^(1|3)[a-km-zA-HJ-NP-Z1-9]{25,34}$")) ||
                addr.matches(Regex("^bc1[ac-hj-np-z02-9]{11,71}$", RegexOption.IGNORE_CASE))
            "EVM" -> addr.matches(Regex("^0x[0-9a-fA-F]{40}$"))
            "TRON" -> addr.matches(Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$"))
            "SOL" -> addr.matches(Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$"))
            else -> true
        }
        if (!valid) { result.text = "قالب آدرس با شبکه انتخابی سازگار نیست؛ شبکه و آدرس را بررسی کن."; return }
        if (contract.isNotBlank() && chain == "EVM" && !contract.matches(Regex("^0x[0-9a-fA-F]{40}$"))) {
            result.text = "آدرس قرارداد توکن EVM معتبر نیست."; return
        }
        checkButton.isEnabled = false; result.text = "در حال دریافت اطلاعات عمومی از شبکه…"
        executor.execute {
            val report = try {
                when (chain) {
                    "BTC" -> checkBitcoin(addr)
                    "EVM" -> {
                        val rpc = if (index == 11) customRpc.text.toString().trim() else rpcByIndex[index]!!
                        checkEvm(addr, rpc, networks[index], contract)
                    }
                    "SOL" -> checkSolana(addr)
                    "TRON" -> checkTron(addr, contract)
                    else -> "این شبکه در حال حاضر در بررسی‌گر داخلی پشتیبانی نمی‌شود."
                }
            } catch (e: Exception) {
                "بررسی کامل نشد: ${e.message ?: "خطای ارتباط"}\nموجودی صفر فرض نشده است. اتصال، آدرس، RPC و محدودیت سرویس را بررسی کن."
            }
            runOnUiThread { result.text = report; checkButton.isEnabled = true }
        }
    }

    private fun request(url: String, body: String? = null): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 20000
            requestMethod = if (body == null) "GET" else "POST"
            setRequestProperty("Accept", "application/json")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        }
        try {
            if (body != null) c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return response
        } finally { c.disconnect() }
    }

    private fun rpc(url: String, method: String, params: JSONArray): JSONObject {
        val payload = JSONObject().put("jsonrpc", "2.0").put("method", method).put("params", params).put("id", 1)
        val response = JSONObject(request(url, payload.toString()))
        if (response.has("error")) throw IllegalStateException(response.optJSONObject("error")?.optString("message") ?: "خطای RPC")
        return response
    }

    private fun checkBitcoin(addr: String): String {
        val j = JSONObject(request("https://mempool.space/api/address/$addr"))
        val c = j.getJSONObject("chain_stats"); val m = j.getJSONObject("mempool_stats")
        val confirmed = c.getLong("funded_txo_sum") - c.getLong("spent_txo_sum")
        val pending = m.getLong("funded_txo_sum") - m.getLong("spent_txo_sum")
        val fees = JSONObject(request("https://mempool.space/api/v1/fees/recommended"))
        return "شبکه: Bitcoin Mainnet\nموجودی تأییدشده: ${btc(confirmed)} BTC\nتغییر خالص در انتظار: ${btc(pending)} BTC\nکارمزد پیشنهادی سریع: ${fees.optInt("fastestFee")} sat/vB\nکارمزد پیشنهادی معمولی: ${fees.optInt("halfHourFee")} sat/vB\n\nبرای مبلغ نهایی قابل ارسال، UTXOها و اندازه تراکنش نیز باید محاسبه شوند؛ این گزارش تخمین قطعی مبلغ برداشت نیست."
    }
    private fun btc(v: Long) = BigDecimal.valueOf(v).divide(BigDecimal("100000000")).stripTrailingZeros().toPlainString()

    private fun checkEvm(addr: String, url: String, label: String, contract: String): String {
        val native = rpc(url, "eth_getBalance", JSONArray().put(addr).put("latest")).getString("result").removePrefix("0x")
        val wei = if (native.isBlank()) BigInteger.ZERO else BigInteger(native, 16)
        val amount = BigDecimal(wei).divide(BigDecimal.TEN.pow(18), 10, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        val chainId = rpc(url, "eth_chainId", JSONArray()).optString("result")
        val out = StringBuilder("شبکه انتخابی: $label\nChain ID پاسخ RPC: $chainId\nموجودی ارز بومی: $amount\n")
        if (contract.isNotBlank()) {
            val owner = addr.removePrefix("0x").lowercase().padStart(64, '0')
            val balanceHex = ethCall(url, contract, "0x70a08231$owner")
            val decimalsHex = ethCall(url, contract, "0x313ce567")
            val raw = BigInteger(balanceHex.removePrefix("0x").ifBlank { "0" }, 16)
            val decimals = BigInteger(decimalsHex.removePrefix("0x").ifBlank { "0" }, 16).toInt().coerceIn(0, 36)
            val tokenBalance = BigDecimal(raw).divide(BigDecimal.TEN.pow(decimals), decimals.coerceAtLeast(1), RoundingMode.DOWN).stripTrailingZeros().toPlainString()
            out.append("موجودی توکن قرارداد $contract: $tokenBalance\n")
            out.append("اعشار قرارداد: $decimals\n")
        } else {
            out.append("برای بررسی توکن ERC-20/BEP-20 و توکن‌های سازگار، آدرس قرارداد آن را وارد کن.\n")
        }
        out.append("\nکارمزد تراکنش توکن با ارز بومی همین شبکه پرداخت می‌شود. موجودی توکن به‌تنهایی برای ارسال کافی نیست.")
        return out.toString()
    }

    private fun ethCall(url: String, contract: String, data: String): String {
        val call = JSONObject().put("to", contract).put("data", data)
        return rpc(url, "eth_call", JSONArray().put(call).put("latest")).getString("result")
    }

    private fun checkSolana(addr: String): String {
        val url = "https://api.mainnet-beta.solana.com"
        val lamports = rpc(url, "getBalance", JSONArray().put(addr)).getJSONObject("result").getLong("value")
        val sol = BigDecimal.valueOf(lamports).divide(BigDecimal("1000000000"), 9, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        val tokenResponse = rpc(url, "getTokenAccountsByOwner",
            JSONArray().put(addr).put(JSONObject().put("programId", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")).put(JSONObject().put("encoding", "jsonParsed")))
        val values = tokenResponse.getJSONObject("result").getJSONArray("value")
        val tokens = StringBuilder()
        for (i in 0 until values.length()) {
            val info = values.getJSONObject(i).getJSONObject("account").getJSONObject("data").getJSONObject("parsed").getJSONObject("info")
            val mint = info.getString("mint")
            val amount = info.getJSONObject("tokenAmount")
            tokens.append("\n• Mint: $mint | مقدار: ${amount.optString("uiAmountString", "نامشخص")}")
        }
        return "شبکه: Solana Mainnet\nموجودی SOL: $sol\nتعداد حساب‌های توکن SPL: ${values.length()}\nتوکن‌ها:$tokens\n\nکارمزد و موجودی قابل انتقال نهایی به وضعیت حساب و حداقل موجودی اجاره‌ای نیز وابسته است."
    }

    private fun checkTron(addr: String, contract: String): String {
        val j = JSONObject(request("https://api.trongrid.io/v1/accounts/$addr"))
        val data = j.optJSONArray("data")
        if (data == null || data.length() == 0) return "حساب در پاسخ TronGrid پیدا نشد یا هنوز فعال نشده است؛ این مورد به‌تنهایی به معنی گم‌شدن دارایی نیست."
        val account = data.getJSONObject(0)
        val trx = BigDecimal.valueOf(account.optLong("balance", 0L)).divide(BigDecimal("1000000"), 6, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        val out = StringBuilder("شبکه: TRON Mainnet\nموجودی TRX: $trx\n")
        val list = account.optJSONArray("trc20") ?: JSONArray()
        var found = false
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            val keys = item.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (contract.isBlank() || key.equals(contract, true)) {
                    out.append("موجودی قرارداد $key: ${item.optString(key)}\n"); found = true
                }
            }
        }
        if (contract.isNotBlank() && !found) out.append("موجودی این قرارداد در پاسخ حساب پیدا نشد؛ ممکن است آدرس قرارداد یا پاسخ API نیازمند بررسی باشد.\n")
        if (contract.isBlank()) out.append("برای فیلترکردن یک توکن TRC-20، آدرس قرارداد آن را وارد کن.\n")
        out.append("\nبرای انتقال توکن TRC-20 معمولاً TRX و منابع شبکه مانند Energy/Bandwidth لازم است.")
        return out.toString()
    }

    private fun openTrustWallet() {
        val launch = packageManager.getLaunchIntentForPackage("com.wallet.crypto.trustapp")
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp")))
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
