package com.gjermundbjaanes.beaconmqtt.newbeacon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gjermundbjaanes.beaconmqtt.R;

import java.util.ArrayList;
import java.util.List;

public class BeaconListAdapter extends BaseAdapter {

    private final Context context;
    private LayoutInflater layoutInflater;
    private List<BeaconListElement> beacons;

    BeaconListAdapter(Context context) {
        this.context = context;

        beacons = new ArrayList<>();
        layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    void updateBeacons(List<BeaconListElement> beacons) {
        this.beacons = beacons;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Object getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = layoutInflater.inflate(R.layout.list_new_beacon_layout, parent, false);
        BeaconListElement beacon = beacons.get(position);

        TextView uuidView = (TextView) rowView.findViewById(R.id.beacon_uuid);
        String uuid = beacon.getUuid();
        if (beacon.isSaved()) {
            uuid = context.getString(R.string.new_beacon_details_already_saved) + " " + uuid;
        }
        uuidView.setText(uuid);

        TextView macView = (TextView) rowView.findViewById(R.id.beacon_mac);
        macView.setText(beacon.getMac());

        TextView detailsView = (TextView) rowView.findViewById(R.id.beacon_details);
        detailsView.setText(context.getString(R.string.beacon_details, beacon.getMajor(), beacon.getMinor()));

        return rowView;
    }
}
