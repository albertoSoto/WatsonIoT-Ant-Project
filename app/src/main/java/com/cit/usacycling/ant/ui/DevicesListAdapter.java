package com.cit.usacycling.ant.ui;

import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.enums.AntDeviceConnection;
import com.cit.usacycling.ant.enums.BSXDeviceConnection;
import com.cit.usacycling.ant.enums.DeviceStatus;
import com.cit.usacycling.ant.enums.ServiceState;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.SharedSettings;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by mrshah on 10/9/2015.
 * <p/>
 * List Adapter for the devices found in search
 */
public class DevicesListAdapter extends BaseAdapter {

    public static List<DevicesListItem> devicesListItems;
    private final LayoutInflater layoutInflater;
    private final Context context;
    @Inject
    SharedSettings settings;
    @Inject
    UnitOfWork unitOfWork;
    private int viewId;

    /*
        Constructor with initial list of devices
    */
    DevicesListAdapter(Context context, int viewId, List<DevicesListItem> list) {
        this(context, viewId);
        devicesListItems = list;
    }

    /*
        Constructor with blank list of devices
    */
    DevicesListAdapter(Context context, int viewId) {
        this.context = context;
        USACyclingApplication.getObjectGraph().inject(this);
        layoutInflater = LayoutInflater.from(this.context);
        this.viewId = viewId;
        devicesListItems = new ArrayList<>();
    }

    /*
        Set the list of devices
    */
    public void set(List<DevicesListItem> list) {
        devicesListItems = list;
        notifyDataSetChanged();
    }

    public void update(DevicesListItem item) {
        for (int i = 0; i < devicesListItems.size(); i++) {
            DevicesListItem itemInList = devicesListItems.get(i);
            if (itemInList.getNumber().equals(item.getNumber())) {
                devicesListItems.remove(i);
                devicesListItems.add(i, item);
                break;
            }
        }
        notifyDataSetChanged();
    }

    /*
        Add a device to the list of devices
    */
    public void add(DevicesListItem devicesListItem) {
        devicesListItems.add(devicesListItem);
    }

    @Override
    public int getCount() {
        return devicesListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return devicesListItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
        Create the view for each row
    */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder widgets;
        if (convertView != null) {
            widgets = (ViewHolder) convertView.getTag();
        } else {
            convertView = layoutInflater.inflate(this.viewId, null);
            widgets = new ViewHolder(convertView);
            convertView.setTag(widgets);
        }

        DevicesListItem dev = unitOfWork.getDeviceRepository().getDeviceByNumber(devicesListItems.get(position).getNumber());

        if (dev == null) {
            dev = devicesListItems.get(position);
        }

        String devType = dev.getType();
        widgets.tv_name.setText(dev.getName());
        widgets.tv_type.setText(devType);
        widgets.tv_number.setText(dev.getNumber());

        if (widgets.tv_deviceStatus != null) {
            widgets.tv_deviceStatus.setText(dev.isPaired() ? DeviceStatus.PAIRED.toString() : DeviceStatus.NOT_PAIRED.toString());
        }

        if (widgets.tv_deviceConnection != null) {
            widgets.tv_deviceConnection.setText(
                    devType.equals(DeviceType.BIKE_POWER.toString()) || devType.equals(DeviceType.HEARTRATE.toString())
                            ? AntDeviceConnection.DEAD.toString()
                            : BSXDeviceConnection.DISCONNECTED.toString());
        }

        HashSet<String> activeDevices = unitOfWork.getDeviceRepository().getAllActiveDevices();

        if (activeDevices.contains(dev.getNumber()) && (widgets.tv_serviceStatus != null && widgets.tv_deviceConnection != null)) {
            widgets.tv_deviceConnection.setText(
                    devType.equals(DeviceType.BIKE_POWER.toString()) || devType.equals(DeviceType.HEARTRATE.toString())
                            ? DeviceState.getValueFromInt(settings.getDeviceLastStatus(dev.getNumber())).toString()
                            : BSXDeviceConnection.getStateById(settings.getDeviceLastStatus(dev.getNumber())));
            widgets.tv_serviceStatus.setText(ServiceState.ENABLED.toString());
            if (devType.equals(Constants.BSX_DEVICE_TYPE)) {
                Log.d("STATUS", dev.getStatus());
                if (dev.getStatus().equals(BSXDeviceConnection.CONNECTED.toString())) {
                    convertView.setBackgroundColor(ActivityCompat.getColor(context, android.R.color.holo_green_light));
                } else {
                    convertView.setBackgroundColor(ActivityCompat.getColor(context, android.R.color.holo_orange_light));
                }
            } else {
                convertView.setBackgroundColor(ActivityCompat.getColor(context, android.R.color.holo_green_light));
            }
        } else {
            if (widgets.tv_serviceStatus != null) {
                widgets.tv_serviceStatus.setText(ServiceState.DISABLED.toString());
            }
            convertView.setBackgroundColor(ActivityCompat.getColor(context, android.R.color.transparent));
            widgets.tv_deviceConnection.setText(
                    devType.equals(DeviceType.BIKE_POWER.toString()) || devType.equals(DeviceType.HEARTRATE.toString())
                            ? AntDeviceConnection.DEAD.toString()
                            : BSXDeviceConnection.DISCONNECTED.toString());
            if (devType.equals(Constants.BSX_DEVICE_TYPE)) {
                if (dev.getStatus().equals(BSXDeviceConnection.DISCONNECTED.toString())) {
                    convertView.setBackgroundColor(ActivityCompat.getColor(context, android.R.color.transparent));
                } else {
                    convertView.setBackgroundColor(ActivityCompat.getColor(context, android.R.color.holo_red_light));
                }
            }
        }

        return convertView;
    }

    static class ViewHolder {
        @Bind(R.id.textView_devName)
        TextView tv_name;
        @Bind(R.id.textView_devType)
        TextView tv_type;
        @Bind(R.id.textView_devNo)
        TextView tv_number;
        @Bind(R.id.textView_deviceStatus)
        TextView tv_deviceStatus;
        @Bind(R.id.textView_deviceConnection)
        TextView tv_deviceConnection;
        @Bind(R.id.textView_serviceStatus)
        TextView tv_serviceStatus;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
