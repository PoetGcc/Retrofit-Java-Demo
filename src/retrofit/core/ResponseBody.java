package retrofit.core;

/**
 * @author gcc
 * @date 2019-01-21
 */
final class ResponseBody {
    private String mBody;

    public ResponseBody(String body) {
        mBody = body;
    }

    String body() {
        return mBody;
    }

    @Override
    public String toString() {
        return mBody;
    }
}
