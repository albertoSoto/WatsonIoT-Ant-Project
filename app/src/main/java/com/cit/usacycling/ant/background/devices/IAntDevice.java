package com.cit.usacycling.ant.background.devices;

import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.enums.DeviceDataType;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

/**
 * Created by Nikolay on 1.11.2015
 */
public interface IAntDevice {
    /*
        Returns device's id
     */
    String getDeviceId();

    /*
        Subscribe for ant data
     */
    void subscribeToAntDeviceEvents();

    AntPluginPcc.IPluginAccessResultReceiver<?> getAccessResultReceiver();

    AntPluginPcc.IDeviceStateChangeReceiver getDeviceStateChangeReceiver();

    IAntDevice getInstance();

    DeviceDataTypeStruct parseDeviceDataTypeStruct(String deviceId, DeviceType deviceType, DeviceDataType dataType);
}
