package retrofit.core;

import com.sun.istack.internal.Nullable;
import retrofit.core.annotation.Content;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static retrofit.core.Utils.parameterError;


/**
 * @author g&c
 * @date 2019-01-19
 * 构建 Request
 * <p>
 * 通过注解进行解析拼接
 */
final class RequestFactory {

    private final Method mMethod;
    private final ParameterHandler<?>[] mParameterHandlers;

    private RequestFactory(Builder builder) {
        this.mMethod = builder.mMethod;
        this.mParameterHandlers = builder.parameterHandlers;
    }

    String create(Object[] args) throws IOException {
        // It is an error to invoke a method with the wrong arg types.
        @SuppressWarnings("unchecked")
        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) mParameterHandlers;
        int argumentCount = args.length;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException("Argument count (" + argumentCount
                    + ") doesn't match expected count (" + handlers.length + ")");
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(stringBuilder, args[p]);
        }
        return stringBuilder.toString();
    }

    static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
        return new Builder(retrofit, method).build();
    }

    static final class Builder {
        final Retrofit mRetrofit;
        final Method mMethod;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        @Nullable
        ParameterHandler<?>[] parameterHandlers;

        Builder(Retrofit retrofit, Method method) {
            this.mRetrofit = retrofit;
            this.mMethod = method;
            this.parameterAnnotationsArray = method.getParameterAnnotations();
            this.parameterTypes = method.getParameterTypes();
        }

        RequestFactory build() {
            // 创建的 Service 接口方法中的参数注解
            int parameterCount = parameterAnnotationsArray.length;
            parameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0; p < parameterCount; p++) {
                parameterHandlers[p] = parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p]);
            }

            return new RequestFactory(this);
        }

        /** 遍历，解析参数注解 */
        private ParameterHandler<?> parseParameter(
                int p, Type parameterType, @Nullable Annotation[] annotations) {
            ParameterHandler<?> result = null;
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    ParameterHandler<?> annotationAction =
                            parseParameterAnnotation(p, parameterType, annotations, annotation);

                    if (annotationAction == null) {
                        continue;
                    }

                    if (result != null) {
                        throw parameterError(mMethod, p,
                                "Multiple Retrofit annotations found, only one allowed.");
                    }

                    result = annotationAction;
                }
            }

            if (result == null) {
                throw parameterError(mMethod, p, "No Retrofit annotation found.");
            }

            return result;
        }

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

        private void validateResolvableType(int p, Type type) {
            if (Utils.hasUnresolvableType(type)) {
                throw parameterError(mMethod, p,
                        "Parameter type must not include a type variable or wildcard: %s", type);
            }
        }
    }
}
