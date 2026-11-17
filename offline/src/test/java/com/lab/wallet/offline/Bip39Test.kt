package com.lab.wallet.offline

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Normalizer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Bip39Test {
    @Test fun standardMnemonicSeedVector() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val password = Normalizer.normalize(mnemonic, Normalizer.Form.NFKD).toByteArray()
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(password, "HmacSHA512"))
        var u = mac.doFinal("mnemonic".toByteArray() + byteArrayOf(0, 0, 0, 1))
        val t = u.clone()
        repeat(2047) {
            u = mac.doFinal(u)
            for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
        }
        val hex = t.joinToString("") { "%02x".format(it) }
        assertEquals("5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4", hex)
    }
}
