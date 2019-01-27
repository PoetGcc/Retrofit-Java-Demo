package retrofit.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author g&c
 * @date 2019-01-20
 * 模拟 okhttp3.Call 发起网络请求
 * <p>
 * 拼接时间
 */
final class RawCall {
    private static final String NEW_LINE = "line.separator";

    /** Service 接口方法中的内容 */
    private String mContent;

    RawCall(String content) {
        mContent = content;
    }

    /** 执行 */
    String execute() {
        DateFormat format = SimpleDateFormat.getDateTimeInstance();
        String time = format.format(new Date());
        return time + System.getProperty(NEW_LINE) + mContent;
    }
}
