package com.stainberg.slothrestme

/**
 * Created by stainberg on 6/3/15.
 */
internal object SlothNetworkConfig {
    const val HTTP_CONNECT_TIMEOUT = 1000 * 10L
    const val HTTP_READ_TIMEOUT = 2000 * 10L
    const val HTTP_WRITE_TIMEOUT = 2000 * 10L
    const val DOWNLOAD_CONNECT_TIMEOUT = 1000 * 10L
    const val DOWNLOAD_READ_TIMEOUT = 1000 * 120L
    const val DOWNLOAD_WRITE_TIMEOUT = 1000 * 60L
}
