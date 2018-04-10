package com.stainberg.slothrestme

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Created by stainberg on 6/3/15.
 */
internal object SlothHttpClient {

    val httpClient: OkHttpClient
    var customClient : SlothCustomHttpClient? = null

    init {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(SlothNetworkConfig.HTTP_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
        builder.readTimeout(SlothNetworkConfig.HTTP_READ_TIMEOUT, TimeUnit.MILLISECONDS)
        builder.writeTimeout(SlothNetworkConfig.HTTP_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        httpClient = builder.build()
    }

    fun forkClient() : OkHttpClient {
        customClient?.httpClient?. let {
            return it.newBuilder().build()
        }?:return httpClient.newBuilder().build()
    }

    fun forkClientBuilder() : OkHttpClient.Builder {
        customClient?.httpClient?. let {
            return it.newBuilder()
        }?: return httpClient.newBuilder()
    }
}


