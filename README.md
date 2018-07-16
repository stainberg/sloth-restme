# sloth-restme
## 基于coroutines和okhttp的http请求库，多请求链式调用取消请求等
### 1.集成
```
compile 'com.stainberg.sloth:sloth-http:1.0.4'
```
### 2.使用

#### 缓存的开启由2个地方控制
- 初始化缓存目录
- 请求中需要调用cache方法

``` kotlin
//开启缓存策略
//初始化缓存
SlothClient.initCache(directory, 50)//缓存在指定目录下
SlothClient.initCache(context, 50)//缓存在外部存储区的cache目录中
```

``` kotlin
//订阅404请求的处理逻辑
SlothClient.subscribeHttpCodeHandler(404, {
 //it参数是Request请求对象
      println("${it.url()} handler 404")
    })
```

```kotlin
//设定请求通用参数和头
SlothClient.fixHeader("123", "321")
SlothClient.fixParam("abc", "cba")
```

```kotlin
//内部debug日志打印,默认为false
SlothLogger.isDebug = true
```

```kotlin
//发起一个网络请求，可以根据需要写onSuccess，onFailed和onCompleted
//onSuccess中的it为ABC对象，一定不为空
//onFailed为错误信息
//onCompleted为请求结束的回调闭包
//重点说明：onSuccess和onFailed可以选择回调在主线程或者是工作协程中，onCompleted一定回调在非主线程
//单请求例子
SlothClient.request("http://192.168.75.36:1234/read").param("ids", SlothGson.toJson(arr)).tag("123").cache(page < 10)//开启缓存
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
    .get()
```

```kotlin
//链式请求例子
//start参数中的闭包为链式调用结束以后执行，在非主线程中执行
//链式调用请求请使用xxSync系列方法，可以参阅源码
//请求中的onSuccess，onFailed和onCompleted同单请求模型
//xxSync函数中的参数为onSuccess和onFailed是否在主线程或者是后台协程中执行，默认为主线程
//onCompleted闭包中有一个取消链式请求的函数cancelRemainderTasks()，该函数执行以后，整个Set将立即结束
//cancelRemainderTasks()后面的请求将不会得到执行
SlothClient.requestSet(
    SlothClient.request("http://192.168.75.36:1234/read/d").param("ids", "1")
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
    SlothClient.request("http://192.168.75.36:1234/read").param("ids", "2")
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
        .getSync(),
    SlothClient.request("http://192.168.75.36:1234/read").param("ids", "3")
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
```

如有疑问请留言
