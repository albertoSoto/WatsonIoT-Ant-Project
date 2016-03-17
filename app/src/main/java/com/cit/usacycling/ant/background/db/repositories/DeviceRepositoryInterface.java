package com.cit.usacycling.ant.background.db.repositories;

import com.cit.usacycling.ant.ui.DevicesListItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public interface DeviceRepositoryInterface {
    boolean insertDevice(DevicesListItem device, boolean setAsPaired);

    HashSet<String> getAllActiveDevices();

    HashSet<DevicesListItem> getAllActiveDevicesDetailed();

    void deactivateAllDevices();

    ArrayList<String> getAllPairedDeviceIds();

    ArrayList<DevicesListItem> getAllPairedDevices();

    DevicesListItem getDeviceByNumber(String number);

    String getDeviceNameByNumber(String number);

    boolean removeDeviceByNumber(String number);

    boolean updateDeviceActiveStatus(String deviceNumber, boolean isActive);

    boolean updateDeviceStatus(String deviceNumber, String status);

    boolean unpairDeviceByNumber(String number);

    boolean updateDeviceNameByNumber(DevicesListItem device, String newName);

    List<DevicesListItem> getAllActivePowerMeters();
}
