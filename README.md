# Retrofit 笔记

**一开始的目的是为了学习动态代理的使用，后来干脆把整个流程梳理了下**

这不是完整的`Retrofit`，是纯`Java`环境下不涉及`Http`的流程代码，`Retrofit`的精简代码，把内部一些设计和逻辑做了精简，主要是为了学习整个代码的大体设计和流程

`Retrofit`内关于`OkHttp、Rx、Android`相关的部分都去除了，其中`okhttp3.call`由一个`RawCall`来代替，内部没有做相关网络请求，只是做了下字符串拼接后`println()`，尽量还保持着`Retrofit`原有的类，有些地方，把注释也给粘贴过来了

**Demo完整代码——>**[GitHub RetrofitDemo](https://github.com/PoetGcc/RetrofitDemo/tree/master)

***

# 1. Demo

使用也基本`Retrofit`的形式，`Demo`本身并无任何意义

## 1.1 Service

```java
/** 获取币种Coin Service 接口 */
public interface IBtcService {
    /** 获取 BTC 信息 */
    Call<String> getBtcCoinInfo(@Content String content);
}
```

`@Content`是自己定义的一个注解，表明方法内是`Content`,作用类比`@Path`等，支持多个不同类型的`Content`，例如

```java
getBtcCoinInfo(@Content String content1, @Content int content2)
```

返回类型必须是`Call<>`

***

## 1.2 使用

```java
public class RetrofitDemo {
    public static void main(String[] args) {
        btcInfo();
    }

    /** BTC 信息 */
    private static void btcInfo() {
        Retrofit retrofit = new Retrofit.Builder().build();
        IBtcService iBtcService = retrofit.create(IBtcService.class);
        Call<String> call = iBtcService.getBtcCoinInfo("BTC $3512.6 ");
        try {
            Response<String> response = call.execute();
            System.out.println(response.body());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

结果就是`Content`前拼接当前时间

```java
2019-1-27 12:01:27
BTC $3512.6
```

***

# 2. 流程

基于`Retrofit2.5.0`版本，整个流程的代码都在`core`文件夹下

整个流程涉及有4个重要部分`Retrofit,CallAdapter,Converter,OkHttpCall`

`Retrofit`作为门面，关键点之一是`crete()`方法，使用动态代理通过`CallAdatper`将`Retrofit.Call`转换为一个`Service`

`Converter`转换器，`Request Body`和`Response Body`都要用

`OkHttpCall`，在实际的`Retrofit`中，持有着一个`OkHttp3.Call`，最终所有的有关网络请求的操作，都会在这里执行。由于我这里将网络请求去掉做精简，只是调用了`RawCall.execute()`来模拟网络请求

####**注意：**<br>
还有一个重要的东西`CallbackExecutor`，给精简去掉了。在`Android`内，由于`UI线程`的原因，网络请求时`OkHtpp`会通过线程池运行在单独线程中，但本`Demo`为了简化流程，就网络请求模拟时，没有新创建线程， **对整个流程学习影响不大**

实际在`Retrofit`中，`Android`环境默认情况下，会用一个`Handler`进行线程间通信将`Response
`回调到`UI`线程

***

## 2.1 build()构建

`Retrofit`构造方法并不是`private`的，但一般都是通过内部的`Builder`来构建

```java
 @Nullable
 private Executor callbackExecutor;

 Builder(Platform platform) {
     this.platform = platform;
 }

 public Builder() {
     this(Platform.get());
 }

 public Retrofit build() {

     Executor callbackExecutor = this.callbackExecutor;
     if (callbackExecutor == null) {
         callbackExecutor = platform.defaultCallbackExecutor();
     }

     // Make a defensive copy of the adapters and add the default Call adapter.
     List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
     callAdapterFactories.addAll(platform.defaultCallAdapterFactories(callbackExecutor));

     // Make a defensive copy of the converters.
     List<Converter.Factory> converterFactories = new ArrayList<>(
                    1 + this.converterFactories.size() + platform.defaultConverterFactoriesSize());

     // Add the built-in converter factory first. This prevents overriding its behavior but also
     // ensures correct behavior when using converters that consume all types.
     converterFactories.add(new BuiltInConverters());
     converterFactories.addAll(this.converterFactories);
     converterFactories.addAll(platform.defaultConverterFactories());
     return new Retrofit(converterFactories, callAdapterFactories);
 }
```

`Executor`可以先不关注

`CallAdapter`作用就是将`Call`转换为`Service`,默认情况下使用`DefaultCallAdapterFactory`

`Converter`转换器，默认情况下，使用`BuiltInConverters`来构建内置的转换器

在调用`build()`后，`Retrofit`就有了关联的`CallAdapterFactory、ConveterFactory`，但此时具体要使用的`CallAdpater、Converter`还不知道

***

## 2.2 动态代理

`Retrofit`作为门面，关键点之一是`crete()`方法，使用动态代理通过`CallAdatper`将`Retrofit.Call`转换为一个`Service`

```java
/**
 * 利用 JDK 动态代理，create 出一个 T(Service)
 * <p>
 * 需要将 Call 通过 CallAdapter 转换成 T
 * <p>
 * Single-interface proxy creation guarded by parameter safety
 *
 * @param service 代理接口 Class
 * @param <T>     代理
 * @return T, 代理 Service
 */
@SuppressWarnings("unchecked")
public <T> T create(final Class<T> service) {
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
            new InvocationHandler() {
                private final Object[] emptyArgs = new Object[0];
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return loadServiceMethod(method).invoke(args != null ? args : emptyArgs);
                }
            });
}
```

`loadServiceMethod()`从已创建的`mServiceCache`中获取当前方法对应的`ServiceMethod`，没有就创建一个新的`put`进去

```java
private ServiceMethod<?> loadServiceMethod(Method method) {
    ServiceMethod<?> result = mServiceCache.get(method);
    if (result != null) {
        return result;
    }
    synchronized (mServiceCache) {
        result = mServiceCache.get(method);
        if (result == null) {
            result = ServiceMethod.parseAnnotations(this, method);
            mServiceCache.put(method, result);
        }
    }
    return result;
}
```

***

### 2.2.1 ServiceMethod

`ServiceMethod`是一个抽象类，有一个`HttpSeeviceMethod`子类，负责将好的`Request,CallAdapter,Converter`组合起来

```java
abstract class ServiceMethod<T> {
    static <T> ServiceMethod<T> parseAnnotations(Retrofit retrofit, Method method) {
        RequestFactory requestFactory = RequestFactory.parseAnnotations(retrofit, method);

        Type returnType = method.getGenericReturnType();
        if (Utils.hasUnresolvableType(returnType)) {
            throw methodError(method,
                    "Method return type must not include a type variable or wildcard: %s", returnType);
        }
        if (returnType == void.class) {
            throw methodError(method, "Service methods cannot return void.");
        }

        return HttpServiceMethod.parseAnnotations(retrofit, method, requestFactory);
    }

