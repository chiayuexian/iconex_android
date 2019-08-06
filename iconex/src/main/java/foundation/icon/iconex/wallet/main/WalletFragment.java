package foundation.icon.iconex.wallet.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import foundation.icon.ICONexApp;
import foundation.icon.MyConstants;
import foundation.icon.iconex.R;
import foundation.icon.iconex.control.BottomSheetMenu;
import foundation.icon.iconex.dialogs.Basic2ButtonDialog;
import foundation.icon.iconex.dialogs.BottomSheetMenuDialog;
import foundation.icon.iconex.dialogs.EditTextDialog;
import foundation.icon.iconex.view.IntroActivity;
import foundation.icon.iconex.menu.WalletBackUpActivity;
import foundation.icon.iconex.menu.WalletPwdChangeActivity;
import foundation.icon.iconex.realm.RealmUtil;
import foundation.icon.iconex.token.manage.TokenManageActivity;
import foundation.icon.iconex.util.ConvertUtil;
import foundation.icon.iconex.util.Utils;
import foundation.icon.iconex.wallet.Wallet;
import foundation.icon.iconex.wallet.WalletEntry;
import foundation.icon.iconex.wallet.detail.WalletDetailActivity;
import loopchain.icon.wallet.core.Constants;
import loopchain.icon.wallet.service.crypto.KeyStoreUtils;

import static foundation.icon.MyConstants.EXCHANGE_USD;

