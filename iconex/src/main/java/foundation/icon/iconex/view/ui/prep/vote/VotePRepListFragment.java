package foundation.icon.iconex.view.ui.prep.vote;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import foundation.icon.ICONexApp;
import foundation.icon.iconex.R;
import foundation.icon.iconex.service.PRepService;
import foundation.icon.iconex.util.ConvertUtil;
import foundation.icon.iconex.view.ui.prep.Delegation;
import foundation.icon.iconex.view.ui.prep.PRep;
import foundation.icon.iconex.view.ui.prep.PRepListAdapter;
import foundation.icon.iconex.wallet.Wallet;
import foundation.icon.iconex.widgets.DividerItemDecorator;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class VotePRepListFragment extends Fragment {
    private static final String TAG = VotePRepListFragment.class.getSimpleName();

    private VoteViewModel vm;
    private OnVotePRepListListener mListener;

    private ImageButton btnSearch;
    private RecyclerView list;
    private PRepListAdapter adapter;

    private Wallet wallet;
    private List<PRep> prepList;
    private List<Delegation> delegations = new ArrayList<>();

    private Disposable disposable;

    public VotePRepListFragment() {
        // Required empty public constructor
    }

    public static VotePRepListFragment newInstance() {
        return new VotePRepListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vm = ViewModelProviders.of(getActivity()).get(VoteViewModel.class);
        wallet = vm.getWallet().getValue();
        delegations = vm.getDelegations().getValue();
        prepList = vm.getPreps().getValue();

        vm.getDelegations().observe(this, new Observer<List<Delegation>>() {
            @Override
            public void onChanged(List<Delegation> delegations) {
                VotePRepListFragment.this.delegations = delegations;
                if (adapter != null) {
                    adapter.setDelegations(delegations);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_vote_prep_list, container, false);
        initView(v);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnVotePRepListListener) {
            mListener = (OnVotePRepListListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnVotePRepListListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (prepList != null && prepList.size() > 0) {
            adapter = new PRepListAdapter(getContext(),
                    PRepListAdapter.Type.VOTE,
                    prepList, getActivity());
            adapter.setDelegations(delegations);
            list.setAdapter(adapter);
        }
    }

    private void initView(View v) {
        list = v.findViewById(R.id.list);
        list.setFocusable(false);
        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecorator(
                        getContext(),
                        ContextCompat.getDrawable(getContext(), R.drawable.line_divider));
        list.addItemDecoration(itemDecoration);
    }

    public interface OnVotePRepListListener {
    }
}
