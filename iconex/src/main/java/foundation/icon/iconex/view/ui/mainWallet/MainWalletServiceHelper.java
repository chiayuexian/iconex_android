package foundation.icon.iconex.view.ui.mainWallet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import foundation.icon.ICONexApp;
import foundation.icon.MyConstants;
import foundation.icon.iconex.service.NetworkService;
import foundation.icon.iconex.service.PRepService;
import foundation.icon.iconex.wallet.Wallet;
import foundation.icon.iconex.wallet.WalletEntry;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import loopchain.icon.wallet.core.Constants;

public class MainWalletServiceHelper {
    private static final String TAG = MainWalletServiceHelper.class.getSimpleName();
    private void Log(String log) { Log.d(TAG, log); }

    public interface OnLoadRemoteDataListener {
        void onLoadRemoteData(List<String[]> icxBalance, List<String[]> ethBalance, List<String[]> errBalance);
        void onLoadPRepsData(HashMap<String, PRepsRemoteData> pRepsData);
    }

    private Context mContext;
    private NetworkService mService;
    private OnLoadRemoteDataListener mListener = null;
    private boolean mIsBound = false;

    private Vector<String[]> mIcxBalance = new Vector<>();
    private Vector<String[]> mEthBalance = new Vector<>();
    private Vector<String[]> mErrBalance = new Vector<>();

    private int requestCount;
    private boolean isReceiveExchange = false;

