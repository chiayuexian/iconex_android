package foundation.icon.iconex.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import foundation.icon.ICONexApp;
import foundation.icon.MyConstants;
import foundation.icon.iconex.R;
import foundation.icon.iconex.dialogs.Basic2ButtonDialog;
import foundation.icon.iconex.dialogs.BasicDialog;
import foundation.icon.iconex.menu.WalletPwdChangeActivityNew;
import foundation.icon.iconex.realm.RealmUtil;
import foundation.icon.iconex.view.ui.mainWallet.MainWalletFragment;
import foundation.icon.iconex.view.ui.mainWallet.MainWalletServiceHelper;
import foundation.icon.iconex.dialogs.IconDisclaimerDialogFragment;
import foundation.icon.iconex.dialogs.MessageDialog;
import foundation.icon.iconex.dialogs.WalletPasswordDialog;
import foundation.icon.iconex.menu.appInfo.AppInfoActivity;
import foundation.icon.iconex.menu.bundle.ExportWalletBundleActivity;
import foundation.icon.iconex.menu.lock.SettingLockActivity;
import foundation.icon.iconex.util.ConvertUtil;
import foundation.icon.iconex.view.ui.mainWallet.MainWalletFragment;
import foundation.icon.iconex.view.ui.mainWallet.MainWalletServiceHelper;
import foundation.icon.iconex.view.ui.mainWallet.component.WalletCardView;
import foundation.icon.iconex.view.ui.mainWallet.component.WalletManageMenuDialog;
import foundation.icon.iconex.view.ui.mainWallet.viewdata.TotalAssetsViewData;
import foundation.icon.iconex.view.ui.mainWallet.viewdata.WalletCardViewData;
import foundation.icon.iconex.view.ui.mainWallet.viewdata.WalletItemViewData;
import foundation.icon.iconex.wallet.Wallet;
import foundation.icon.iconex.wallet.WalletEntry;
import foundation.icon.iconex.wallet.main.MainActivity;
import kotlin.jvm.functions.Function1;
import loopchain.icon.wallet.core.Constants;

