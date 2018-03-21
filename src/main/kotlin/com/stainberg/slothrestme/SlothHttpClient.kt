package com.stainberg.slothrestme

import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by stainberg on 6/3/15.
 */
internal object SlothHttpClient {

    val httpClient: OkHttpClient
    val downloadClient: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(SlothNetworkConfig.HTTP_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
        builder.readTimeout(SlothNetworkConfig.HTTP_READ_TIMEOUT, TimeUnit.MILLISECONDS)
        builder.writeTimeout(SlothNetworkConfig.HTTP_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        val protocols = ArrayList<Protocol>()
        protocols.add(Protocol.HTTP_1_1)
        builder.protocols(protocols)
        httpClient = builder.build()

        val downloadb = OkHttpClient.Builder()
        downloadb.connectTimeout(SlothNetworkConfig.DOWNLOAD_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
        downloadb.readTimeout(SlothNetworkConfig.DOWNLOAD_READ_TIMEOUT, TimeUnit.MILLISECONDS)
        downloadb.writeTimeout(SlothNetworkConfig.DOWNLOAD_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        downloadClient = downloadb.build()
    }

}
