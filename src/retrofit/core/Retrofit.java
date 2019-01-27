package retrofit.core;

import com.sun.istack.internal.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * @author gcc
 * @date 2019-01-17
 * 拆解学习 Retrofit
 * <p>
 * 仿照 Retrofit 代码形式，动态代理 demo
 * <p>
 * create() ：需要将 Call 通过 CallAdapter 转换成 T
 */
public class Retrofit {

    /** Service 缓存 */
    private final Map<Method, ServiceMethod<?>> mServiceCache = new ConcurrentHashMap<>();
    /** Converter.Factory */
    private final List<Converter.Factory> mConverterFactories;
    /** CallAdapter */
    private final List<CallAdapter.Factory> mCallAdapterFactories;

    private Retrofit(List<Converter.Factory> converterFactories,
                     List<CallAdapter.Factory> callAdapterFactories) {
        // Copy+unmodifiable at call site.
        this.mConverterFactories = converterFactories;
        this.mCallAdapterFactories = callAdapterFactories;
    }

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

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #mCallAdapterFactories}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    CallAdapter<?, ?> callAdapter(Type returnType) {
        return nextCallAdapter(null, returnType);
    }

    private CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType) {
        checkNotNull(returnType, "returnType == null");
        int start = mCallAdapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = mCallAdapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = mCallAdapterFactories.get(i).get(returnType, this);
            if (adapter != null) {
                return adapter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(mCallAdapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = mCallAdapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(mCallAdapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #mConverterFactories factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #mConverterFactories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    private <T> Converter<ResponseBody, T> nextResponseBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        int start = mConverterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = mConverterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    mConverterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<ResponseBody, T>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(mConverterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = mConverterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(mConverterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link String} from the available
     * {@linkplain #mCallAdapterFactories}.
     */
    <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        for (int i = 0, count = mConverterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    mConverterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    private static <T> T checkNotNull(@Nullable T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }

    /** Build a new  {@link Retrofit} */
    public static final class Builder {
        private final Platform platform;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();


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
    }
}
