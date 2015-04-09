package com.research.itsl.ism_d2d;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

    private List<WifiP2pDevice> items;

    /**
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public WiFiPeerListAdapter(Context context, int textViewResourceId,
                               List<WifiP2pDevice> objects) {
        super(context, textViewResourceId, objects);
        items = objects;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = View.inflate(getContext(), R.layout.device_cell_layout, null);
        }
        WifiP2pDevice device = items.get(position);
        if (device != null) {
            TextView nameView = (TextView) v.findViewById(R.id.device_cell_name);
            nameView.setText(device.deviceName);
            TextView addressView=(TextView) v.findViewById(R.id.device_cell_address);
            addressView.setText(device.deviceAddress);
        }

        return v;

    }
}