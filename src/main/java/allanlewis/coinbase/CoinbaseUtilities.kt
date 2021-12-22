package allanlewis.coinbase

import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CoinbaseUtilities {

    private val SHARED_MAC = Mac.getInstance("HmacSHA256")

    fun timestamp(): String {
        return  (System.currentTimeMillis() / 1000).toString()
    }

    fun sign(secret: String, path: String, method: String, body: String, timestamp: String): String {
        val prehash = timestamp + method.uppercase() + path + body
        val secretDecoded: ByteArray = Base64.getDecoder().decode(secret)
        val keyspec = SecretKeySpec(secretDecoded, SHARED_MAC.algorithm)
        val sha256 = SHARED_MAC.clone() as Mac
        sha256.init(keyspec)
        return Base64.getEncoder().encodeToString(sha256.doFinal(prehash.toByteArray()))
    }

}