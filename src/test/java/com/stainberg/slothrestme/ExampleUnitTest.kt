package com.stainberg.slothrestme

import com.alibaba.fastjson.JSON
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        val arr = arrayListOf<String>()
        arr.add("d36ab911728b4e9b8dafe63c43fe0906")

        SlothClient.requestSet(
                SlothClient.request("http://192.168.75.36:1234/read").param("ids", JSON.toJSONString(arr))
                    .get(ABC::class.java, {result, code ->
                        println(code)
                        println(JSON.toJSON(result))
                    })
        ).start({
            println("set end")
        })

        while (true) {

        }
    }

    private class ABC : SlothResponse() {
        var read_nums = arrayListOf<BCD>()

        class BCD : SlothResponse() {
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