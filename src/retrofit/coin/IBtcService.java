package retrofit.coin;


import retrofit.core.Call;
import retrofit.core.annotation.Content;

/**
 * @author gcc
 * @date 2019-01-17
 * 获取币种Coin Service 接口
 */
public interface IBtcService {
    /** 获取 BTC 信息 */
    Call<String> getBtcCoinInfo(@Content String content);
}
