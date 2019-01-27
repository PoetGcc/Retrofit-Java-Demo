package retrofit.core;

import com.sun.istack.internal.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author g&c
 * @date 2019-01-20
 */
class Platform {
    private static final Platform PLATFORM = findPlatform();

    static Platform get() {
        return PLATFORM;
    }

    private static Platform findPlatform() {
        return new Platform();
    }

    @Nullable
    Executor defaultCallbackExecutor() {
        return null;
    }

    List<? extends CallAdapter.Factory> defaultCallAdapterFactories(
            @Nullable Executor callbackExecutor) {
        // TODO: 2019-01-20 为了好理解，可以考虑把 callbackExecutor  删掉，因为目前就一个线程
        return singletonList(DefaultCallAdapterFactory.INSTANCE);
    }

    List<? extends Converter.Factory> defaultConverterFactories() {
        return emptyList();
    }

    int defaultConverterFactoriesSize() {
        return 0;
    }
}
