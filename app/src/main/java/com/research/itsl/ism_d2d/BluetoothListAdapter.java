package com.research.itsl.ism_d2d;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class BluetoothListAdapter extends ArrayAdapter<BluetoothDevice> {


    /**
     * @param context
     * @param textViewResourceId
     */
    public BluetoothListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = View.inflate(getContext(), R.layout.device_cell_layout, null);
        }
        BluetoothDevice device = getItem(position);
        if (device != null) {
            TextView nameView = (TextView) v.findViewById(R.id.device_cell_name);
            nameView.setText(device.getName());
            TextView addressView=(TextView) v.findViewById(R.id.device_cell_address);
            addressView.setText(device.getAddress());
        }

        return v;

    }
}