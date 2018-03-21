package com.stainberg.slothrestme


import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * Created by Stainberg on 7/9/15.
 */
internal object SlothHttpUtils {

    fun getNameValuePair(params: Map<String, String>): String? {
        var StrArgs: String? = null
        val set = params.entries
        for ((key, value) in set) {
            try {
                if (StrArgs == null) {
                    StrArgs = key + "=" + URLEncoder.encode(value, "UTF-8")
                } else {
                    StrArgs = "&" + key + "=" + URLEncoder.encode(value, "UTF-8")
                    StrArgs += StrArgs
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

        }
        return StrArgs
    }

}
