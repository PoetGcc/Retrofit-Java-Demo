package retrofit.core;

import java.io.IOException;

/**
 * @author g&c
 * @date 2019-01-20
 */
final class OkHttpCall<T> implements Call<T> {
    private final Object[] mArgs;
    /** 负责根据注解组装 Request */
    private final RequestFactory mRequestFactory;
    /** Response Converter : 转换器将 ResponseBody 转换成为了 String */
    private final Converter<ResponseBody, T> mResponseConverter;
    /** 真正的 Call，最终实际进行 execute() */
    private RawCall mRawCall;
    /** 是否已执行，一个 OkHttpCall 只能执行一次 */
    private boolean mExecuted;

    OkHttpCall(RequestFactory requestFactory, Object[] args, Converter<ResponseBody, T> responseConverter) {
        this.mRequestFactory = requestFactory;
        this.mArgs = args;
        this.mResponseConverter = responseConverter;
    }

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

    private Response<T> parseResponse(String s) throws IOException {
        T body = mResponseConverter.convert(new ResponseBody(s));
        return Response.success(body);
    }


    private RawCall createRawCall() throws IOException {
        return new RawCall(mRequestFactory.create(mArgs));
    }
}
