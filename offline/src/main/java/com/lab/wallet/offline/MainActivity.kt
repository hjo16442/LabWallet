package com.lab.wallet.offline

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var phrase: EditText
    private lateinit var passphrase: EditText
    private lateinit var target: EditText
    private lateinit var output: TextView
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(36, 32, 36, 32)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        fun label(value: String, size: Float = 16f) {
            root.addView(TextView(this).apply {
                text = value
                textSize = size
                setPadding(0, 14, 0, 6)
            })
        }
        label("بررسی آفلاین کلید مجاز TRON", 22f)
        label("این برنامه عبارت بازیابی را فقط برای محاسبه محلی استفاده می‌کند. مجوز اینترنت در این برنامه وجود ندارد.")
        label("عبارت بازیابی انگلیسی ۱۲ یا ۲۴ کلمه‌ای")
        phrase = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_VARIATION_PASSWORD
            minLines = 3
            gravity = Gravity.TOP
            hint = "کلمات را فقط روی گوشی وارد کن"
        }
        root.addView(phrase)
        label("BIP39 Passphrase (اگر هنگام ساخت کیف پول تعیین کرده‌ای)")
        passphrase = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "در غیر این صورت خالی بگذار"
        }
        root.addView(passphrase)
        label("آدرس مجاز مورد بررسی")
        target = EditText(this).apply {
            setSingleLine(true)
            setText("TUmdykWXNWHbRk3V4wFGKGHKMdh6jHzijm")
        }
        root.addView(target)
        runButton = Button(this).apply {
            text = "بررسی آفلاین مسیرهای TRON"
            isAllCaps = false
        }
        root.addView(runButton)
        output = TextView(this).apply {
            text = "هنوز بررسی انجام نشده است."
            textSize = 15f
            setTextIsSelectable(true)
            setPadding(14, 18, 14, 18)
        }
        root.addView(output)
        label("هشدار امنیتی مهم", 18f)
        label("تا زمانی که کد و APK را مستقل بررسی نکرده‌ای، عبارت واقعی را وارد نکن. گوشی را روی حالت پرواز بگذار و Wi‑Fi و Bluetooth را هم خاموش کن. این ابزار فقط تطبیق آدرس را بررسی می‌کند؛ موجودی را جابه‌جا نمی‌کند و مجوز بلاک‌چین را تغییر نمی‌دهد.")
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        runButton.setOnClickListener {
            val words = phrase.text.toString()
            val pass = passphrase.text.toString()
            val wanted = target.text.toString().trim()
            if (words.isBlank() || wanted.isBlank()) {
                output.text = "عبارت و آدرس هدف را وارد کن."
                return@setOnClickListener
            }
            runButton.isEnabled = false
            output.text = "در حال بررسی محلی…"
            worker.execute {
                val report = try {
                    assets.open("english.txt").use { stream ->
                        TronDeriver.check(words, pass, wanted, stream)
                    }
                } catch (e: Exception) {
                    "بررسی انجام نشد: ${e.message ?: "خطای داخلی"}"
                }
                runOnUiThread {
                    output.text = report
                    phrase.text?.clear()
                    passphrase.text?.clear()
                    runButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
