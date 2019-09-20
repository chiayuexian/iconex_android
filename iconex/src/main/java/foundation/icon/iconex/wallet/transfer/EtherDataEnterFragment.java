package foundation.icon.iconex.wallet.transfer;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.spongycastle.util.encoders.Hex;

import java.util.Locale;

import foundation.icon.MyConstants;
import foundation.icon.iconex.R;
import foundation.icon.iconex.dev_dialogs.MessageDialog;
import foundation.icon.iconex.dialogs.BasicDialog;
import foundation.icon.iconex.util.Utils;
import foundation.icon.iconex.wallet.transfer.data.InputData;
import foundation.icon.iconex.widgets.MyEditText;
import kotlin.jvm.functions.Function1;

public class EtherDataEnterFragment extends Fragment {

    private static final String TAG = EtherDataEnterFragment.class.getSimpleName();

    private MyEditText editData;

    private ViewGroup layoutComplete;
    private Button btnComplete;

    private Button btnClose;
    private TextView btnOption;

    private State state;
    public enum State {
        INPUT,
        VIEW
    }

    private OnEnterDataLisnter mListener;
    public interface OnEnterDataLisnter {
        void onSetData(String data);

        void onDataCancel(String data);

        void onDataDelete();
    }

    public EtherDataEnterFragment() {
        // Required empty public constructor
    }

    public static EtherDataEnterFragment newInstance(String data) {
        EtherDataEnterFragment fragment = new EtherDataEnterFragment();
        Bundle args = new Bundle();
        args.putSerializable("data", data);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        View v = inflater.inflate(R.layout.fragment_enter_data_ether, container, false);

        btnClose = v.findViewById(R.id.btn_close);
        btnOption = v.findViewById(R.id.btn_option);
        btnComplete = v.findViewById(R.id.btn_complete);
        layoutComplete = v.findViewById(R.id.layout_complete);
        editData = v.findViewById(R.id.edit_data);

        if (getArguments() != null)
            editData.setText(((String) getArguments().get("data")));

        // init edit data
        editData.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnComplete.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editData.setHint(R.string.hintHexData);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageDialog messageDialog = new MessageDialog(getActivity());
                messageDialog.setSingleButton(false);
                messageDialog.setTitleText(getString(R.string.cancelEnterData));
                messageDialog.setOnConfirmClick(new Function1<View, Boolean>() {
                    @Override
                    public Boolean invoke(View view) {
                        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                        if (mListener != null)
                            mListener.onDataCancel(editData.getText().toString());
                        return true;
                    }
                });
                messageDialog.show();
            }
        });

        btnOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == State.VIEW) {
                    // btn_option.getText() == Modify
                    btnOption.setText(getString(R.string.delete));
                    editData.setSelection(editData.getText().toString().length());
                    layoutComplete.setVisibility(View.VISIBLE);

                    editData.setFocusableInTouchMode(true);
                    editData.requestFocus();

                    state = State.INPUT;
                } else {
                    // btn_option.getText() == Delete
                    MessageDialog messageDialog = new MessageDialog(getActivity());
                    messageDialog.setTitleText(getString(R.string.msgDeleteData));
                    messageDialog.setSingleButton(false);
                    messageDialog.setOnConfirmClick(new Function1<View, Boolean>() {
                        @Override
                        public Boolean invoke(View view) {
                            if (mListener != null)
                                mListener.onDataDelete();
                            return true;
                        }
                    });
                    messageDialog.show();
                }
            }
        });

        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.onSetData(editData.getText().toString());
            }
        });

        if (editData.getText().length() > 0) {
            // view mode
            state = State.VIEW;
            editData.setFocusable(false);
            btnOption.setText(getString(R.string.modified));
            layoutComplete.setVisibility(View.GONE);
        }

        btnOption.setEnabled(editData.getText().length() > 0);
        editData.requestFocus();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEnterDataLisnter) {
            mListener = (OnEnterDataLisnter) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnEnterDataLisnter");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
