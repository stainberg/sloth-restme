package com.stainberg.slothrestme

/**
 * Created by stainberg on 6/3/15.
 */
internal object SlothNetworkConfig {
    val HTTP_CONNECT_TIMEOUT = 1000 * 10L
    val HTTP_READ_TIMEOUT = 2000 * 10L
    val HTTP_WRITE_TIMEOUT = 2000 * 10L

    val DOWNLOAD_CONNECT_TIMEOUT = 1000 * 10L
    val DOWNLOAD_READ_TIMEOUT = 1000 * 120L
    val DOWNLOAD_WRITE_TIMEOUT = 1000 * 60L
}
