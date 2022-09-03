package com.mce.wirelesstransfer.ui;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.mce.wirelesstransfer.R;
import java.util.ArrayList;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ListViewHolder> {

    ArrayList<WifiP2pDevice> list = new ArrayList<>();
    MainActivity activity;

    public DeviceListAdapter(MainActivity activity)
    {
        this.activity = activity;
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.device_layout, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListViewHolder holder, int position) {
        holder.deviceName.setText(list.get(position).deviceName);
        holder.deviceName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.deviceSelected(list.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setDevices(ArrayList<WifiP2pDevice> list)
    {
        this.list = list;
        notifyDataSetChanged();
    }

    class ListViewHolder extends RecyclerView.ViewHolder {

        TextView deviceName;

        ListViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
        }
    }
}
