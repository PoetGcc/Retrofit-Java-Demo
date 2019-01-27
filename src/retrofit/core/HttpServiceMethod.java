package retrofit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static retrofit.core.Utils.methodError;


/**
 * @author g&c
 * @date 2019-01-19
 * Adapts an invocation of an interface method into an HTTP call
 * <p/>
 * 将声明的 Service 接口适配成 Retrofit Call
 */
class HttpServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {

    /** 解析注解 */
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

    private static <ReturnT, ResponseT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
            Retrofit retrofit, Method method) {
        Type returnType = method.getGenericReturnType();
        try {
            // noinspection unchecked
            return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw methodError(method, e, "Unable to create call adapter for %s", returnType);
        }
    }

    private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
            Retrofit retrofit, Method method, Type responseType) {
        Annotation[] annotations = method.getAnnotations();
        try {
            return retrofit.responseBodyConverter(responseType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw methodError(method, e, "Unable to create converter for %s", responseType);
        }
    }

    /** Request Factory */
    private final RequestFactory mRequestFactory;
    /** 转换器 */
    private final Converter<ResponseBody, ResponseT> mResponseConverter;
    /** Call 适配器 */
    private final CallAdapter<ResponseT, ReturnT> mCallAdapter;

    private HttpServiceMethod(RequestFactory requestFactory,
                              CallAdapter<ResponseT, ReturnT> callAdapter, Converter<ResponseBody, ResponseT> responseConverter) {
        this.mRequestFactory = requestFactory;
        this.mCallAdapter = callAdapter;
        this.mResponseConverter = responseConverter;
    }

    @Override
    ReturnT invoke(Object[] args) {
        return mCallAdapter.adapt(new OkHttpCall<>(mRequestFactory, args, mResponseConverter));
    }
}
