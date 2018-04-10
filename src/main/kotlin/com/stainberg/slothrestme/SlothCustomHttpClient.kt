package com.stainberg.slothrestme

import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder

/**
 * Created by stainberg on 6/3/15.
 */
class SlothCustomHttpClient {

    internal var httpClient: OkHttpClient?= null
    private var builder : Builder? = null

    fun getBuilder() : Builder {
        builder = Builder()
        return builder!!
    }

    fun build() : OkHttpClient? {
        builder?. let {
            httpClient = it.build()
            return httpClient!!
        }
        return null
    }
}


