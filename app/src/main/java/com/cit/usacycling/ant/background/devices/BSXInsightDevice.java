package com.cit.usacycling.ant.background.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.enums.BSXDeviceConnection;
import com.cit.usacycling.ant.enums.DeviceDataType;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.DataCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 18.11.2015
 */
public class BSXInsightDevice {
    Context adopterService;
    String address;
    Queue<Object> sWriteQueue;
    boolean isWriting;
    BluetoothGatt mGatt;
    String sServiceUUID = "2e4ed00a-d9f0-5490-ff4b-d17374c433ef";
    String sCharacUUID = "2e4ee00a-d9f0-5490-ff4b-d17374c433ef";
    String sCharToNotify = "2e4ee00d-d9f0-5490-ff4b-d17374c433ef";
    private Handler mhSendValue;
    private float mfSMO2;
    private BluetoothDevice BSXDevice;
    private BluetoothAdapter mBTAdapter;
    private boolean mConnected;

    @Inject
    DataCollector collector;

    public BSXInsightDevice(BluetoothDevice device, Context adopterServiceContext) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.sWriteQueue = new ConcurrentLinkedQueue<Object>();
        this.isWriting = false;
        this.mGatt = null;
        this.mhSendValue = new Handler();
        this.adopterService = adopterServiceContext;
        this.address = device.getAddress();
        this.BSXDevice = device;
        this.mfSMO2 = 102.3f;
        this.mBTAdapter = null;
        this.mConnected = false;
    }


    private void updateStatus(BSXDeviceConnection sStatus) {
        Intent deviceStateIntent = new Intent();
        deviceStateIntent.setAction(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        deviceStateIntent.putExtra(Constants.STATE_EXTRA, sStatus.getConnectionId());
        deviceStateIntent.putExtra(Constants.NUMBER_EXTRA, address);
        adopterService.sendBroadcast(deviceStateIntent);
        Log.d("BSX", "Broadcast Sent");
    }

    public boolean connectGatt() {
        boolean result = false;
        try {
            BSXDevice.connectGatt(adopterService, true, mGattCallback);
            updateStatus(BSXDeviceConnection.CONNECTING);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean disconnectGatt() {
        try {
            if (mGatt != null) {
                Log.d("BSXConn", "Stopping BSX Device " + address);
                updateStatus(BSXDeviceConnection.TURNING_OFF);
                BluetoothGattService s = mGatt.getService(UUID.fromString(sServiceUUID));
                BluetoothGattCharacteristic ch = s.getCharacteristic(UUID.fromString(sCharacUUID));
                byte[] b1 = {0x04, 0x00};
                ch.setValue(b1);
                write(ch);
            }

            mhSendValue.removeCallbacks(rSendValue);
            Handler h = new Handler();
            h.postDelayed(disconnectCommand, 300);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    private synchronized void doWrite(Object o) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (o instanceof BluetoothGattCharacteristic) {
            isWriting = true;
            Log.d("BSXWriteChar", "" + mGatt.writeCharacteristic((BluetoothGattCharacteristic) o));
        } else if (o instanceof BluetoothGattDescriptor) {
            isWriting = true;
            Log.d("BSXWriteDesc", "" + mGatt.writeDescriptor((BluetoothGattDescriptor) o));
        } else {
            nextWrite();
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            isWriting = false;
            nextWrite();
            Log.d("BSXCharWrite", "Completed");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (gatt.getDevice().getAddress().equals(address)) {
                isWriting = false;
                nextWrite();
                Log.d("BSXDescWrite", "Completed");
                BluetoothGattCharacteristic ch1 = gatt.getService(UUID.fromString("2e4ed00a-d9f0-5490-ff4b-d17374c433ef")).getCharacteristic(UUID.fromString(sCharToNotify));
                gatt.setCharacteristicNotification(ch1, true);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            boolean sendState = false;
            BSXDeviceConnection sStatus = BSXDeviceConnection.UNKNOWN;
            Log.d("BSXDevice", "Address = " + gatt.getDevice().getAddress());
            Log.d("BSXDevice", "Connection State = " + newState);
            Log.d("BSXDevice", "Connection Status = " + status);
            if (gatt.getDevice().getAddress().equals(address)) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    sStatus = BSXDeviceConnection.DISCONNECTED;
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    sendState = true;
                    Log.d("BSXCon", "Connected " + address);
                    gatt.discoverServices();
                    mGatt = gatt;
                    mConnected = true;
                    sStatus = BSXDeviceConnection.STARTING;
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED && mConnected == true) {
                    sendState = true;
                    Log.d("BSXCon", "Disconnected " + address);
                    mhSendValue.removeCallbacks(rSendValue);
                    mConnected = false;
                    mGatt = null;
//                    scanLeDevice();
                    sStatus = BSXDeviceConnection.DISCONNECTED;
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED && mConnected == false) {
                    mGatt = null;
                    Log.d("BSXCon", "Disconnected " + address + ", Wasn't yet connected");
//                    gatt.connect();
                } else {
                    Log.d("BSX", "Connection State Unknown");
                }
            }

            updateStatus(sStatus);
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (gatt.getDevice().getAddress().equals(address)) {
                Log.d("BSXCon", "Services Discovered - " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BSXCon", "Services Discovered");
                    BluetoothGattService s = gatt.getService(UUID.fromString(sServiceUUID));
                    BluetoothGattCharacteristic ch = s.getCharacteristic(UUID.fromString(sCharacUUID));
                    byte[] b1 = {0x04, 0x00};
                    ch.setValue(b1);
                    write(ch);
                    ch.setValue(new byte[]{0x04, 0x01});
                    write(ch);
                    BluetoothGattCharacteristic ch1 = gatt.getService(UUID.fromString("2e4ed00a-d9f0-5490-ff4b-d17374c433ef")).getCharacteristic(UUID.fromString(sCharToNotify));
                    UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = ch1.getDescriptor(uuid);

                    Log.d("BSXDescSetValue", "" + descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                    write(descriptor);
                    mhSendValue.postDelayed(rSendValue, 1000);
                    updateStatus(BSXDeviceConnection.CONNECTED);
                } else {
                }
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d("BSX Read", "value");
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (gatt.getDevice().getAddress().equals(address)) {
                byte[] val = characteristic.getValue();
                ByteBuffer bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(val[18]);
                bb.put(val[19]);

                short shortVal = bb.getShort(0);
                float fVal = (float) ((float) shortVal / 10.0);

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%02X ", val[19]));
                sb.append(String.format("%02X ", val[18]));

                Log.d("BSX Changed", "shortVal = " + shortVal);
                Log.d("BSX Changed", "fVal = " + fVal);
                Log.d("BSX Changed", "bytes = " + sb);

                mfSMO2 = fVal;
            }
        }
    };

    public void scanLeDevice() {
        if (mBTAdapter == null) {
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        mBTAdapter.startLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String deviceAddress = device.getAddress();

                    if (device.getAddress() != null && deviceAddress.equals(address)) {
                        connectGatt();
                    }
                }
            };

    public DeviceDataTypeStruct parseDeviceDataTypeStruct(String deviceId, String deviceType, DeviceDataType dataType) {
        return new DeviceDataTypeStruct(deviceId, deviceType, dataType);
    }

    private synchronized void write(Object o) {
        if (sWriteQueue.isEmpty() && !isWriting) {
            doWrite(o);
        } else {
            sWriteQueue.add(o);
        }
    }

    private synchronized void nextWrite() {
        if (!sWriteQueue.isEmpty() && !isWriting) {
            doWrite(sWriteQueue.poll());
        }
    }

    private Runnable rSendValue = new Runnable() {
        @Override
        public void run() {
            if (mfSMO2 < 0) {

            } else {
                JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, mfSMO2);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    mfSMO2 = -1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                collector.addData(
                        parseDeviceDataTypeStruct(
                                address,
                                Constants.BSX_DEVICE_TYPE,
                                DeviceDataType.SMO2),
                        data);
            }
            mhSendValue.postDelayed(rSendValue, 500);
        }
    };

    private Runnable disconnectCommand = new Runnable() {

        @Override
        public void run() {
            if (sWriteQueue.isEmpty()) {
                if (mGatt != null) {
                    mGatt.close();
//                    mGatt.disconnect();
                }
                updateStatus(BSXDeviceConnection.DISCONNECTED);
            } else {
                Handler h = new Handler();
                h.postDelayed(disconnectCommand, 300);
            }
        }
    };
}
