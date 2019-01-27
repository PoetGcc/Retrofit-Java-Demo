package retrofit.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static retrofit.core.Utils.methodError;


/**
 * @author gcc
 * @date 2019-01-18
 */
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
