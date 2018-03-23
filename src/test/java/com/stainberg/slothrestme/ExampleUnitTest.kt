package com.stainberg.slothrestme

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        SlothLogger.isDebug = false
        val arr = arrayListOf<String>()
        arr.add("d36ab911728b4e9b8dafe63c43fe0906")
        println(Thread.currentThread().id)
        SlothClient.requestSet(
                SlothClient.request("http://192.168.75.36:1234/read").param("ids", SlothGson.toJson(arr)).tag("123")
                        .onSuccess(ABC::class.java, {
                            println("onSuccess ${SlothGson.toJson(it)}")
                        })
                        .onFailed {code, message->
                            println("onFailed Code = $code message = $message")
                        }
                        .onCompleted {
                            println("onCompleted")
                            println(Thread.currentThread().id)
                        }
                    .getSync(SlothHandleType.background),
                SlothClient.request("http://192.168.75.36:1234/read").param("ids", SlothGson.toJson(arr)).tag("123")
                        .onSuccess(ABC::class.java, {
                            println("onSuccess ${SlothGson.toJson(it)}")
                        })
                        .onFailed {code, message->
                            println("onFailed Code = $code message = $message")
                        }
                        .onCompleted {
                            println("onCompleted")
                            println(Thread.currentThread().id)
                            cancelRemainderTasks()
                        }
                        .getSync(SlothHandleType.main),
                SlothClient.request("http://192.168.75.36:1234/read").param("ids", SlothGson.toJson(arr)).tag("123")
                    .onSuccess(ABC::class.java, {
                        println("onSuccess ${SlothGson.toJson(it)}")
                    })
                    .onFailed {code, message->
                        println("onFailed Code = $code message = $message")
                    }
                    .onCompleted {
                        println("onCompleted")
                        println(Thread.currentThread().id)
                    }
                    .getSync()
        ).start({
            println("set end")
        })

//        SlothClient.request("http://192.168.75.36:1234/read").param("ids", SlothGson.toJson(arr))
//                .onSuccess(ABC::class.java, {
//                    println("onSuccess ${SlothGson.toJson(it)}")
//                })
//                .onFailed {code, message->
//                    println("onFailed Code = $code message = $message")
//                }
//                .onCompleted {
//                    println("onCompleted")
//                }
//                .get()

        while (true) {

        }
    }

    class ABC : SlothResponse {
        var read_nums = arrayListOf<BCD>()

        class BCD : SlothResponse {
            var id = ""
            var read_num = 0

        }
    }

    class MyRequest : SlothRequest() {
        init {
            url("http://youtube.com")
        }
    }

}