public class MainWalletActivity extends AppCompatActivity implements
        MainWalletFragment.AsyncRequester,
        WalletCardView.OnClickWalletItemListner,
        MainWalletFragment.SideMenu,
        MainWalletFragment.PRepsMenu,
        MainWalletServiceHelper.OnLoadRemoteDataListener {

    public static String TAG = MainWalletActivity.class.getSimpleName();

    // findFragmentByTag
    private static String MAIN_WALLET_FRAGMENT_TAG = "main wallet fragment";

    // service connect
    private MainWalletServiceHelper mainWalletServiceHelper = null;

    private boolean isFingerprintInvalidated = false;

    // cache data
    private List<WalletCardViewData> cachedlstWalletData = new ArrayList<>();
    private List<String[]> cachedIcxBalance = new ArrayList<>();
    private List<String[]> cachedEthBalance = new ArrayList<>();
    private List<String[]> cachedErrBalance = new ArrayList<>();
    private HashMap<String, MainWalletServiceHelper.PRepsRemoteData> cachedpRepsData = new HashMap<>();

    private Map<String, WalletEntry> indexedWalletEntry = new HashMap<>();
    private Map<String, WalletItemViewData> indexedWalletItemData = new HashMap<>();
    private Map<Integer, WalletEntry> indexedByIdWalletEntry = new HashMap<>();
    private Map<Integer, Wallet> indexedByIdWallet = new HashMap<>();

    // ================== activity life cycle
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null) {
            MyConstants.MainPopUp popUp = (MyConstants.MainPopUp) getIntent().getExtras().get("popup");
            if (popUp == MyConstants.MainPopUp.BUNDLE) {
                MessageDialog messageDialog = new MessageDialog(this);
                messageDialog.setTitleText(getString(R.string.msgLoadBundle));
                messageDialog.show();
            }
        }

        isFingerprintInvalidated = getIntent().getBooleanExtra(AuthActivity.EXTRA_INVALIDATED, false);

        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, MainWalletFragment.newInstance(), MAIN_WALLET_FRAGMENT_TAG)
                .commit();

        mainWalletServiceHelper = new MainWalletServiceHelper(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainWalletServiceHelper.resume();

        showRecoverFingerprintAuth();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mainWalletServiceHelper.stop();
    }

    // ===================== ViewData listener (MainWalletFragment.AsyncRequester)
    @Override
    public void asyncRequestInitData() {
        cachingWalletItemData();
        getMainWalletFragment().asyncResponseInit(cachedlstWalletData);
        mainWalletServiceHelper.getPRepsRemoteData(new ArrayList<String>() {{
            for (Wallet wallet : ICONexApp.wallets) {
                if (wallet.getCoinType().equals(Constants.KS_COINTYPE_ICX)) {
                    add(wallet.getAddress());
                }
            }
        }});
    }

    @Override
    public void asyncRequestRefreshData() {
        mainWalletServiceHelper.requestRemoteData();
        mainWalletServiceHelper.getPRepsRemoteData(new ArrayList<String>() {{
            for (Wallet wallet : ICONexApp.wallets) {
                if (wallet.getCoinType().equals(Constants.KS_COINTYPE_ICX)) {
                    add(wallet.getAddress());
                }
            }
        }});
    }

    @Override
    public void asyncRequestChangeExchangeUnit(MainWalletFragment.ExchangeUnit exchangeUnit) {
        setExchange(exchangeUnit);
    }

    @Override
    public void notifyWalletDatachage() {
        setPRepsData(cachedIcxBalance, cachedEthBalance, cachedErrBalance, cachedpRepsData);
    }

    // =================== Service listener (MainWalletServiceHelper.OnLoadRemoteDataListener)
    @Override
    public void onLoadRemoteData(List<String[]> icxBalance, List<String[]> ethBalance, List<String[]> errBalance) {
        Log.d(TAG, "load remote data, icx: " + icxBalance.size() + ", eth: " + ethBalance.size() + ", err: " + errBalance.size());
        cachedIcxBalance = icxBalance;
        cachedEthBalance = ethBalance;
        cachedErrBalance = errBalance;

        setPRepsData(cachedIcxBalance, cachedEthBalance, cachedErrBalance, cachedpRepsData);
    }

    @Override
    public void onLoadPRepsData(HashMap<String, MainWalletServiceHelper.PRepsRemoteData> pRepsData) {
        cachedpRepsData = pRepsData;
        setPRepsData(cachedIcxBalance, cachedEthBalance, cachedErrBalance, cachedpRepsData);
    }

    // =================== private methods
    private void cachingWalletItemData() {
        // load Item
        cachedlstWalletData = new ArrayList<>();
        for (Wallet wallet : ICONexApp.wallets) {
            cachedlstWalletData.add(WalletCardViewData.convertWallet2ViewData(wallet));
        }

        // indexing wallet entry
        indexedWalletEntry = new HashMap<>();
        indexedByIdWalletEntry = new HashMap<>();
        indexedByIdWallet = new HashMap<>();
        for (Wallet wallet : ICONexApp.wallets) {
            for (WalletEntry entry : wallet.getWalletEntries()) {
                String key = wallet.getAddress() + "," + entry.getId();
                indexedWalletEntry.put(key, entry);
                indexedByIdWallet.put(entry.getId(), wallet);
                indexedByIdWalletEntry.put(entry.getId(), entry);
            }
        }

        // indexing view data
        indexedWalletItemData = new HashMap<>();
        for (WalletCardViewData walletViewData : cachedlstWalletData) {
            for (WalletItemViewData itemViewData : walletViewData.getLstWallet()) {
                String key = walletViewData.getAddress() + "," + itemViewData.getEntryID();
                indexedWalletItemData.put(key, itemViewData);
            }
        }
    }

    private void setPRepsData(List<String[]> icxBalance,
                              List<String[]> ethBalance,
                              List<String[]> errBalance,
                              HashMap<String, MainWalletServiceHelper.PRepsRemoteData> pRepsData) {

        BigDecimal totalAsset = setBalances(icxBalance, ethBalance, errBalance);

        for (WalletCardViewData cardViewData : cachedlstWalletData) {
            MainWalletServiceHelper.PRepsRemoteData data = pRepsData.get(cardViewData.getAddress());
            if (data != null) {
                cardViewData.setStaked(data.getStake());
                cardViewData.setiScore(data.getiScore());

                WalletItemViewData walletItemViewData = cardViewData.getLstWallet().get(0);
                walletItemViewData.setStacked(data.getStake());
                walletItemViewData.setiScore(data.getiScore());
            }
        }


        BigInteger totalVoted = BigInteger.ZERO;
        BigInteger totalStake = BigInteger.ZERO;

        for (String address : pRepsData.keySet()) {
            MainWalletServiceHelper.PRepsRemoteData data = pRepsData.get(address);

            totalVoted = totalVoted.add(data.getTotalDelegated());
            totalStake = totalStake.add(data.getStake());
        }

        for (Wallet wallet : ICONexApp.wallets) {
            MainWalletServiceHelper.PRepsRemoteData data = pRepsData.get(wallet.getAddress());
            if (data != null) {
                wallet.setStaked(data.getStake());
                wallet.setiScore(data.getiScore());
                wallet.setVotingPower(data.getVotingPower());
            }
        }

        BigDecimal dTotalVoted = new BigDecimal(ConvertUtil.getValue(totalVoted, 18));
        BigDecimal dTotalStake = new BigDecimal(ConvertUtil.getValue(totalStake, 18));
        BigDecimal votedPower = dTotalStake.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                dTotalVoted.divide(dTotalStake, 1, RoundingMode.HALF_UP);

        getMainWalletFragment().asyncResponseRefreash(
                cachedlstWalletData,
                new TotalAssetsViewData()
                        .setTotalAsset(totalAsset)
                        .setVotedPower(votedPower)
        );
    }

    private BigDecimal setBalance(String id, String address, String result, String unit) {
        String key = address + "," + id;
        WalletEntry walletEntry = indexedWalletEntry.get(key);
        WalletItemViewData viewData = indexedWalletItemData.get(key);
        if (walletEntry == null || viewData == null) return null;

        try {
            walletEntry.setBalance(result);
            // if result == "-" then throw exception
            String strDecimal = ConvertUtil.getValue(new BigInteger(walletEntry.getBalance()), walletEntry.getDefaultDec());
            BigDecimal balance = new BigDecimal(strDecimal);

            String exchangeKey = viewData.getSymbol().toLowerCase() + unit;
            BigDecimal exchanger = new BigDecimal(ICONexApp.EXCHANGE_TABLE.get(exchangeKey));
            BigDecimal exchanged = balance.multiply(exchanger);

            viewData.setAmount(balance).setExchanged(exchanged);
            return exchanged;
        } catch (Exception e) {
            // if result == "-" then amount(balance), exchange set null
            Log.d(TAG, "set balance " + walletEntry.getName() + ": " + e.getMessage());
            viewData.setAmount(null).setExchanged(null);
            return null;
        }
    }

    private BigDecimal setBalances(
            List<String[]> icxBalance,
            List<String[]> ethBalance,
            List<String[]> errBalance) {

        BigDecimal totalAsset = icxBalance.size() == 0 && ethBalance.size() == 0 ? null : BigDecimal.ZERO;
        MainWalletFragment.ExchangeUnit exchageUnit = getMainWalletFragment().getCurrentExchangeUnit();
        String strUnit = exchageUnit.name().toLowerCase();

        cachingWalletItemData();

        for (String[] param : icxBalance) {
            BigDecimal exchanged = setBalance(param[0], param[1], param[2], strUnit);
            if (exchanged != null) {
                totalAsset = totalAsset.add(exchanged);
            } // else
        }

        for (String[] param : ethBalance) {
            BigDecimal exchanged = setBalance(param[0], param[1], param[2], strUnit);
            if (exchanged != null) {
                totalAsset = totalAsset.add(exchanged);
            } // else
        }

        for (String[] param : errBalance) {
            setBalance(param[0], param[1], param[2], strUnit);
        }

        return totalAsset;
    }

    private void setExchange(MainWalletFragment.ExchangeUnit exchangeUnit) {
        String unit = exchangeUnit.name();

        BigDecimal totalAsset = null;
        for (WalletItemViewData viewData : indexedWalletItemData.values()) {
            try {
                BigDecimal balance = viewData.getAmount();

                String exchangeKey = viewData.getSymbol().toLowerCase() + unit.toLowerCase();
                BigDecimal exchanger = new BigDecimal(ICONexApp.EXCHANGE_TABLE.get(exchangeKey));
                BigDecimal exchanged = balance.multiply(exchanger);

                viewData.setExchanged(exchanged);
                if (totalAsset == null) totalAsset = BigDecimal.ZERO;
                totalAsset = totalAsset.add(exchanged);
            } catch (Exception e) {
                viewData.setExchanged(null);
            }

        }
        getMainWalletFragment().asyncResponseChangeExchangeUnit(
                exchangeUnit,
                new TotalAssetsViewData().setTotalAsset(totalAsset)
        );
    }

    private MainWalletFragment getMainWalletFragment() {
        return ((MainWalletFragment) getSupportFragmentManager()
                .findFragmentByTag(MAIN_WALLET_FRAGMENT_TAG));
    }

    private Wallet findWalletByViewData(WalletCardViewData viewData) {
        String address = viewData.getAddress();
        for (Wallet wallet : ICONexApp.wallets) {
            if (wallet.getAddress().equals(address)) {
                return wallet;
            }
        }

        return null;
    }

    // ======================= on click item listenr
    @Override
    public void onClickWalletItem(WalletItemViewData itemViewData) {
        Integer entryID = itemViewData.getEntryID();
        Wallet wallet = indexedByIdWallet.get(entryID);
        WalletEntry walletEntry = indexedByIdWalletEntry.get(entryID);

        startActivity(
                new Intent(this, WalletDetailActivity.class)
                        .putExtra(WalletDetailActivity.PARAM_ENTRY_ID, entryID)
                        .putExtra(WalletDetailActivity.PARAM_WALLET, ((Serializable) wallet))
                        .putExtra(WalletDetailActivity.PARAM_WALLET_ENTRY, ((Serializable) walletEntry))
        );
    }

    // ==================================== side menu item
    @Override
    public void createWallet() {
        startActivity(new Intent(this, CreateWalletActivity.class));
    }

    @Override
    public void loadWallet() {
        startActivity(new Intent(this, LoadWalletActivity.class));
    }

    @Override
    public void exportWalletBundle() {
        startActivity(new Intent(this, ExportWalletBundleActivity.class));
    }

    @Override
    public void screenLock() {
        startActivity(new Intent(this, SettingLockActivity.class)
                .putExtra(SettingLockActivity.ARG_TYPE, MyConstants.TypeLock.DEFAULT));
    }

    @Override
    public void appVer() {
        startActivity(new Intent(this, AppInfoActivity.class));
    }

    @Override
    public void iconexDisclamers() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new IconDisclaimerDialogFragment())
                .addToBackStack(null)
                .commit();
    }

    // ========================= P-Peps Menu
    private MessageDialog messageDialog;
    private WalletPasswordDialog passwordDialog;

    @Override
    public void pReps(WalletCardViewData viewData) {
        startActivity(new Intent(this, PRepListActivity.class));
    }

    @Override
    public void stake(WalletCardViewData viewData) {
        Wallet wallet = findWalletByViewData(viewData);

        passwordDialog = new WalletPasswordDialog(this, wallet,
                new WalletPasswordDialog.OnPassListener() {
                    @Override
                    public void onPass(byte[] bytePrivateKey) {
                        startActivity(new Intent(MainWalletActivity.this, PRepStakeActivity.class)
                                .putExtra("wallet", (Serializable) wallet)
                                .putExtra("privateKey", Hex.toHexString(bytePrivateKey)));
                    }
                });

        try {
            BigInteger balance = new BigInteger(wallet.getWalletEntries().get(0).getBalance());
            if (balance.compareTo(new BigInteger("5")) < 0) {
                messageDialog = new MessageDialog(MainWalletActivity.this);
                messageDialog.setTitleText(getString(R.string.notEnoughForStaking));
                messageDialog.show();
            } else {
                passwordDialog.show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Balance not set", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void vote(WalletCardViewData viewData) {
        Wallet wallet = findWalletByViewData(viewData);

        passwordDialog = new WalletPasswordDialog(this, wallet,
                new WalletPasswordDialog.OnPassListener() {
                    @Override
                    public void onPass(byte[] bytePrivateKey) {
                        startActivity(new Intent(MainWalletActivity.this, PRepVoteActivity.class)
                                .putExtra("wallet", (Serializable) wallet)
                                .putExtra("privateKey", Hex.toHexString(bytePrivateKey)));
                    }
                });

        BigInteger votingPower = wallet.getVotingPower();
//        if (votingPower.compareTo(BigInteger.ZERO) == 0) {
//            messageDialog = new MessageDialog(MainWalletActivity.this);
//            messageDialog.setTitleText(getString(R.string.hasNoVotingPower));
//            messageDialog.show();
//        } else {
        passwordDialog.show();
//        }
    }

    @Override
    public void iScore(WalletCardViewData viewData) {
        Wallet wallet = findWalletByViewData(viewData);

        passwordDialog = new WalletPasswordDialog(this, wallet,
                new WalletPasswordDialog.OnPassListener() {
                    @Override
                    public void onPass(byte[] bytePrivateKey) {
                        startActivity(new Intent(MainWalletActivity.this, PRepIScoreActivity.class)
                                .putExtra("wallet", (Serializable) wallet)
                                .putExtra("privateKey", Hex.toHexString(bytePrivateKey)));
                    }
                });

        BigInteger votingPower = wallet.getVotingPower();
//        if (votingPower.compareTo(BigInteger.ZERO) == 0) {
//            messageDialog = new MessageDialog(MainWalletActivity.this);
//            messageDialog.setTitleText(getString(R.string.hasNoVotingPower));
//            messageDialog.show();
//        } else {
        passwordDialog.show();
//        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        } else {
            super.onBackPressed();
        }
    }

    private void showRecoverFingerprintAuth() {
        if (isFingerprintInvalidated) {
            isFingerprintInvalidated = false;
            MessageDialog messageDialog = new MessageDialog(this);
            messageDialog.setTitleText(getString(R.string.authMsgRecoverFingerprintAuth));
            messageDialog.setSingleButton(false);
            messageDialog.setOnConfirmClick(new Function1<View, Boolean>() {
                @Override
                public Boolean invoke(View view) {
                    startActivity(new Intent(MainWalletActivity.this, SettingLockActivity.class)
                            .putExtra(SettingLockActivity.ARG_TYPE, MyConstants.TypeLock.RECOVER));
                    return true;
                }
            });
            messageDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case WalletManageMenuDialog.REQ_PASSWORD_CHANGE: {
                WalletPwdChangeActivityNew.getActivityResult(resultCode, data, new WalletPwdChangeActivityNew.OnResultListener() {
                    @Override
                    public void onResult(Wallet wallet) {
                        try { RealmUtil.loadWallet(); }
                        catch (Exception e) { e.printStackTrace(); }
                        setPRepsData(cachedIcxBalance, cachedEthBalance, cachedErrBalance, cachedpRepsData);
                    }
                });
            } break;
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}