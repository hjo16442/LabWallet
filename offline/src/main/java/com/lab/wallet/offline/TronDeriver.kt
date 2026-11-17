package com.lab.wallet.offline

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.KeccakDigest
import java.io.InputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.Normalizer

internal object TronDeriver {
    private data class ExtendedKey(val key: BigInteger, val chain: ByteArray)
    private val curveParams = SECNamedCurves.getByName("secp256k1")
    private val order = curveParams.n
    private val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun check(mnemonicInput: String, passphraseInput: String, wantedInput: String, wordStream: InputStream): String {
        val words = mnemonicInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size != 12 && words.size != 24) return "عبارت باید دقیقاً ۱۲ یا ۲۴ کلمه انگلیسی باشد."
        val wordList = loadWords(wordStream)
        val indexes = words.map { wordList.indexOf(it.lowercase()) }
        if (indexes.any { it < 0 }) return "حداقل یک کلمه در فهرست رسمی BIP39 انگلیسی پیدا نشد."
        if (!validChecksum(indexes)) return "چک‌سام BIP39 معتبر نیست؛ ترتیب یا املای کلمات را بررسی کن."
        val seed = mnemonicSeed(words, passphraseInput)
        val wanted = wantedInput.trim()
        val found = mutableListOf<String>()
        val checked = mutableListOf<String>()
        try {
            val master = masterKey(seed)
            var account = deriveChild(master, 44L or HARDENED)
            master.chain.fill(0)
            account = deriveChild(account, 195L or HARDENED)
            account = deriveChild(account, HARDENED)
            val external = deriveChild(account, 0L)
            for (index in 0L..20L) {
                val child = deriveChild(external, index)
                val address = tronAddress(child.key)
                checked.add("m/44'/195'/0'/0/$index  →  $address")
                if (address == wanted) found.add("m/44'/195'/0'/0/$index")
                child.chain.fill(0)
            }
            account.chain.fill(0)
            external.chain.fill(0)
        } finally {
            seed.fill(0)
        }
        return buildString {
            append("بررسی آفلاین پایان یافت.\n")
            append("اعتبار BIP39: تأیید شد (${words.size} کلمه)\n")
            append("مسیرهای بررسی‌شده: ۲۱ مسیر استاندارد TRON، از index صفر تا ۲۰\n")
            append("آدرس هدف: ").append(wanted).append("\n\n")
            if (found.isNotEmpty()) {
                append("تطبیق پیدا شد؛ عبارت واردشده این آدرس را تولید می‌کند.\n")
                append("مسیر منطبق: ").append(found.joinToString()).append("\n")
                append("این نتیجه به معنی امکان برداشت قطعی نیست؛ امضای تراکنش و وضعیت مجوز روی زنجیره باید جداگانه بررسی شود.")
            } else {
                append("در مسیرهای بررسی‌شده تطبیقی پیدا نشد.\n")
                append("ممکن است Passphrase متفاوت، مسیر مشتق‌سازی دیگر یا index بالاتر استفاده شده باشد.")
            }
            append("\n\nآدرس‌های مشتق‌شده (فقط عمومی):\n")
            checked.forEach { append(it).append("\n") }
        }
    }

    internal fun mnemonicSeed(words: List<String>, passphraseInput: String): ByteArray {
        val normalized = Normalizer.normalize(words.joinToString(" "), Normalizer.Form.NFKD)
        val passphrase = Normalizer.normalize(passphraseInput, Normalizer.Form.NFKD)
        return pbkdf2(normalized.toByteArray(Charsets.UTF_8), ("mnemonic" + passphrase).toByteArray(Charsets.UTF_8), 2048, 64)
    }

    private fun loadWords(stream: InputStream): List<String> {
        val words = stream.bufferedReader(Charsets.UTF_8).use { it.readLines().map(String::trim).filter(String::isNotEmpty) }
        require(words.size == 2048) { "فهرست BIP39 باید ۲۰۴۸ کلمه داشته باشد" }
        return words
    }

    private fun validChecksum(indexes: List<Int>): Boolean {
        val totalBits = indexes.size * 11
        val entropyBits = totalBits * 32 / 33
        val checksumBits = totalBits - entropyBits
        val bits = BooleanArray(totalBits)
        var p = 0
        for (index in indexes) for (b in 10 downTo 0) bits[p++] = ((index shr b) and 1) == 1
        val entropy = ByteArray(entropyBits / 8)
        for (i in 0 until entropyBits) if (bits[i]) entropy[i / 8] = (entropy[i / 8].toInt() or (1 shl (7 - i % 8))).toByte()
        val hash = MessageDigest.getInstance("SHA-256").digest(entropy)
        for (i in 0 until checksumBits) {
            val expected = ((hash[i / 8].toInt() ushr (7 - i % 8)) and 1) == 1
            if (bits[entropyBits + i] != expected) return false
        }
        entropy.fill(0); hash.fill(0)
        return true
    }

    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(password, "HmacSHA512"))
        val out = ByteArray(length)
        val blocks = (length + mac.macLength - 1) / mac.macLength
        for (block in 1..blocks) {
            val input = salt + byteArrayOf(0, 0, 0, block.toByte())
            var u = mac.doFinal(input)
            val t = u.clone()
            for (i in 2..iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val offset = (block - 1) * mac.macLength
            System.arraycopy(t, 0, out, offset, minOf(t.size, length - offset))
            u.fill(0); t.fill(0); input.fill(0)
        }
        password.fill(0); salt.fill(0)
        return out
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }

    private fun masterKey(seed: ByteArray): ExtendedKey {
        val digest = hmac("Bitcoin seed".toByteArray(Charsets.US_ASCII), seed)
        val key = BigInteger(1, digest.copyOfRange(0, 32))
        require(key.signum() > 0 && key < order) { "BIP32 master key نامعتبر" }
        val chain = digest.copyOfRange(32, 64)
        digest.fill(0)
        return ExtendedKey(key, chain)
    }

    private fun deriveChild(parent: ExtendedKey, index: Long): ExtendedKey {
        val data = if ((index and HARDENED) != 0L) byteArrayOf(0) + ser256(parent.key) + ser32(index)
            else compressedPublicKey(parent.key) + ser32(index)
        val digest = hmac(parent.chain, data)
        val il = BigInteger(1, digest.copyOfRange(0, 32))
        require(il < order) { "BIP32 child index نامعتبر" }
        val childKey = il.add(parent.key).mod(order)
        require(childKey.signum() != 0) { "BIP32 child key صفر شد" }
        val chain = digest.copyOfRange(32, 64)
        data.fill(0); digest.fill(0)
        return ExtendedKey(childKey, chain)
    }

    private fun ser256(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        val out = ByteArray(32)
        val sourceOffset = if (raw.size > 32) raw.size - 32 else 0
        val length = minOf(raw.size, 32)
        System.arraycopy(raw, sourceOffset, out, 32 - length, length)
        raw.fill(0)
        return out
    }

    private fun ser32(value: Long): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value.toInt()).array()
    private fun compressedPublicKey(privateKey: BigInteger): ByteArray = curveParams.g.multiply(privateKey).normalize().getEncoded(true)

    private fun tronAddress(privateKey: BigInteger): String {
        val public = curveParams.g.multiply(privateKey).normalize().getEncoded(false)
        val digest = ByteArray(32)
        val keccak = KeccakDigest(256)
        keccak.update(public, 1, public.size - 1)
        keccak.doFinal(digest, 0)
        val payload = byteArrayOf(0x41) + digest.copyOfRange(12, 32)
        val first = MessageDigest.getInstance("SHA-256").digest(payload)
        val second = MessageDigest.getInstance("SHA-256").digest(first)
        val checked = payload + second.copyOfRange(0, 4)
        public.fill(0); digest.fill(0); first.fill(0); second.fill(0); payload.fill(0)
        return base58(checked).also { checked.fill(0) }
    }

    private fun base58(input: ByteArray): String {
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) zeros++
        var value = BigInteger(1, input)
        val radix = BigInteger.valueOf(58)
        val result = StringBuilder()
        while (value > BigInteger.ZERO) {
            val parts = value.divideAndRemainder(radix)
            result.append(alphabet[parts[1].toInt()])
            value = parts[0]
        }
        repeat(zeros) { result.append('1') }
        return result.reverse().toString()
    }

    private const val HARDENED = 0x80000000L
}
