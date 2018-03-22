package com.stainberg.slothrestme

/**
 * Created by Stainberg on 21/03/2018.
 */

fun String.Request() : SlothRequest<SlothResponse> {
    return SlothStandaloneRequest<SlothResponse>().url(this)
}