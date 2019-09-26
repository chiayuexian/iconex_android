package foundation.icon.iconex.view.ui.mainWallet.items;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import foundation.icon.iconex.R;
import foundation.icon.iconex.view.ui.mainWallet.viewdata.WalletItemViewData;

public class WalletWalletItem extends WalletItem{

    public ViewGroup layoutWalletItem;

    public TextView txtSymbol;
    public TextView txtName;
    public TextView txtAmount;
    public TextView txtExchanged;

    public WalletWalletItem(@NonNull Context context) {
        super(context);
        initView();
    }

    private void initView() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_wallet_wallet, this, false);

        layoutWalletItem = v.findViewById(R.id.wallet_item_layout);

        txtSymbol = v.findViewById(R.id.txt_symbol);
        txtName = v.findViewById(R.id.txt_name);
        txtAmount = v.findViewById(R.id.txt_amount);
        txtExchanged = v.findViewById(R.id.txt_exchanged);

        addView(v);
    }

    @Override
    public void bind(WalletItemViewData data) {
        txtSymbol.setText(data.getSymbol());
        txtName.setText(data.getName());
        txtAmount.setText(data.getTxtAmount());
        txtExchanged.setText(data.getTxtExchanged());
    }

    @Override
    public void setOnClickWalletItem(OnClickListener listener) {
        layoutWalletItem.setOnClickListener(listener);
    }
}