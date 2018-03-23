@file:JvmName("SlothString")
package com.stainberg.slothrestme

/**
 * Created by Stainberg on 21/03/2018.
 */

fun String.Request() : SlothRequest {
    return SlothStandaloneRequest().url(this)
}