    public MainWalletServiceHelper(Context context, OnLoadRemoteDataListener listener) {
        mContext = context;
        mListener = listener;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NetworkService.NetworkServiceBinder binder = (NetworkService.NetworkServiceBinder) service;
            mService = binder.getService();
            mService.registerBalanceCallback(mBalanceCallback);
            mService.registerExchangeCallback(mExchangeCallback);
            mIsBound = true;
            Log("service connected");

            requestRemoteData();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
            Log("service disconnected");
        }
    };

    private NetworkService.BalanceCallback mBalanceCallback = new NetworkService.BalanceCallback() {
        @Override
        public void onReceiveICXBalance(String id, String address, String result) {
            Log("Receive icx balance: " + result);
            mIcxBalance.add(new String[] { id, address, result});
            if(isDoneRequest(true, false)) completeRequest();
        }

        @Override
        public void onReceiveETHBalance(String id, String address, String result) {
            Log("Receive eth balance: " + result);
            address = address.substring(2);
            mEthBalance.add(new String[] { id, address, result });

            if(isDoneRequest(true, false)) completeRequest();
        }

        @Override
        public void onReceiveError(String id, String address, int code) {
            Log("Receive balance err code:" + code);
            if (address.startsWith(MyConstants.PREFIX_HEX)) {
                address = address.substring(2);
            }

            mErrBalance.add(new String[] { id, address, MyConstants.NO_BALANCE });

            if(isDoneRequest(true, false)) completeRequest();
        }

        @Override
        public void onReceiveException(String id, String address, String msg) {
            Log("Receive balance exception " + msg);
            if (address.startsWith(MyConstants.PREFIX_HEX)) {
                address = address.substring(2);
            }

            mErrBalance.add(new String[] { id, address, MyConstants.NO_BALANCE });

            if(isDoneRequest(true,false)) completeRequest();
        }
    };

    private NetworkService.ExchangeCallback mExchangeCallback = new NetworkService.ExchangeCallback() {
        @Override
        public void onReceiveExchangeList() {
            Log("Receive Exchange list");
            if(isDoneRequest(false,true)) completeRequest();
        }

        @Override
        public void onReceiveError(String resCode) {
            Log("Receive Exchange error: " + resCode);
            if(isDoneRequest(false,true)) completeRequest();
        }

        @Override
        public void onReceiveException(Throwable t) {
            t.printStackTrace();
            if(isDoneRequest(false,true)) completeRequest();
        }
    };

    private void completeRequest () {
        Log.d(TAG, "complete Request!");
        if (mListener != null) {
            mListener.onLoadRemoteData(
                    new ArrayList<String[]>() {{ addAll(mIcxBalance); }},
                    new ArrayList<String[]>() {{ addAll(mEthBalance); }},
                    new ArrayList<String[]>() {{ addAll(mErrBalance); }}
            );

            mIcxBalance.clear();
            mEthBalance.clear();
            mErrBalance.clear();
        }
    }

    private synchronized boolean isDoneRequest(boolean countingRequest, boolean markingExchange) {
        if (requestCount > 0 && countingRequest) {
            requestCount--;
        }

        if (markingExchange) {
            isReceiveExchange = true;
        }

        Log.d(TAG, "remain count: " + requestCount + ", isReceiveExchange: " + isReceiveExchange);
        return requestCount == 0 && isReceiveExchange;
    }

    private void cancleRequest() {
        if (!isDoneRequest(false,false)) {
            Log.d(TAG, "cancle request");
            mService.stopGetBalance();
            mIcxBalance.clear();
            mIcxBalance.clear();
            isReceiveExchange = false;
        }
    }

    public void requestRemoteData() {
        cancleRequest();

        Log.d(TAG, "request remote data");
        Object[] balanceList = makeGetBalanceList();
        HashMap<String, String> icxList = (HashMap<String, String>) balanceList[0];
        HashMap<String, String[]> ircList = (HashMap<String, String[]>) balanceList[1];
        HashMap<String, String> ethList = (HashMap<String, String>) balanceList[2];
        HashMap<String, String[]> ercList = (HashMap<String, String[]>) balanceList[3];

        requestCount = icxList.size() + ircList.size() + ethList.size() + ercList.size();
        isReceiveExchange = false;

        mService.getBalance(icxList, Constants.KS_COINTYPE_ICX);
        mService.getTokenBalance(ircList, Constants.KS_COINTYPE_ICX);
        mService.getBalance(ethList, Constants.KS_COINTYPE_ETH);
        mService.getTokenBalance(ercList, Constants.KS_COINTYPE_ETH);

        String exchangeList = makeExchangeList();
        mService.requestExchangeList(exchangeList);
    }

    private Object[] makeGetBalanceList() {
        HashMap<String, String> icxList = new HashMap<>();
        HashMap<String, String> ethList = new HashMap<>();
        HashMap<String, String[]> ercList = new HashMap<>();
        HashMap<String, String[]> ircList = new HashMap<>();

        for (Wallet info : ICONexApp.wallets) {
            if (info.getCoinType().equals(Constants.KS_COINTYPE_ICX)) {
                List<WalletEntry> entries = info.getWalletEntries();
                for (WalletEntry entry : entries) {
                    if (entry.getType().equals(MyConstants.TYPE_COIN))
                        icxList.put(Integer.toString(entry.getId()), entry.getAddress());
                    else
                        ircList.put(Integer.toString(entry.getId()), new String[]{entry.getAddress(), entry.getContractAddress()});
                }
            } else {
                List<WalletEntry> entries = info.getWalletEntries();
                for (WalletEntry entry : entries) {
                    if (entry.getType().equals(MyConstants.TYPE_COIN)) {
                        ethList.put(Integer.toString(entry.getId()), MyConstants.PREFIX_HEX + entry.getAddress());
                    } else {
                        ercList.put(Integer.toString(entry.getId()), new String[]{MyConstants.PREFIX_HEX + entry.getAddress(), entry.getContractAddress()});
                    }
                }
            }
        }

        return new Object[]{icxList, ircList, ethList, ercList};
    }

    private String makeExchangeList() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ICONexApp.EXCHANGES.size(); i++) {
            String symbol = ICONexApp.EXCHANGES.get(i);
            sb.append(symbol + MyConstants.EXCHANGE_USD.toLowerCase());
            sb.append(",");
            sb.append(symbol + MyConstants.EXCHANGE_BTC.toLowerCase());
            sb.append(",");
            sb.append(symbol + MyConstants.EXCHANGE_ETH.toLowerCase());

            if (i < ICONexApp.EXCHANGES.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public void resume() {
        Log("resume");
        Intent intent = new Intent(mContext, NetworkService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if (mIsBound) requestRemoteData();
    }

    public void stop () {
        Log("stop");
        cancleRequest();
        if (mIsBound) {
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void getPRepsRemoteData(List<String> icxAddresses) {
        PRepService pRepService = new PRepService(ICONexApp.NETWORK.getUrl());
        Hashtable<String, BigInteger> pRepsResults = new Hashtable<>();
        Log.d("GetPRepsData", "size=" +icxAddresses.size());
        String err = "Get %s error: %s, address: %s";

        Completable.merge(new ArrayList<Completable>() {{
            for (String address : icxAddresses) {
//                // ========== get stake
//                add(Completable.fromAction(new Action() {
//                    @Override
//                    public void run() {
//                        try {
//                            Log.d("GetPRepsData", address + " request stake");
//                            RpcObject rpcObject = pRepService.getStake(address).asObject();
//                            RpcItem stake = rpcObject.getItem("stake");
//                            pRepsResults.put(address+"-stake", stake.asInteger());
//                        } catch (Exception e) {
//                            String msg = String.format(err, "stake", e.getMessage(), address);
//                            Log.d("GetPRepsData", msg);
//                        }
//                    }
//                }));
                // ========== get i-score
                add(Completable.fromAction(new Action() {
                    @Override
                    public void run() {
                        try {
                            Log.d("GetPRepsData", address + " request i-score");
                            RpcObject rpcObject = pRepService.getIScore(address).asObject();
                            RpcItem iscore = rpcObject.getItem("iscore");
                            pRepsResults.put(address+"-iscore", iscore.asInteger());
                        } catch (Exception e) {
                            String msg = String.format(err, "i-score", e.getMessage(), address);
                            Log.d("GetPRepsData", msg);
                        }
                    }
                }));
                // ========== get delegation
                add(Completable.fromAction(new Action() {
                    @Override
                    public void run() {
                        try {
                            Log.d("GetPRepsData", address + " request delegation");
                            RpcObject rpcObject = pRepService.getDelegation(address).asObject();
                            RpcItem totalDelegated = rpcObject.getItem("totalDelegated");
                            RpcItem votingPower = rpcObject.getItem("votingPower");
                            pRepsResults.put(address+"-totalDelegated",totalDelegated.asInteger());
                            pRepsResults.put(address+"-votingPower", votingPower.asInteger());
                        } catch (Exception e) {
                            String msg = String.format(err, "delegation", e.getMessage(), address);
                            Log.d("GetPRepsData", msg);
                        }
                    }
                }));
            }
        }}).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        HashMap<String, PRepsRemoteData> pRepsData = new HashMap<>();
                        for(String address: icxAddresses) {
                            PRepsRemoteData pRepsRemoteData = new PRepsRemoteData()
                                    // .setStake(pRepsResults.get(address + "-stake"))
                                    .setiScore(pRepsResults.get(address + "-iscore"))
                                    .setTotalDelegated(pRepsResults.get(address + "-totalDelegated"))
                                    .setVotingPower(pRepsResults.get(address + "-votingPower"));

                            // get stake
                            BigInteger stake = pRepsRemoteData.getTotalDelegated()
                                    .add(pRepsRemoteData.getVotingPower());
                            pRepsRemoteData.setStake(stake);

                            Log.d("GetPRepsData", pRepsRemoteData.toString());
                            pRepsData.put(address, pRepsRemoteData);
                        }
                        mListener.onLoadPRepsData(pRepsData);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    public static class PRepsRemoteData {
        public BigInteger stake;
        public BigInteger iScore;
        public BigInteger totalDelegated;
        public BigInteger votingPower;

        public BigInteger getStake() {
            return stake;
        }

        public PRepsRemoteData setStake(BigInteger stake) {
            this.stake = stake;
            return this;
        }

        public BigInteger getiScore() {
            return iScore;
        }

        public PRepsRemoteData setiScore(BigInteger iScore) {
            this.iScore = iScore;
            return this;
        }

        public BigInteger getTotalDelegated() {
            return totalDelegated;
        }

        public PRepsRemoteData setTotalDelegated(BigInteger totalDelegated) {
            this.totalDelegated = totalDelegated;
            return this;
        }

        public BigInteger getVotingPower() {
            return votingPower;
        }

        public PRepsRemoteData setVotingPower(BigInteger votingPower) {
            this.votingPower = votingPower;
            return this;
        }

        @Override
        public String toString() {
            return "PRepsRemoteData{" +
                    "stake=" + stake +
                    ", iScore=" + iScore +
                    ", totalDelegated=" + totalDelegated +
                    ", votingPower=" + votingPower +
                    '}';
        }
    }
}