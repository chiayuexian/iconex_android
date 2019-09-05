package foundation.icon.iconex.service;

import com.google.gson.JsonElement;

import foundation.icon.iconex.service.response.VSResponse;
import loopchain.icon.wallet.core.request.RequestData;
import loopchain.icon.wallet.core.response.LCResponse;
import loopchain.icon.wallet.core.response.TRResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by js on 2018. 2. 15..
 */

public interface RESTApiService {

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST(ServiceConstants.LC_API_HEADER + ServiceConstants.LC_API_V3)
    Call<LCResponse> sendRequest(@Body RequestData requestData);

    @GET(ServiceConstants.TR_V0 + ServiceConstants.TR_API_EX_HEADER)
    Call<TRResponse> sendGetExchangeList(@Query("codeList") String codeList);

    @GET(ServiceConstants.TR_V3 + ServiceConstants.TR_API_TX_LIST_HEADER)
    Call<TRResponse> sendGetTxList(@Query("address") String address, @Query("page") int page);

    @GET(ServiceConstants.TR_V3 + ServiceConstants.TR_API_TOKEN_TX_LIST)
    Call<TRResponse> sendGetTokenTxList(@Query("page") int page, @Query("contractAddr") String contract, @Query("tokenAddr") String addr);

    @GET(ServiceConstants.VS_API)
    Call<VSResponse> sendVersionCheck();

    @GET(ServiceConstants.ETH_TOKENS)
    Call<JsonElement> getEthTokens();
}
