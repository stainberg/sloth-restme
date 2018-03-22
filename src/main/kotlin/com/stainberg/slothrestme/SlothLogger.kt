package com.stainberg.slothrestme

import com.alibaba.fastjson.JSON

/**
 * Created by Stainberg on 9/14/17.
 */

object SlothLogger {

    var isDebug: Boolean = false

    internal fun log(tag: String, x: String) {
        if (isDebug) {
            println("$tag = $x")
        }
    }

    internal fun log(tag: String, o: Any) {
        if (isDebug) {
            try {
                println("$tag = ${JSON.toJSONString(o)}")
            } catch (e: Exception) {
                println("$tag = Println Error e = ${e.message}")
            }

        }
    }

    internal fun <T : SlothResponse> printcUrl(o: SlothRequest<T>) {
        if (isDebug) {
            println("cURL String = ${buildCurlString(o)}")
        }
    }


    private fun <T : SlothResponse> buildCurlString(request: SlothRequest<T>): String {
        var url = request.url()
        val wspace = " "
        val builder = StringBuilder()
        builder.append("curl -X")
        builder.append(wspace)
        builder.append(request.method())
        builder.append(wspace)
        for (entry in request.headers().entries) {
            builder.append("-H")
            builder.append(wspace)
            builder.append("\"")
            builder.append(entry.key)
            builder.append(":")
            builder.append(entry.value)
            builder.append("\"")
            builder.append(wspace)
        }
        if (request.params().isNotEmpty()) {
            if (request.method() === SlothRequestType.GET) {
                url += "?" + SlothHttpUtils.getNameValuePair(request.params())
            } else {
                builder.append("-d")
                builder.append(wspace)
                builder.append("'")
                request.jsonObject()?. let {
                    builder.append(JSON.toJSONString(request.jsonObject()))
                }?: run {
                    var index = 1
                    for (entry in request.params().entries) {
                        builder.append(entry.key)
                        builder.append("=")
                        builder.append(entry.value)
                        if (index < request.params().size) {
                            builder.append("&")
                        }
                        index++
                    }
                }
                builder.append("'")
                builder.append(wspace)
            }
        }
        builder.append("\"")
        builder.append(url)
        builder.append("\"")
        return builder.toString()
    }
}
