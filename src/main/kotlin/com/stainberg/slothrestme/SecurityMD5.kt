package com.stainberg.slothrestme

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal object SecurityMD5 {

    fun ToMD5(bytes: ByteArray): String {
        try {
            val algorithm = MessageDigest.getInstance("MD5")
            algorithm.reset()
            algorithm.update(bytes)
            return toHexString(algorithm.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    fun ToMD5(str: String): String {
        if (str.isEmpty()) {
            return ""
        }
        try {
            val algorithm = MessageDigest.getInstance("MD5")
            algorithm.reset()
            algorithm.update(str.toByteArray())
            return toHexString(algorithm.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    private fun toHexString(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (i in bytes.indices) {
            var `val` = bytes[i].toInt()
            if (`val` < 0) {
                `val` += 256
            }
            if (`val` < 16) {
                hexString.append("0")
            }
            hexString.append(Integer.toHexString(`val`))
        }
        return hexString.toString()
    }

}