    abstract T invoke(Object[] args);
}
```

`parseAnnotations()`内，首先会通过`RequestFactory.parseAnnotations()`进行解析注解，然后构建出一个`RequestFactory`

再判断`returnType`返回类型，若为`void`，就会抛出异常，也就是创建的`Service`接口返回值不能为`void`

返回值为子类`HttpServiceMethod`的`parseAnnotations()`方法返回值

***

#### HttpServiceMethod

内部主要有两个方法：

```java
 /** 构建具体的 CallAdapter，Response Converter */
 static <ResponseT, ReturnT> HttpServiceMethod<ResponseT, ReturnT> parseAnnotations(
         Retrofit retrofit, Method method, RequestFactory requestFactory) {
     // 创建 CallAdapter
     CallAdapter<ResponseT, ReturnT> callAdapter = createCallAdapter(retrofit, method);
     Type responseType = callAdapter.responseType();
     // Response Converter
     Converter<ResponseBody, ResponseT> responseConverter =
             createResponseConverter(retrofit, method, responseType);
     return new HttpServiceMethod<>(requestFactory, callAdapter, responseConverter);
 }

 /** ServiceMehod 抽象方法，将OkHttpCall进行适配*/
 @Override
 ReturnT invoke(Object[] args) {
     return mCallAdapter.adapt(new OkHttpCall<>(mRequestFactory, args, mResponseConverter));
 }
```

到了此时，具体要使用的`CallApdater、Converter`都已经创建好，就等着调用`Service`内的方法进行网络请求

#### `mCallAdapter.adapt()`，就是适配器模式，将`Service`适配成`Call`

***

## 2.3 Converter 转换器

`Converter<F,T>`是一个接口，将一个`F -> T`

两个地方要用，`Request Body`以及`Response Body`

`Request Body`需要解析注解，拿到注解的信息拼装用于网络请求的`Request`

`Response Body`就是将`Json`转换成为`Bean`，但`Demo`里这里简化了，只是直接将字符串进行返回

***

### 2.3.1 解析 Request Body 注解

在`ServiceMethod.parseAnnotations()`内，`RequestFactory.parseAnnotations()`时会进行解析注解，组装`Request`

```java
static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
    return new Builder(retrofit, method).build();
}

