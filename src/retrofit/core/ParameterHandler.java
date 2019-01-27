package retrofit.core;

import com.sun.istack.internal.Nullable;

import java.io.IOException;

/**
 * @author g&c
 * @date 2019-01-20
 * 参数注解 Handler
 */
abstract class ParameterHandler<T> {
    abstract void apply(StringBuilder builder, @Nullable T value) throws IOException;

    final ParameterHandler<Iterable<T>> iterable() {
        return new ParameterHandler<Iterable<T>>() {
            @Override
            void apply(StringBuilder builder, @Nullable Iterable<T> values) throws IOException {
                if (values == null) {
                    return;
                }
                for (T value : values) {
                    ParameterHandler.this.apply(builder, value);
                }
            }
        };
    }

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
}
