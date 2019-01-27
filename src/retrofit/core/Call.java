package retrofit.core;

import java.io.IOException;

/**
 * @author gcc
 * @date 2019-01-17
 * Retrofit Call
 * <p/>
 * 最终逻辑处理者
 */
public interface Call<T> extends Cloneable {

    /**
     * 最终执行
     *
     * @return 返回 Response
     */
    Response<T> execute() throws IOException;
}