```

进行 build 构建`RequestFactory `

```java
RequestFactory build() {
    // 创建的 Service 接口方法中的参数注解
    int parameterCount = parameterAnnotationsArray.length;
    parameterHandlers = new ParameterHandler<?>[parameterCount];
    for (int p = 0; p < parameterCount; p++) {
        parameterHandlers[p] = parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p]);
    }
    return new RequestFactory(this);
}
```

在`parseParameter()`就是根据`Service`接口内方法参数，使用`parseParameterAnnotation()`进行遍历对每个注解进行解析

```java
/** 解析具体的参数注解 */
@Nullable
private ParameterHandler<?> parseParameterAnnotation(
        int p, Type type, Annotation[] annotations, Annotation annotation) {
    if (annotation instanceof Content) {
        validateResolvableType(p, type);

        Converter<?, String> converter =
                mRetrofit.stringConverter(type, annotations);
        return new ParameterHandler.Content<>(converter);
    }
    return null;
}
```

`@Content`是一个自己定义的注解

```java
/** 内容注解 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Content {
}
```

`ParameterHandler`一个抽象类，根据注解，实现了一个`Content`子类，负责对注解进行解析拼接。注解是`RUNTIME`型的，为了加快读取，使用`ParameterHandler`

```java
static final class Content<T> extends ParameterHandler<T> {

    private final Converter<T, String> mValueConverter;

    Content(Converter<T, String> valueConverter) {
        this.mValueConverter = valueConverter;
    }

    @Override
    void apply(StringBuilder builder, T value) throws IOException {
        builder.append(mValueConverter.convert(value));
    }
}
```

在`apply()`将注解标记的参数拼接到`StirngBuilder`，在`OkHttpCall`内发起网路请求时，会通过这个`StirngBuilder`拿到`Request Body`

***

### 2.3.2 Response Body 转换

在`Retrofit.Builder`的`build()`方法内，添加了一个`BuiltInConverters`内置的转换器

```java
converterFactories.add(new BuiltInConverters());
```

#### BuiltInConverters 内置转换器

```java
/** 内置的转换器 */
final class BuiltInConverters extends Converter.Factory {
	@Nullable
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        // 由于简化过程没有进行 http 请求，最终都转换成了 String
        if (type == String.class) {
            return ResponseBodyToStringConverter.INSTANCE;
        }

        // 返回 Void
        if (type == Void.class) {
            return VoidResponseBodyConverter.INSTANCE;
        }
        return null;
    }

    /** Response Body 转换成 String */
    static final class ResponseBodyToStringConverter implements Converter<ResponseBody, String> {
        static final ResponseBodyToStringConverter INSTANCE = new ResponseBodyToStringConverter();

        @Override
        public String convert(ResponseBody value) throws IOException {
            return value.body();
        }
    }
}
```

在`ResponseBodyToStringConverter`内直接返回了`ResponseBody.body()`，也就是`Stirng`本身

***

## 2.4 OkHttpCall

当`call.execute()`时，最终会调用`OkHttpCall`内部逻辑，发起网络请求

```java
@Override
public Response<T> execute() throws IOException {
    RawCall call;
    synchronized (this) {
        if (mExecuted) {
            throw new IllegalStateException("Already executed.");
        }
        mExecuted = true;
        call = mRawCall;
        if (call == null) {
            try {
                call = mRawCall = createRawCall();
            } catch (IOException | RuntimeException | Error e) {
                throw e;
            }
        }
    }
    return parseResponse(call.execute());
}
```

在方法内，会先创建`RawCall`

```java
private RawCall createRawCall() throws IOException {
        return new RawCall(mRequestFactory.create(mArgs));
}
```

在`mRequestFactory.create()`内，会将`ParameterHandler`存的解析的注解拼装的`Request Body`转成`String`

`parseResponse()`解析结果

```java
private Response<T> parseResponse(String s) throws IOException {
    T body = mResponseConverter.convert(new ResponseBody(s));
    return Response.success(body);
}
```

根据创建的`Response Body Converter`来解析，之后构建一个`Response`类包装下，返回

***

# 3. 最后

整个精简后的`Demo`流程就这些，可以看看具体的代码




