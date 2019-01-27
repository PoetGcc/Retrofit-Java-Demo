package retrofit;


import retrofit.coin.IBtcService;
import retrofit.core.Call;
import retrofit.core.Response;
import retrofit.core.Retrofit;

import java.io.IOException;

/**
 * @author gcc
 * @date 2019-01-17
 */
public class RetrofitDemo {
    public static void main(String[] args) {
        btcInfo();
    }

    /** BTC 信息 */
    private static void btcInfo() {
        Retrofit retrofit = new Retrofit.Builder().build();
        IBtcService iBtcService = retrofit.create(IBtcService.class);
        Call<String> call = iBtcService.getBtcCoinInfo("BTC $3512.6 ");
        try {
            Response<String> response = call.execute();
            System.out.println(response.body());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