public class WalletFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = WalletFragment.class.getSimpleName();

    private Context mContext;

    private Wallet mWallet;
    private List<WalletEntry> mWalletEntries;
    private WalletEntry mToken;

    private TextView txtWalletAlias;
    private TextView txtBalanceUnit;
    private Button btnViewAddress, btnWalletMenu;
    private TextView txtTotalBalance;

    private ViewGroup loadingBalance;

    private RecyclerView entryRecyclerView;
    private WalletRecyclerAdapter entryRecyclerAdapter;

    private final int RC_CHANGE_PWD = 101;
    private final int RC_DETAIL = 201;
    private final int RC_SWAP = 301;

    public WalletFragment() {
        // Required empty public constructor
    }

    public static WalletFragment newInstance(Wallet wallet) {
        WalletFragment fragment = new WalletFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("mWallet", wallet);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWallet = (Wallet) getArguments().get("mWallet");
            mWalletEntries = mWallet.getWalletEntries();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_wallet, container, false);

        txtWalletAlias = v.findViewById(R.id.txt_wallet_alias);
        txtWalletAlias.setText(mWallet.getAlias());

        btnViewAddress = v.findViewById(R.id.btn_wallet_address);
        btnViewAddress.setOnClickListener(this);
        btnWalletMenu = v.findViewById(R.id.btn_wallet_menu);
        btnWalletMenu.setOnClickListener(this);

        txtTotalBalance = v.findViewById(R.id.txt_total_balance);
        txtBalanceUnit = v.findViewById(R.id.txt_balance_unit);

        loadingBalance = v.findViewById(R.id.loading_balance);

        entryRecyclerView = v.findViewById(R.id.recycler_coins);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        walletNotifyDataChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_wallet_address:
                if (mWallet.getCoinType().equals(Constants.KS_COINTYPE_ETH))
                    ((MainActivity) getActivity()).showWalletAddress(mWallet.getAlias(), MyConstants.PREFIX_HEX + mWallet.getAddress());
                else
                    ((MainActivity) getActivity()).showWalletAddress(mWallet.getAlias(), mWallet.getAddress());

                break;

            case R.id.btn_wallet_menu:
                BottomSheetMenuDialog menuDialog = new BottomSheetMenuDialog(getActivity(), getString(R.string.manageWallet),
                        BottomSheetMenuDialog.SHEET_TYPE.MENU);
                menuDialog.setMenuData(makeMenus());
                menuDialog.setOnItemClickListener(menuListener);
                menuDialog.show();
                break;
        }
    }

    private void setTotalBalance() {
        Double totalBalance = 0.0;
        int cntNoBalance = 0;
        String unit = ((MainActivity) getActivity()).getExchangeUnit();
        for (WalletEntry entry : mWallet.getWalletEntries()) {
            if (!entry.getBalance().isEmpty()) {
                if (entry.getBalance().equals(MyConstants.NO_BALANCE)) {
                    cntNoBalance++;
                } else {
                    try {
                        BigInteger balance = new BigInteger(entry.getBalance());
                        String value = ConvertUtil.getValue(balance, entry.getDefaultDec());
                        Double doubBalance = Double.parseDouble(value);

                        String exchange = entry.getSymbol().toLowerCase() + unit.toLowerCase();
                        String strPrice;
                        if (exchange.equals("etheth"))
                            strPrice = "1";
                        else
                            strPrice = ICONexApp.EXCHANGE_TABLE.get(exchange);

                        if (strPrice != null) {
                            totalBalance += doubBalance * Double.parseDouble(strPrice);
                        }
                    } catch (Exception e) {
                        // Do nothing.
                    }
                }
            }
        }

        if (cntNoBalance == mWallet.getWalletEntries().size()) {
            txtTotalBalance.setText(MyConstants.NO_BALANCE);
        } else {
            if (unit.equals(EXCHANGE_USD)) {
                txtTotalBalance.setText(String.format(Locale.getDefault(), "%,.2f", totalBalance));
            } else {
                txtTotalBalance.setText(String.format(Locale.getDefault(), "%,.4f", totalBalance));
            }
        }

        txtBalanceUnit.setText(unit);
    }

    private ArrayList<BottomSheetMenu> makeMenus() {
        ArrayList<BottomSheetMenu> menus = new ArrayList<>();
        BottomSheetMenu menu = new BottomSheetMenu(R.drawable.ic_edit, mWallet.getAlias());
        menu.setTag(MyConstants.TAG_MENU_ALIAS);
        menus.add(menu);

        menu = new BottomSheetMenu(R.drawable.ic_setting, getString(R.string.menuManageToken));
        menu.setTag(MyConstants.TAG_MENU_TOKEN);
        menus.add(menu);

        menu = new BottomSheetMenu(R.drawable.ic_backup, getString(R.string.menuBackupWallet));
        menu.setTag(MyConstants.TAG_MENU_BACKUP);
        menus.add(menu);

        menu = new BottomSheetMenu(R.drawable.ic_side_lock, getString(R.string.menuChangePwd));
        menu.setTag(MyConstants.TAG_MENU_PWD);
        menus.add(menu);

        menu = new BottomSheetMenu(R.drawable.ic_delete, getString(R.string.menuDeleteWallet));
        menu.setTag(MyConstants.TAG_MENU_REMOVE);
        menus.add(menu);

        return menus;
    }

    private BottomSheetMenuDialog.OnItemClickListener menuListener = new BottomSheetMenuDialog.OnItemClickListener() {
        @Override
        public void onBasicItem(String item) {

        }

        @Override
        public void onCoinItem(int position) {

        }

        @Override
        public void onMenuItem(String tag) {
            switch (tag) {
                case MyConstants.TAG_MENU_ALIAS:
                    editTextDialog = new EditTextDialog(getActivity(), getString(R.string.modWalletAlias));
                    editTextDialog.setHint(getString(R.string.hintWalletAlias));
                    editTextDialog.setInputType(EditTextDialog.TYPE_INPUT.ALIAS);
                    editTextDialog.setAlias(mWallet.getAlias());
                    editTextDialog.setOnConfirmCallback(mAliasDialogCallback);
                    editTextDialog.show();
                    break;

                case MyConstants.TAG_MENU_TOKEN:
                    Intent intent = new Intent(mContext, TokenManageActivity.class);
                    intent.putExtra("walletInfo", (Serializable) mWallet);

                    if (mWallet.getCoinType().equals(Constants.KS_COINTYPE_ICX))
                        intent.putExtra("type", TokenManageActivity.TOKEN_TYPE.IRC);
                    else
                        intent.putExtra("type", TokenManageActivity.TOKEN_TYPE.ERC);

                    startActivity(intent);
                    break;

                case MyConstants.TAG_MENU_BACKUP:
                    editTextDialog = new EditTextDialog(getActivity(), getString(R.string.enterWalletPassword));
                    editTextDialog.setHint(getString(R.string.hintWalletPassword));
                    editTextDialog.setInputType(EditTextDialog.TYPE_INPUT.PASSWORD);
                    editTextDialog.setPasswordType(EditTextDialog.RESULT_PWD.BACKUP);
                    editTextDialog.setOnPasswordCallback(mPasswordDialogCallback);
                    editTextDialog.show();
                    break;

                case MyConstants.TAG_MENU_PWD:
                    startActivityForResult(new Intent(getActivity(), WalletPwdChangeActivity.class)
                            .putExtra("walletInfo", (Serializable) mWallet), RC_CHANGE_PWD);
                    break;

                case MyConstants.TAG_MENU_REMOVE:
                    BigInteger asset = getAsset();
                    final Basic2ButtonDialog dialog = new Basic2ButtonDialog(getActivity());
                    if (asset.compareTo(BigInteger.ZERO) == 0
                            && loadingBalance.getVisibility() == View.GONE) {
                        dialog.setMessage(getString(R.string.removeWallet));
                        dialog.setOnDialogListener(new Basic2ButtonDialog.OnDialogListener() {
                            @Override
                            public void onOk() {
                                RealmUtil.removeWallet(mWallet.getAddress());
                                try {
                                    RealmUtil.loadWallet();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (ICONexApp.mWallets.size() == 0) {
                                    startActivity(new Intent(getActivity(), IntroActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                                } else {
                                    ((MainActivity) getActivity()).notifyWalletChanged();
                                }

                                dialog.dismiss();
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                        dialog.show();
                    } else {
                        dialog.setMessage(getString(R.string.warningRemoveWallet));
                        dialog.setOnDialogListener(new Basic2ButtonDialog.OnDialogListener() {
                            @Override
                            public void onOk() {
                                editTextDialog = new EditTextDialog(getActivity(), getString(R.string.enterWalletPassword));
                                editTextDialog.setHint(getString(R.string.hintWalletPassword));
                                editTextDialog.setInputType(EditTextDialog.TYPE_INPUT.PASSWORD);
                                editTextDialog.setPasswordType(EditTextDialog.RESULT_PWD.REMOVE);
                                editTextDialog.setOnPasswordCallback(mPasswordDialogCallback);
                                editTextDialog.show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                        dialog.show();
                    }
                    break;
            }
        }
    };

    private BigInteger getAsset() {
        BigInteger asset = new BigInteger("0");
        for (WalletEntry entry : mWallet.getWalletEntries()) {
            try {
                BigInteger balance = new BigInteger(entry.getBalance());
                asset = asset.add(balance);
            } catch (Exception e) {
                // Do nothing.
            }
        }

        return asset;
    }

    public void walletNotifyDataChanged() {

        loadingBalance.setVisibility(View.VISIBLE);

        for (Wallet wallet : ICONexApp.mWallets) {
            if (wallet.getAddress().equals(mWallet.getAddress())) {
                mWallet = wallet;
                break;
            }
        }

        if (loadingBalance.getVisibility() == View.VISIBLE) {

            boolean isDone = true;
            for (WalletEntry entry : mWallet.getWalletEntries()) {
                if (entry.getBalance().isEmpty()) {
                    isDone = isDone && false;
                } else {
                    isDone = isDone && true;
                }
            }

            if (isDone) {
                loadingBalance.setVisibility(View.GONE);
            }
        }

        setTotalBalance();

        entryRecyclerAdapter = new WalletRecyclerAdapter(getActivity(), mWallet);
        entryRecyclerAdapter.setClickListener(new WalletRecyclerAdapter.ItemClickListener() {
            @Override
            public void onItemClick(WalletEntry walletEntry) {
                startActivityForResult(new Intent(getActivity(), WalletDetailActivity.class)
                        .putExtra("walletInfo", (Serializable) mWallet)
                        .putExtra("walletEntry", (Serializable) walletEntry), RC_DETAIL);
            }

            @Override
            public void onRequestSwap(WalletEntry own, WalletEntry coin) {

            }
        });
        entryRecyclerView.setAdapter(entryRecyclerAdapter);
    }

    public String getAddress() {
        return mWallet.getAddress();
    }

    private EditTextDialog editTextDialog;

    private EditTextDialog.OnConfirmCallback mAliasDialogCallback = new EditTextDialog.OnConfirmCallback() {
        @Override
        public void onConfirm(String target) {
            String alias = Utils.strip(target);

            if (alias.isEmpty()) {
                editTextDialog.setError(getString(R.string.errWhiteSpace));
                return;
            }

            if (alias.trim().length() == 0) {
                editTextDialog.setError(getString(R.string.errWhiteSpace));
                return;
            }

            for (Wallet info : ICONexApp.mWallets) {
                if (info.getAlias().equals(alias)) {
                    editTextDialog.setError(getString(R.string.duplicateWalletAlias));
                    return;
                }
            }

            RealmUtil.modWalletAlias(mWallet.getAddress(), alias);
            mWallet.setAlias(alias);
            txtWalletAlias.setText(mWallet.getAlias());
            ((MainActivity) getActivity()).refreshNameView();
            editTextDialog.dismiss();
        }
    };

    private EditTextDialog.OnPasswordCallback mPasswordDialogCallback = new EditTextDialog.OnPasswordCallback() {
        @Override
        public void onConfirm(EditTextDialog.RESULT_PWD result, String pwd) {
            JsonObject keyStore = new Gson().fromJson(mWallet.getKeyStore(), JsonObject.class);
            byte[] bytePrivKey;
            try {
                JsonObject crypto = null;
                if (keyStore.has("crypto"))
                    crypto = keyStore.get("crypto").getAsJsonObject();
                else
                    crypto = keyStore.get("Crypto").getAsJsonObject();

                bytePrivKey = KeyStoreUtils.decryptPrivateKey(pwd, mWallet.getAddress(), crypto, mWallet.getCoinType());
                if (bytePrivKey != null) {
                    if (result == EditTextDialog.RESULT_PWD.BACKUP) {
                        startActivity(new Intent(getActivity(), WalletBackUpActivity.class)
                                .putExtra("walletInfo", (Serializable) mWallet)
                                .putExtra("privateKey", Hex.toHexString(bytePrivKey)));
                    } else {
                        RealmUtil.removeWallet(mWallet.getAddress());
                        try {
                            RealmUtil.loadWallet();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (ICONexApp.mWallets.size() == 0) {
                            startActivity(new Intent(getActivity(), IntroActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                        } else {
                            ((MainActivity) getActivity()).notifyWalletChanged();
                        }
                    }

                    editTextDialog.dismiss();
                } else {
                    editTextDialog.setError(getString(R.string.errPassword));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private boolean hasSwapWallet(String address) throws Exception {
        for (Wallet wallet : ICONexApp.mWallets) {
            if (address.equals(wallet.getAddress()))
                return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_DETAIL:
                if (resultCode == WalletDetailActivity.RES_REFRESH) {
                    ((MainActivity) getActivity()).notifyWalletChanged();
                } else {
                    ((MainActivity) getActivity()).refreshNameView();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
