package com.lab.wallet.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Bip39Test {
    private val phrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    @Test fun standardMnemonicSeedVector() {
        val seed = TronDeriver.mnemonicSeed(phrase.split(" "), "")
        val hex = seed.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals(
            "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4",
            hex
        )
        seed.fill(0)
    }

    @Test fun validMnemonicPassesChecksumAndDerivesTronAddresses() {
        val stream = javaClass.getResourceAsStream("/english.txt")!!
        val report = TronDeriver.check(phrase, "", "TUEZSdKsoDHQMeZwihtdoBiN46zxhGWYdH", stream)
        assertTrue(report.contains("اعتبار BIP39: تأیید شد"))
        assertTrue(report.contains("m/44'/195'/0'/0/0"))
    }

    @Test fun invalidChecksumIsRejected() {
        val stream = javaClass.getResourceAsStream("/english.txt")!!
        val invalid = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon"
        val report = TronDeriver.check(invalid, "", "TUEZSdKsoDHQMeZwihtdoBiN46zxhGWYdH", stream)
        assertTrue(report.contains("چک‌سام BIP39 معتبر نیست"))
    }
}
