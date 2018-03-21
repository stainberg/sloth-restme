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
        SlothClient.requestSet(
                SlothClient.request("http://baidu.com").addParam("123", "321").addHeader("abc", "cba")
                    .get(ABC::class.java, {
                        println(it.javaClass.simpleName)
                    }),
                SlothClient.request("http://163.com").addParam("123", "321").addHeader("abc", "cba")
                    .get(ABC.BCD::class.java, {
                        println(it.javaClass.simpleName)
                    }),
                MyRequest().get(ABC.BCD.MyResp::class.java, {
                        println(it.javaClass.simpleName)
                    }),
                "http://google.com".Request()
                        .get(ABC::class.java, {
                            println(it.javaClass.simpleName)
                        })
        ).start({
            println("set end")
        })

        while (true) {

        }
    }

    private class ABC : SlothResponse() {
        var url = ""

        class BCD : SlothResponse() {
            var _url = ""

            class MyResp : SlothResponse() {
                var myUrl = ""
            }

        }
    }

    class MyRequest : SlothRequest() {
        init {
            url("http://youtube.com")
        }
    }

}