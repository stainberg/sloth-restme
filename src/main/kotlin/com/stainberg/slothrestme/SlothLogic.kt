package com.stainberg.slothrestme

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.*
import java.io.IOException

/**
 * Created by Stainberg on 20/03/2018.
 */
internal object SlothLogic {

    fun <T : SlothResponse> get(request: SlothRequest<T>) : Deferred<*> {
        val block = StandaloneResponseBlock<T>()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    fun <T : SlothResponse> post(request: SlothRequest<T>) : Deferred<*> {
        val block = StandaloneResponseBlock<T>()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    fun <T : SlothResponse> patch(request: SlothRequest<T>) : Deferred<*> {
        val block = StandaloneResponseBlock<T>()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    fun <T : SlothResponse> delete(request: SlothRequest<T>) : Deferred<*> {
        val block = StandaloneResponseBlock<T>()
        val task = async(CommonPool, CoroutineStart.LAZY, block = {
            fetchRequest(request, block)
        })
        block.initTask(task)
        return task
    }

    private suspend fun <T : SlothResponse>  fetchRequest(request: SlothRequest<T>, block : ResponseBlock<T>) {
        val req = parse(request)
        var code = 0
        block.request = request
        var result: T? = null
        try {
            val response = SlothHttpClient.httpClient.newCall(req).execute()
            var responseString: String
            SlothLogger.printcUrl(request)
            SlothLogger.log("Request id = ${request.tag}", request)
            response?.let { resp ->
                code = resp.code()
                if (resp.isSuccessful) {
                    resp.body()?.let { body ->
                        responseString = body.string()
                        if (request.cls != null) {
                            if(responseString.isNotEmpty()) {
                                SlothLogger.log("Response String id = ${request.tag}", responseString)
                                result = JSON.parseObject(responseString, request.cls)
                            } else {
                                code = SlothNetworkConstants.BUSINESS_ERROR
                            }
                        }
                    }
                }
                resp.close()
            }?: run {
                code = SlothNetworkConstants.NO_RESPONSE
            }
        } catch (e : IOException) {
            code = SlothNetworkConstants.NETWORK_ERROR
        } catch (e : JSONException) {
            code = SlothNetworkConstants.PARSER_ERROR
        }
        request.success?. let {
            it(block, result, code)
        }
    }

    private fun <T : SlothResponse> parse(requestEntity: SlothRequest<T>) : Request {
        val params = requestEntity.params()
        val headers = requestEntity.headers()
        val attachments = requestEntity.attachments()
        val builder = Request.Builder()
        builder.addHeader("Cache-Control", "no-cache")
        if (headers.isNotEmpty()) {
            val set = headers.entries
            for (maEntry in set) {
                builder.addHeader(maEntry.key, maEntry.value)
            }
        }
        if (requestEntity.method() == SlothRequestType.GET) {
            if (params.isNotEmpty()) {
                requestEntity.url += "?" + SlothHttpUtils.getNameValuePair(params)!!
            }
        } else if (requestEntity.method() == SlothRequestType.POST) {
            if (requestEntity.jsonObject() != null) {
                builder.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(requestEntity.jsonObject())))
            } else {
                val body: RequestBody
                if (attachments.size == 0) {
                    val bodyBuilder = FormBody.Builder()
                    for (maEntry in params.entries) {
                        bodyBuilder.add(maEntry.key, maEntry.value)
                    }
                    body = bodyBuilder.build()
                } else {
                    val bodyBuilder = MultipartBody.Builder()
                    bodyBuilder.setType(MultipartBody.FORM)
                    for (maEntry in params.entries) {
                        bodyBuilder.addFormDataPart(maEntry.key, maEntry.value)
                    }
                    for (request in attachments) {
                        bodyBuilder.addFormDataPart(request.key, request.filename, request.body)
                    }
                    body = bodyBuilder.build()
                }
                builder.post(body)
            }
        } else if (requestEntity.method() == SlothRequestType.PATCH) {
            if (requestEntity.jsonObject() != null) {
                builder.patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(requestEntity.jsonObject())))
            } else {
                val body: RequestBody
                if (attachments.size == 0) {
                    val bodyBuilder = FormBody.Builder()
                    for (maEntry in params.entries) {
                        bodyBuilder.add(maEntry.key, maEntry.value)
                    }
                    body = bodyBuilder.build()
                } else {
                    val bodyBuilder = MultipartBody.Builder()
                    bodyBuilder.setType(MultipartBody.FORM)
                    for (maEntry in params.entries) {
                        bodyBuilder.addFormDataPart(maEntry.key, maEntry.value)
                    }
                    for (request in attachments) {
                        bodyBuilder.addFormDataPart(request.key, request.filename, request.body)
                    }
                    body = bodyBuilder.build()
                }
                builder.patch(body)
            }
        } else if (requestEntity.method() == SlothRequestType.DELETE) {
            if (requestEntity.jsonObject() != null) {
                builder.patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(requestEntity.jsonObject())))
            } else {
                val body: RequestBody
                if (attachments.size == 0) {
                    val bodyBuilder = FormBody.Builder()
                    for (maEntry in params.entries) {
                        bodyBuilder.add(maEntry.key, maEntry.value)
                    }
                    body = bodyBuilder.build()
                } else {
                    val bodyBuilder = MultipartBody.Builder()
                    bodyBuilder.setType(MultipartBody.FORM)
                    for (maEntry in params.entries) {
                        bodyBuilder.addFormDataPart(maEntry.key, maEntry.value)
                    }
                    for (request in attachments) {
                        bodyBuilder.addFormDataPart(request.key, request.filename, request.body)
                    }
                    body = bodyBuilder.build()
                }
                builder.delete(body)
            }
        }
//        builder.tag(SecurityMD5.ToMD5(url))
        builder.url(requestEntity.url)
        return builder.build()
    }

}