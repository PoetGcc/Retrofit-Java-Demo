package retrofit.core;

import com.sun.istack.internal.Nullable;

import java.lang.reflect.Type;

/**
 * @author g&c
 * @date 2019-01-19
 * 将 R 适配成 T
 */
public interface CallAdapter<R, T> {

    /**
     * Returns the value type that this adapter uses when converting the HTTP response body to a Java
     * object. For example, the response type for {@code Call<Repo>} is {@code Repo}. This type
     * is used to prepare the {@code call} passed to {@code #adapt}.
     * <p>
     * Note: This is typically not the same type as the {@code returnType} provided to this call
     * adapter's factory.
     */
    Type responseType();

    /** 转换 ： 返回一个接口 T 来代表 Call */
    T adapt(Call<R> call);

    /** CallAdapter  Factory */
    abstract class Factory {
        /** 创建一个 CallAdapter */
        @Nullable
        public abstract CallAdapter<?, ?> get(Type returnType, Retrofit retrofit);
    }
}
