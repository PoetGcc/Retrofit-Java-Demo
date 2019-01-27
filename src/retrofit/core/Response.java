package retrofit.core;

import com.sun.istack.internal.Nullable;

/**
 * @author g&c
 * @date 2019-01-19
 * 响应
 */
public final class Response<T> {

    @Nullable
    private final T mBody;

    private Response(T body) {
        this.mBody = body;
    }

    /** 创建一个成功的 Response */
    static <T> Response<T> success(T body) {
        return new Response<>(body);
    }

    @Nullable
    public T body() {
        return mBody;
    }
}
