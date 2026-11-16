package com.lab.wallet

import java.math.BigInteger

// BUG-A18: کریپتوی دست‌ساز. فقط برای لب — در محصول واقعی از کتابخانهٔ استاندارد استفاده کن.
object Secp {
    val P: BigInteger = BigInteger("fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f", 16)
    val N: BigInteger = BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16)
    private val G = Pair(
        BigInteger("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798", 16),
        BigInteger("483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8", 16)
    )
    private val K = BigInteger("4242424242424242424242424242424242424242424242424242424242424242", 16) // BUG-A2

    private fun inv(a: BigInteger, m: BigInteger) = a.mod(m).modInverse(m)
    private typealias Pt = Pair<BigInteger, BigInteger>

    private fun add(p: Pt?, q: Pt?): Pt? {
        if (p == null) return q
        if (q == null) return p
        if (p.first == q.first && p.second.add(q.second).mod(P).signum() == 0) return null
        val lam = if (p == q)
            p.first.pow(2).multiply(BigInteger.valueOf(3)).multiply(inv(p.second.shiftLeft(1), P)).mod(P)
        else
            q.second.subtract(p.second).multiply(inv(q.first.subtract(p.first), P)).mod(P)
        val x = lam.pow(2).subtract(p.first).subtract(q.first).mod(P)
        return Pair(x, lam.multiply(p.first.subtract(x)).subtract(p.second).mod(P))
    }

    private fun mul(k: BigInteger, pt: Pt): Pt? {
        var r: Pt? = null; var acc: Pt? = pt; var kk = k
        while (kk.signum() > 0) {
            if (kk.testBit(0)) r = add(r, acc)
            acc = add(acc, acc); kk = kk.shiftRight(1)
        }
        return r
    }

    fun sign(d: BigInteger, z: BigInteger): Pair<BigInteger, BigInteger> {
        val r = mul(K, G)!!.first.mod(N)                       // nonce ثابت → r همیشه یکی است
        val s = inv(K, N).multiply(z.add(r.multiply(d))).mod(N)
        return Pair(r, s)
    }
}
