package com.stainberg.slothrestme

/**
 * Created by Stainberg on 9/14/17.
 */

object SlothLogger {

    var isDebug: Boolean = false

    internal fun log(tag: String, x: String?) {
        if (isDebug) {
            println("$tag = $x")
        }
    }

    internal fun log(tag: String, o: Any?) {
        if (isDebug) {
            try {
                o?. let {
                    println("$tag = ${SlothGson.toJson(it)}")
                }
            } catch (e: Exception) {
                println("$tag = Println Error e = ${e.message}")
            }

        }
    }

    internal fun printcUrl(o: SlothRequest) {
        if (isDebug) {
            println("cURL String = ${buildCurlString(o)}")
        }
    }


    private fun buildCurlString(request: SlothRequest): String {
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
                    builder.append(SlothGson.toJson(it))
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
