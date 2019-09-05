package foundation.icon.iconex.service;

import java.io.IOException;
import java.math.BigInteger;

import foundation.icon.iconex.wallet.Wallet;
import foundation.icon.icx.Call;
import foundation.icon.icx.Request;
import foundation.icon.icx.SignedTransaction;
import foundation.icon.icx.Transaction;
import foundation.icon.icx.TransactionBuilder;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.IconAmount;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import loopchain.icon.wallet.core.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class IconService {

    private foundation.icon.icx.IconService iconService;

    public IconService(String host) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        iconService = new foundation.icon.icx.IconService(new HttpProvider(httpClient, host));
    }

    public BigInteger getBalance(String balance) throws IOException {
        return iconService.getBalance(new Address(balance)).execute();
    }

    public Bytes sendTransaction(SignedTransaction transaction) throws IOException {
        return iconService.sendTransaction(transaction).execute();
    }

    public RpcItem getStepPrice() throws IOException {
        Call<RpcItem> call = new Call.Builder()
                .to(new Address(Constants.ADDRESS_GOVERNANCE))
                .method(Constants.METHOD_GETSTEPPRICE)
                .build();

        return iconService.call(call).execute();
    }

    public BigInteger estimateStep(String address) throws IOException {
        foundation.icon.icx.IconService test =
                new foundation.icon.icx.IconService(new HttpProvider(Urls.Yeouido.Node.getUrl(), 3));
        BigInteger networkId = new BigInteger("3");
        Address fromAddress = new Address(address);
        Address toAddress = new Address(Constants.ADDRESS_GOVERNANCE);

        BigInteger value = IconAmount.of("0", IconAmount.Unit.ICX).toLoop();
        long timestamp = System.currentTimeMillis() * 1000L;
        BigInteger nonce = new BigInteger("1");

        Transaction transaction = TransactionBuilder.newBuilder()
                .nid(networkId)
                .from(fromAddress)
                .to(toAddress)
                .value(value)
                .timestamp(new BigInteger(Long.toString(timestamp)))
                .nonce(nonce)
                .call("claimIScore")
                .build();

        return test.estimateStep(transaction).execute();
    }
}
