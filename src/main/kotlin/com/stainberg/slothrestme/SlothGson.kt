package com.stainberg.slothrestme

import android.text.TextUtils

import com.google.gson.Gson

/**
 * Created by Stainberg on 7/6/15.
 */
object SlothGson {

    private val gson = Gson()

    fun <T> fromJson(srcStr: String, cls: Class<T>): T? {
        if (TextUtils.isEmpty(srcStr)) {
            return null
        }
        return gson.fromJson(srcStr, cls)
    }

    fun toJson(`object`: Any): String {
        return gson.toJson(`object`)
    }
}
