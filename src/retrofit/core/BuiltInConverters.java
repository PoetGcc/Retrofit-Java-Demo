package retrofit.core;

import com.sun.istack.internal.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author g&c
 * @date 2019-01-20
 * 内置的转换器
 */
final class BuiltInConverters extends Converter.Factory {

    @Nullable
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        // TODO: 2019-01-21  由于简化过程没有进行 http 请求，最终都转换成了 String
        if (type == String.class) {
            return ResponseBodyToStringConverter.INSTANCE;
        }

        // 返回 Void
        if (type == Void.class) {
            return VoidResponseBodyConverter.INSTANCE;
        }
        return null;
//        return ResponseBodyToStringConverter.INSTANCE;
    }

    /** Response Body 转换成 String */
    static final class ResponseBodyToStringConverter implements Converter<ResponseBody, String> {
        static final ResponseBodyToStringConverter INSTANCE = new ResponseBodyToStringConverter();

        @Override
        public String convert(ResponseBody value) throws IOException {
            return value.body();
        }
    }

    /** Void */
    static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override
        public Void convert(ResponseBody value) {
            return null;
        }
    }


    /** 转换成 String */
    static final class ToStringConverter implements Converter<Object, String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override
        public String convert(Object value) {
            return value.toString();
        }
    }
}
