package com.rokid.udpbroadcast.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rokid.udpbroadcast.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListAdapter extends BaseAdapter {

    private List<DeviceBean> mDeviceList;
    private Context mContext;

    public DeviceListAdapter(Context context) {
        mContext = context;
        mDeviceList = new ArrayList<>();
    }

    public void clear() {
        mDeviceList.clear();
        this.notifyDataSetChanged();
    }

    public void addAll(Set deviceSet) {
        mDeviceList.addAll(deviceSet);
        this.notifyDataSetChanged();
    }

    public void add(DeviceBean device) {
        mDeviceList.add(device);
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mHolder;
        if (convertView == null) {
            mHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.device_list_item, null, true);
            mHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            mHolder.tv_ip = (TextView) convertView.findViewById(R.id.tv_ip);
            mHolder.tv_online = (TextView) convertView.findViewById(R.id.tv_online);
            convertView.setTag(mHolder);
        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }

        String name = mDeviceList.get(position).getName();
        String ip = mDeviceList.get(position).getIp();
        String online = mDeviceList.get(position).getRoom();

        mHolder.tv_name.setText(name);
        mHolder.tv_ip.setText(ip);
        mHolder.tv_online.setText(online);
        return convertView;
    }

    class ViewHolder {
        private TextView tv_name;
        private TextView tv_ip;
        private TextView tv_online;
    }
}
