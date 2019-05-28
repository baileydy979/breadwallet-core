package com.breadwallet.cryptodemo;

import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.crypto.System;
import com.breadwallet.crypto.Transfer;
import com.breadwallet.crypto.TransferHash;
import com.breadwallet.crypto.Wallet;
import com.breadwallet.crypto.WalletManager;
import com.breadwallet.crypto.events.transfer.TranferEvent;
import com.breadwallet.crypto.events.transfer.TransferChangedEvent;
import com.breadwallet.crypto.events.transfer.TransferConfirmationEvent;
import com.breadwallet.crypto.events.transfer.TransferCreatedEvent;
import com.breadwallet.crypto.events.transfer.TransferDeletedEvent;
import com.breadwallet.crypto.events.transfer.TransferEventVisitor;
import com.breadwallet.crypto.events.transfer.TransferListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WalletTransfersActivity extends AppCompatActivity implements TransferListener {

    private Wallet wallet;
    private List<Transfer> transfers;

    private RecyclerView transfersView;
    private RecyclerView.Adapter transferAdapter;
    private RecyclerView.LayoutManager transferLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_transfers);

        // TODO: Get this properly via an intent
        wallet = CoreCryptoApplication.system.getWallets().get(0);
        transfers = new ArrayList<>();

        transfersView = findViewById(R.id.transfer_recycler_view);
        transfersView.hasFixedSize();
        transfersView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));

        transferLayoutManager = new LinearLayoutManager(this);
        transfersView.setLayoutManager(transferLayoutManager);

        transferAdapter = new Adapter(transfers);
        transfersView.setAdapter(transferAdapter);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(String.format("Wallet: %s", wallet.getName()));

        CoreCryptoApplication.listener.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        transfers.clear();
        transfers.addAll(wallet.getTransfers());

        transferAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CoreCryptoApplication.listener.removeListener(this);

        transfers.clear();
        wallet = null;
    }

    @Override
    public void handleTransferEvent(System system, WalletManager manager, Wallet wallet, Transfer transfer, TranferEvent event) {
        runOnUiThread(() -> {
            event.accept(new TransferEventVisitor<Void>() {
                @Override
                public Void visit(TransferChangedEvent event) {
                    int index = transfers.indexOf(wallet);
                    if (index != -1) {
                        transferAdapter.notifyItemChanged(index);
                    }
                    return null;
                }

                @Override
                public Void visit(TransferConfirmationEvent event) {
                    int index = transfers.indexOf(wallet);
                    if (index != -1) {
                        transferAdapter.notifyItemChanged(index);
                    }
                    return null;
                }

                @Override
                public Void visit(TransferCreatedEvent event) {
                    int index = transfers.indexOf(transfer);
                    if (index == -1) {
                        index = transfers.size();
                        transfers.add(index, transfer);
                        transferAdapter.notifyItemInserted(index);
                    }
                    return null;
                }

                @Override
                public Void visit(TransferDeletedEvent event) {
                    int index = transfers.indexOf(wallet);
                    if (index != -1) {
                        transfers.remove(index);
                        transferAdapter.notifyItemRemoved(index);
                    }
                    return null;
                }
            });
        });
    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private List<Transfer> transfers;

        Adapter(List<Transfer> transfers) {
            this.transfers = transfers;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_wallet_transfer_item, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder vh, int i) {
            Transfer transfer = transfers.get(i);

            DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
            String dateText = transfer.getConfirmation()
                    .transform((conf) -> dateFormatter.format(new Date(conf.timestamp * 1000))).or("<pending>");

            String addressText = transfer.getHash().transform(TransferHash::toString).or("<pending>");
            addressText = String.format("Hash: %s", addressText);

            String amountText = transfer.getAmountDirected().toString();
            String feeText = String.format("Fee: %s", transfer.getFee());

            String stateText = String.format("State: %s", transfer.getState());

            vh.dateView.setText(dateText);
            vh.amountView.setText(amountText);
            vh.addressView.setText(addressText);
            vh.feeView.setText(feeText);
            vh.stateView.setText(stateText);
        }

        @Override
        public int getItemCount() {
            return transfers.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView dateView;
        public TextView amountView;
        public TextView addressView;
        public TextView feeView;
        public TextView stateView;

        public ViewHolder(@NonNull View view) {
            super(view);

            dateView = view.findViewById(R.id.item_date);
            amountView = view.findViewById(R.id.item_amount);
            addressView = view.findViewById(R.id.item_address);
            feeView = view.findViewById(R.id.item_fee);
            stateView = view.findViewById(R.id.item_state);
        }
    }
}