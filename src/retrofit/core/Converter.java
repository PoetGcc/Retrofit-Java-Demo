package retrofit.core;

import com.sun.istack.internal.Nullable;
import retrofit.core.annotation.Content;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author g&c
 * @date 2019-01-20
 * 转换器, F -> T
 * <p>
 * Request Body 及 Response Body 都可以用
 */
public interface Converter<F, T> {

    @Nullable
    T convert(F value) throws IOException;

    /** Creates {@link Converter} instances based on a type and target usage. */
    abstract class Factory {

        /**
         * Returns a {@link Converter} for converting an HTTP response body to {@code type}, or null if
         * {@code type} cannot be handled by this factory. This is used to create converters for
         * response types such as {@code SimpleResponse} from a {@code Call<SimpleResponse>}
         * declaration.
         */
        @Nullable
        Converter<ResponseBody, ?> responseBodyConverter(Type type,
                                                         Annotation[] annotations, Retrofit retrofit) {
            return null;
        }

        /**
         * Returns a {@link Converter} for converting {@code type} to a {@link String}, or null if
         * {@code type} cannot be handled by this factory.
         * <p>
         * This is used to create converters for types specified by {@link Content @Content} values.
         */
        @Nullable
        Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            return null;
        }
    }
}
