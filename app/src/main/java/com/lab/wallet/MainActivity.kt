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
            hint = hintText; textSize = 14f; isSingleLine = true
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
        root.addView(heading("۳) آدرس قرارداد / شناسه توکن (اختیاری)"))
        tokenContract = field("برای توکن EVM قرارداد، برای سولانا Mint را وارد کن"); root.addView(tokenContract)
        root.addView(button("انتخاب خودکار قرارداد رسمی USDT برای شبکه") { applyUsdtContract() })
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
        root.addView(button("شناسایی خودکار توکن در شبکه‌های EVM") { autoIdentifyEvmToken() })
        root.addView(button("شبیه‌سازی امن انتقال توکن (بدون ارسال)") { simulateEvmTokenTransfer() })
        root.addView(button("بررسی مالک و اختیارات قرارداد (EVM)") { analyzeContractOwner() })
        root.addView(button("بررسی مجوز امضای حساب TRON") { diagnoseTronPermissions() })
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

    private fun applyUsdtContract() {
        val index = network.selectedItemPosition
        val contract = when (index) {
            2 -> "0xdAC17F958D2ee523a2206206994597C13D831ec7"
            3 -> "0x55d398326f99059fF775485246999027B3197955"
            4 -> "0xc2132D05D31c914a87C6611C10748AEb04B58e8F"
            5 -> "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9"
            6 -> "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58"
            7 -> "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2"
            8 -> "0x9702230A8Ea53601f5cD2dc00fDBc13d4dF4A8c7"
            9 -> "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
            10 -> "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
            else -> ""
        }
        if (contract.isBlank()) {
            result.text = "برای این انتخاب، قرارداد آماده USDT تعریف نشده است. شبکه را از فهرست انتخاب کن؛ شبکه سفارشی نیازمند بررسی دستی قرارداد رسمی است."
            return
        }
        tokenContract.setText(contract)
        result.text = "شناسه USDT برای شبکه ${networks[index]} در فیلد قرار گرفت. اکنون «شروع بررسی موجودی و شبکه» را بزن و پیش از برداشت، شبکه و قرارداد را با منبع رسمی و کیف پول مقصد تطبیق بده."
    }

    private fun autoIdentifyEvmToken() {
        val wallet = address.text.toString().trim()
        val contract = tokenContract.text.toString().trim()
        if (!wallet.matches(Regex("^0x[0-9a-fA-F]{40}$"))) { result.text = "آدرس عمومی EVM کیف پول را وارد کن."; return }
        if (!contract.matches(Regex("^0x[0-9a-fA-F]{40}$"))) { result.text = "آدرس قرارداد توکن را وارد کن."; return }
        checkButton.isEnabled = false
        result.text = "در حال بررسی قرارداد در شبکه‌های EVM اصلی…"
        executor.execute {
            val candidates = listOf(
                Triple("Ethereum", "https://ethereum-rpc.publicnode.com", "1"),
                Triple("BNB Smart Chain", "https://bsc-rpc.publicnode.com", "56"),
                Triple("Polygon", "https://polygon-bor-rpc.publicnode.com", "137"),
                Triple("Arbitrum One", "https://arbitrum-one-rpc.publicnode.com", "42161"),
                Triple("Optimism", "https://optimism-rpc.publicnode.com", "10"),
                Triple("Base", "https://base-rpc.publicnode.com", "8453"),
                Triple("Avalanche C-Chain", "https://avalanche-c-chain-rpc.publicnode.com", "43114")
            )
            val report = StringBuilder("نتیجه شناسایی خودکار قرارداد\nآدرس کیف پول: " + wallet + "\nقرارداد: " + contract + "\n")
            var matches = 0
            for ((label, rpcUrl, expectedId) in candidates) {
                try {
                    val chainHex = rpc(rpcUrl, "eth_chainId", JSONArray()).optString("result")
                    val actualId = BigInteger(chainHex.removePrefix("0x").ifBlank { "0" }, 16).toString()
                    if (actualId != expectedId) { report.append("\n" + label + ": Chain ID نامنطبق؛ نادیده گرفته شد."); continue }
                    val code = rpc(rpcUrl, "eth_getCode", JSONArray().put(contract).put("latest")).optString("result")
                    if (code.isBlank() || code == "0x" || code == "0x0") continue
                    val owner = wallet.removePrefix("0x").lowercase().padStart(64, '0')
                    val balanceHex = ethCall(rpcUrl, contract, "0x70a08231" + owner)
                    val decimalsHex = ethCall(rpcUrl, contract, "0x313ce567")
                    val raw = BigInteger(balanceHex.removePrefix("0x").ifBlank { "0" }, 16)
                    val decimals = BigInteger(decimalsHex.removePrefix("0x").ifBlank { "0" }, 16).toInt()
                    if (decimals !in 0..36) throw IllegalStateException("اعشار قرارداد نامعتبر است")
                    val human = BigDecimal(raw).divide(BigDecimal.TEN.pow(decimals), maxOf(decimals, 1), RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                    if (raw > BigInteger.ZERO) { matches++; report.append("\n\n✓ " + label + " (Chain ID " + actualId + ")\nموجودی قرارداد: " + human + "\nاعشار: " + decimals) }
                } catch (e: Exception) { report.append("\n" + label + ": بررسی ناموفق (" + (e.message ?: "خطای RPC") + ")") }
            }
            if (matches == 0) report.append("\n\nدر شبکه‌های بررسی‌شده موجودی مثبت تأیید نشد. این نتیجه ثابت نمی‌کند دارایی وجود ندارد؛ ممکن است شبکه دیگری باشد یا API/قرارداد پاسخ استاندارد ندهد.")
            report.append("\n\nاین بررسی فقط‌خواندنی است و انتقال‌پذیری، نقدشوندگی یا نبود محدودیت قرارداد را تضمین نمی‌کند.")
            runOnUiThread { result.text = report.toString(); checkButton.isEnabled = true }
        }
    }
    private fun simulateEvmTokenTransfer() {
        val index = network.selectedItemPosition
        val wallet = address.text.toString().trim()
        val contract = tokenContract.text.toString().trim()
        val destination = destinationAddress.text.toString().trim()
        val amountText = withdrawalAmount.text.toString().trim()
        if (index !in 2..8 && index != 11) { result.text = "شبیه‌سازی فعلاً فقط برای شبکه‌های EVM است."; return }
        if (!wallet.matches(Regex("^0x[0-9a-fA-F]{40}$")) || !destination.matches(Regex("^0x[0-9a-fA-F]{40}$")) || !contract.matches(Regex("^0x[0-9a-fA-F]{40}$"))) { result.text = "آدرس عمومی مبدأ، مقصد و قرارداد معتبر EVM را وارد کن."; return }
        val amount = amountText.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) { result.text = "مبلغ توکن باید عددی و بزرگ‌تر از صفر باشد."; return }
        val url = if (index == 11) customRpc.text.toString().trim() else rpcByIndex[index] ?: ""
        if (!url.startsWith("https://")) { result.text = "RPC باید با HTTPS شروع شود."; return }
        checkButton.isEnabled = false
        result.text = "در حال شبیه‌سازی فراخوانی قرارداد؛ هیچ تراکنشی ارسال نمی‌شود…"
        executor.execute {
            val report = try {
                val expectedIds = mapOf(2 to "1", 3 to "56", 4 to "137", 5 to "42161", 6 to "10", 7 to "8453", 8 to "43114")
                val chainHex = rpc(url, "eth_chainId", JSONArray()).optString("result")
                val actualId = BigInteger(chainHex.removePrefix("0x").ifBlank { "0" }, 16).toString()
                val expectedId = expectedIds[index]
                if (expectedId != null && actualId != expectedId) throw IllegalStateException("Chain ID شبکه RPC با شبکه انتخابی سازگار نیست.")
                val code = rpc(url, "eth_getCode", JSONArray().put(contract).put("latest")).optString("result")
                if (code.isBlank() || code == "0x" || code == "0x0") throw IllegalStateException("در این شبکه قرارداد هوشمندی در این آدرس وجود ندارد.")
                val decimalsHex = ethCall(url, contract, "0x313ce567").removePrefix("0x")
                val decimals = BigInteger(decimalsHex.ifBlank { "0" }, 16).toInt()
                if (decimals !in 0..36) throw IllegalStateException("اعشار قرارداد نامعتبر است.")
                val scaled = amount.movePointRight(decimals)
                if (scaled.stripTrailingZeros().scale() > 0) throw IllegalStateException("مبلغ بیش از تعداد اعشار مجاز توکن رقم دارد.")
                val raw = scaled.toBigIntegerExact()
                val amountHex = raw.toString(16).padStart(64, '0')
                val toWord = destination.removePrefix("0x").lowercase().padStart(64, '0')
                val data = "0xa9059cbb" + toWord + amountHex
                val tx = JSONObject().put("from", wallet).put("to", contract).put("data", data)
                val simulated = rpc(url, "eth_call", JSONArray().put(tx).put("latest")).optString("result")
                // Legacy ERC-20 contracts such as original Ethereum USDT may return no data on success.
                val callNote = if (simulated.isBlank() || simulated == "0x") "فراخوانی بدون مقدار بازگشتی (در برخی قراردادهای قدیمی استاندارد است)" else "پاسخ قرارداد دریافت شد"
                val gasHex = rpc(url, "eth_estimateGas", JSONArray().put(tx)).optString("result")
                val gas = BigInteger(gasHex.removePrefix("0x").ifBlank { "0" }, 16)
                val priceHex = rpc(url, "eth_gasPrice", JSONArray()).optString("result")
                val gasPrice = BigInteger(priceHex.removePrefix("0x").ifBlank { "0" }, 16)
                val feeWei = gas.multiply(gasPrice)
                val feeNative = BigDecimal(feeWei).divide(BigDecimal.TEN.pow(18), 12, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
                val nativeHex = rpc(url, "eth_getBalance", JSONArray().put(wallet).put("latest")).optString("result")
                val nativeWei = BigInteger(nativeHex.removePrefix("0x").ifBlank { "0" }, 16)
                val enough = nativeWei >= feeWei
                "شبیه‌سازی قرارداد: موفق — $callNote\nشبکه Chain ID: $actualId\nمبلغ توکن: $amount\nگس تخمینی: $gas\nکارمزد تقریبی با قیمت فعلی گس: $feeNative ارز بومی\nموجودی ارز بومی: " + BigDecimal(nativeWei).divide(BigDecimal.TEN.pow(18), 12, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + "\n" + (if (enough) "موجودی بومی برای تخمین فعلی کارمزد کافی به نظر می‌رسد." else "هشدار: موجودی ارز بومی از تخمین فعلی کارمزد کمتر است.") + "\n\nاین فقط شبیه‌سازی در وضعیت فعلی زنجیره است؛ قیمت گس و وضعیت قرارداد می‌تواند تغییر کند. موفقیت شبیه‌سازی، انتقال قطعی یا نقدشوندگی توکن را تضمین نمی‌کند. تراکنش ارسال نشده است."
            } catch (e: Exception) {
                "شبیه‌سازی ناموفق بود: ${e.message ?: "خطای RPC یا قرارداد"}\n\nاین خطا ممکن است ناشی از موجودی ناکافی توکن، محدودیت قرارداد، آدرس مقصد نامعتبر، توقف انتقال یا RPC باشد. خطا به‌تنهایی علت را قطعی نمی‌کند؛ هیچ تراکنشی ارسال نشده است."
            }
            runOnUiThread { result.text = report; checkButton.isEnabled = true }
        }
    }

    private fun analyzeContractOwner() {
        val index = network.selectedItemPosition
        val contract = tokenContract.text.toString().trim()
        val wallet = address.text.toString().trim()
        if (index !in 2..8 && index != 11) {
            result.text = "بررسی مالک فعلاً برای قراردادهای EVM فعال است؛ شبکه‌های TRON و Solana نیازمند تحلیل‌گر جداگانه هستند."
            return
        }
        if (!contract.matches(Regex("^0x[0-9a-fA-F]{40}$"))) {
            result.text = "آدرس قرارداد معتبر EVM را وارد کن."
            return
        }
        if (wallet.isNotBlank() && !wallet.matches(Regex("^0x[0-9a-fA-F]{40}$"))) {
            result.text = "آدرس کیف پول واردشده معتبر نیست؛ برای بررسی وضعیت blacklist احتمالی، آدرس عمومی معتبر لازم است."
            return
        }
        val url = if (index == 11) customRpc.text.toString().trim() else rpcByIndex[index] ?: ""
        if (!url.startsWith("https://")) {
            result.text = "RPC معتبر HTTPS برای شبکه انتخابی در دسترس نیست."
            return
        }
        checkButton.isEnabled = false
        result.text = "در حال تحلیل مالک و قابلیت‌های مدیریتی قرارداد؛ فقط‌خواندنی…"
        executor.execute {
            val report = try {
                val expectedIds = mapOf(2 to "1", 3 to "56", 4 to "137", 5 to "42161", 6 to "10", 7 to "8453", 8 to "43114")
                val chainHex = rpc(url, "eth_chainId", JSONArray()).optString("result")
                val actualId = BigInteger(chainHex.removePrefix("0x").ifBlank { "0" }, 16).toString()
                val expectedId = expectedIds[index]
                if (expectedId != null && actualId != expectedId) {
                    throw IllegalStateException("Chain ID پاسخ RPC با شبکه انتخابی تطابق ندارد.")
                }
                val code = rpc(url, "eth_getCode", JSONArray().put(contract).put("latest")).optString("result")
                if (code.isBlank() || code == "0x" || code == "0x0") {
                    throw IllegalStateException("در این آدرس روی شبکه انتخابی قرارداد مستقر نیست.")
                }
                val out = StringBuilder("گزارش تحلیل مالک قرارداد\nشبکه: ${networks[index]} — Chain ID: $actualId\nقرارداد: $contract\n")
                var ownerFound = false
                try {
                    val ownerHex = ethCall(url, contract, "0x8da5cb5b").removePrefix("0x")
                    if (ownerHex.length >= 64) {
                        val owner = "0x" + ownerHex.takeLast(40)
                        ownerFound = true
                        if (owner.equals("0x0000000000000000000000000000000000000000", true)) {
                            out.append("\nمالک گزارش‌شده: آدرس صفر (ممکن است مالکیت واگذار/لغو شده باشد؛ به‌تنهایی اثبات قطعی نیست).")
                        } else {
                            out.append("\nمالک فعلی طبق owner(): $owner")
                            if (wallet.isNotBlank()) out.append(if (owner.equals(wallet, true)) "\nآدرس کیف پول واردشده با مالک قرارداد یکسان است." else "\nآدرس کیف پول واردشده مالک قرارداد نیست.")
                        }
                    } else {
                        out.append("\nتابع owner() پاسخ قابل‌تفسیر نداد.")
                    }
                } catch (_: Exception) {
                    out.append("\nتابع استاندارد owner() پشتیبانی نشد یا فراخوانی آن ناموفق بود.")
                }
                try {
                    val pausedHex = ethCall(url, contract, "0x5c975abb").removePrefix("0x")
                    val paused = BigInteger(pausedHex.ifBlank { "0" }, 16) != BigInteger.ZERO
                    out.append(if (paused) "\nوضعیت paused(): فعال — قرارداد اعلام می‌کند انتقال‌ها ممکن است متوقف باشند." else "\nوضعیت paused(): غیرفعال — تابع paused() مقدار false برگرداند.")
                } catch (_: Exception) {
                    out.append("\nوضعیت توقف: تابع استاندارد paused() در این قرارداد قابل‌خواندن نبود؛ این به معنی نبود قابلیت توقف نیست.")
                }
                val lowerCode = code.lowercase()
                val hasTransferOwnership = lowerCode.contains("f2fde38b")
                val hasRenounceOwnership = lowerCode.contains("715018a6")
                out.append("\n\nنشانه‌های بایت‌کد (فقط قرینه، نه اثبات):")
                out.append(if (hasTransferOwnership) "\n• selector مربوط به transferOwnership(address) دیده شد." else "\n• selector شناخته‌شده transferOwnership(address) در بایت‌کد خام پیدا نشد.")
                out.append(if (hasRenounceOwnership) "\n• selector مربوط به renounceOwnership() دیده شد." else "\n• selector شناخته‌شده renounceOwnership() در بایت‌کد خام پیدا نشد.")
                out.append("\n• وجود selector به معنی قابل‌فراخوانی بودن برای همه نیست؛ معمولاً دسترسی مالک/نقش لازم است.")
                out.append("\n\nمحدودیت‌های مهم:")
                out.append("\n• قراردادهای Proxy ممکن است منطق مالکیت را در Implementation یا Admin جداگانه نگه دارند؛ این بررسی به‌تنهایی مالک Proxy را تعیین نمی‌کند.")
                out.append("\n• نبود owner() به معنی renounce شدن مالکیت نیست؛ قرارداد ممکن است Ownable نباشد یا از AccessControl استفاده کند.")
                out.append("\n• blacklist، نقش‌های ادمین، تغییر مالیات و محدودیت انتقال در هر قرارداد روش متفاوتی دارند و هنوز به‌صورت عمومی و قطعی قابل تشخیص نیستند.")
                out.append("\n• این گزارش فقط‌خواندنی است؛ هیچ تراکنشی ارسال نشده است.")
                out.toString()
            } catch (e: Exception) {
                "تحلیل قرارداد ناموفق بود: ${e.message ?: "خطای RPC"}\nشبکه، قرارداد و RPC را بررسی کن. خطای خواندن به معنی امن یا ناامن بودن قرارداد نیست."
            }
            runOnUiThread { result.text = report; checkButton.isEnabled = true }
        }
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
                    "SOL" -> checkSolana(addr, contract)
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
        val expectedIds = mapOf(2 to "1", 3 to "56", 4 to "137", 5 to "42161", 6 to "10", 7 to "8453", 8 to "43114")
        val actualChainId = BigInteger(chainId.removePrefix("0x").ifBlank { "0" }, 16).toString()
        val expected = expectedIds[networks.indexOf(label)]
        if (expected != null && actualChainId != expected) throw IllegalStateException("شبکه RPC با انتخاب شما تطابق ندارد؛ Chain ID مورد انتظار " + expected + " و پاسخ " + actualChainId)
        val out = StringBuilder("شبکه انتخابی: $label\nChain ID پاسخ RPC: $actualChainId\nموجودی ارز بومی: $amount\n")
        if (contract.isNotBlank()) {
            val code = rpc(url, "eth_getCode", JSONArray().put(contract).put("latest")).optString("result")
            if (code.isBlank() || code == "0x" || code == "0x0") throw IllegalStateException("در این شبکه کدی در آدرس قرارداد وجود ندارد؛ قرارداد یا شبکه اشتباه است.")
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

    private fun checkSolana(addr: String, tokenMint: String): String {
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
            if (tokenMint.isBlank() || mint.equals(tokenMint, true)) {
                tokens.append("\n• Mint: $mint | مقدار: ${amount.optString("uiAmountString", "نامشخص")}")
            }
        }
        if (tokenMint.isNotBlank() && tokens.isEmpty()) tokens.append("\nتوکنی با این Mint در حساب‌های SPL کلاسیک پیدا نشد.")
        return "شبکه: Solana Mainnet\nموجودی SOL: $sol\nتعداد حساب‌های توکن بررسی‌شده: ${values.length()}\nتوکن‌ها:$tokens\n\nکارمزد و موجودی قابل انتقال نهایی به وضعیت حساب و حداقل موجودی اجاره‌ای نیز وابسته است."
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

    private fun diagnoseTronPermissions() {
        val wallet = address.text.toString().trim()
        if (network.selectedItemPosition != 10) {
            result.text = "ابتدا شبکه «ترون (TRON / TRX)» را انتخاب کن."
            return
        }
        if (!wallet.matches(Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$"))) {
            result.text = "آدرس عمومی معتبر TRON را وارد کن. عبارت بازیابی یا کلید خصوصی لازم نیست."
            return
        }
        checkButton.isEnabled = false
        result.text = "در حال خواندن مجوزهای Owner و Active از شبکه اصلی TRON؛ فقط‌خواندنی…"
        executor.execute {
            val report = try {
                val body = JSONObject().put("address", wallet).put("visible", true)
                val account = JSONObject(request("https://api.trongrid.io/wallet/getaccount", body.toString()))
                if (account.has("Error")) throw IllegalStateException(account.optString("Error"))
                if (!account.has("address")) {
                    "حساب از TronGrid پاسخ معتبر نگرفت یا هنوز روی زنجیره فعال نیست. این نتیجه به‌تنهایی به معنی از دست رفتن دارایی نیست."
                } else {
                    val out = StringBuilder()
                    out.append("گزارش مجوز امضای TRON Mainnet\nآدرس عمومی: $wallet\n")
                    val owner = account.optJSONObject("owner_permission")
                    if (owner == null) {
                        out.append("\n⛔ مجوز Owner در پاسخ وجود ندارد؛ ساختار حساب نیازمند بررسی بیشتر است.\n")
                    } else {
                        appendTronPermission(out, "Owner", owner, wallet, false)
                    }
                    val actives = account.optJSONArray("active_permission") ?: JSONArray()
                    if (actives.length() == 0) {
                        out.append("\nهیچ Active permission در پاسخ دیده نشد.\n")
                    } else {
                        for (i in 0 until actives.length()) {
                            val p = actives.optJSONObject(i) ?: continue
                            appendTronPermission(out, "Active #\${p.optInt("id", i + 2)}", p, wallet, true)
                        }
                    }
                    val witness = account.optJSONObject("witness_permission")
                    if (witness != null) appendTronPermission(out, "Witness", witness, wallet, false)
                    out.append("\nنتیجه و محدودیت:\n")
                    out.append("• اگر آدرس شما در فهرست کلیدهای Owner/Active نباشد، یا وزن آن به حد نصاب نرسد، همین کلید به‌تنهایی برای آن مجوز کافی نیست.\n")
                    out.append("• برای انتقال TRC-20 باید Active permission انتخاب‌شده اجازه اجرای قرارداد هوشمند را نیز داشته باشد؛ فهرست بالا داده خام عملیات را نشان می‌دهد.\n")
                    out.append("• این بررسی ثابت نمی‌کند Trust Wallet کلید خصوصی متناظر را در دسترس دارد؛ فقط وضعیت مجوز ثبت‌شده روی بلاک‌چین را می‌خواند.\n")
                    out.append("• اگر کلید مجاز دیگری در دسترس نباشد، این برنامه نمی‌تواند مجوز را دور بزند یا خودکار تغییر دهد. تغییر مجوز فقط با امضای معتبرِ دارای اختیار فعلی ممکن است.\n")
                    out.append("\nهیچ عبارت بازیابی، کلید خصوصی یا امضایی دریافت یا ارسال نشد.")
                    out.toString()
                }
            } catch (e: Exception) {
                "خواندن مجوزهای TRON ناموفق بود: \${e.message ?: "خطای ارتباط یا محدودیت API"}\nموجودی صفر فرض نشده و هیچ تغییری در کیف پول ایجاد نشده است."
            }
            runOnUiThread { result.text = report; checkButton.isEnabled = true }
        }
    }

    private fun appendTronPermission(out: StringBuilder, label: String, permission: JSONObject, wallet: String, active: Boolean) {
        val threshold = permission.optLong("threshold", -1L)
        val keys = permission.optJSONArray("keys") ?: JSONArray()
        var ownWeight = 0L
        val keyLines = StringBuilder()
        for (i in 0 until keys.length()) {
            val key = keys.optJSONObject(i) ?: continue
            val signer = key.optString("address")
            val weight = key.optLong("weight", 0L)
            val isOurs = signer.equals(wallet, true)
            if (isOurs) ownWeight += weight
            keyLines.append("\n  • $signer | وزن: $weight\${if (isOurs) "  ← آدرس حساب شما" else ""}")
        }
        out.append("\n$label — حد نصاب: $threshold")
        out.append("\nوزن آدرس حساب شما: $ownWeight")
        if (threshold > 0 && ownWeight >= threshold) {
            out.append("\n✓ وزن این آدرس به‌تنهایی به حد نصاب می‌رسد.")
        } else {
            out.append("\n⚠ وزن این آدرس به‌تنهایی به حد نصاب نمی‌رسد.")
        }
        out.append("\nکلیدهای ثبت‌شده:$keyLines")
        if (active) {
            out.append("\nمحدوده عملیات (operations): \${permission.optString("operations", "در پاسخ موجود نیست")}")
        }
        out.append("\n")
    }

    private fun openTrustWallet() {
        val launch = packageManager.getLaunchIntentForPackage("com.wallet.crypto.trustapp")
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.wallet.crypto.trustapp")))
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }
}
