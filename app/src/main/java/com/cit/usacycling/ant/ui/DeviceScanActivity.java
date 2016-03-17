package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.background.services.BSXInsightsService;
import com.cit.usacycling.ant.background.services.BikePowerWattsMonitorService;
import com.cit.usacycling.ant.background.services.DeviceSearchService;
import com.cit.usacycling.ant.background.services.HeartRateMonitorService;
import com.cit.usacycling.ant.enums.BSXDeviceConnection;
import com.cit.usacycling.ant.enums.DeviceStatus;
import com.cit.usacycling.ant.enums.ServiceState;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.ConnectionWatchStruct;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.SharedSettings;
import com.cit.usacycling.ant.global.receivers.CommonBroadacastReceivers;
import com.cit.usacycling.ant.mock.services.BSXInsightsServiceMock;
import com.cit.usacycling.ant.mock.services.BikePowerWattsMonitorServiceMock;
import com.cit.usacycling.ant.mock.services.DeviceSearchServiceMock;
import com.cit.usacycling.ant.mock.services.HeartRateMonitorServiceMock;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DeviceScanActivity extends Activity {

    @Bind(R.id.listView)
    ListView lv_devices;
    @Inject
    SharedSettings settings;
    @Inject
    BuildProperties buildProperties;
    @Inject
    CToast cToast;
    private BroadcastReceiver updateAdapterReceiver;
    private RenameDeviceAlertDialog renameDialog;

    @Inject
    UnitOfWork unitOfWork;

    private DevicesListAdapter adapter;
    private final ConnectionWatchStruct connectionWatchStruct = new ConnectionWatchStruct(this);
    private final Handler closeDialogNoInetHandler = new Handler();
    private final Runnable closeDialogNoInetRunnable = new Runnable() {
        @Override
        public void run() {
            if (!connectionWatchStruct.isNetworkConnectionAvailable() && renameDialog != null && (renameDialog.getDialog() != null)) {
                renameDialog.getDialog().dismiss();
            }

            closeDialogNoInetHandler.postDelayed(this, 3000);
        }
    };

    private final BroadcastReceiver deviceStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String number = intent.getStringExtra(Constants.NUMBER_EXTRA);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int devCount = lv_devices.getCount();
                    for (int i = 0; i < devCount; i++) {
                        try {
                            View v = lv_devices.getChildAt(i);
                            final int state = intent.getIntExtra(Constants.STATE_EXTRA, 0);
                            settings.setDeviceLastStatus(number, state);
                            if (v != null && ((TextView) v.findViewById(R.id.textView_devNo)).getText().equals(number)) {
                                String devType = ((TextView) v.findViewById(R.id.textView_devType)).getText().toString();
                                if (!(devType.equals(DeviceType.BIKE_POWER.toString()) || devType.equals(DeviceType.HEARTRATE.toString()))) {
                                    String bsxState = String.valueOf(state);
                                    DevicesListItem listItem = unitOfWork.getDeviceRepository().getDeviceByNumber(number);
                                    Log.d("STATE", bsxState);
                                    if (unitOfWork.getDeviceRepository().getAllActiveDevices().contains(number)) {
                                        unitOfWork.getDeviceRepository().updateDeviceStatus(number, bsxState);
                                        listItem.setStatus(bsxState);
                                    } else {
                                        Log.d("MAIN", "BSX CONNECTED");
                                        unitOfWork.getDeviceRepository().updateDeviceStatus(number, bsxState);
                                        listItem.setStatus(bsxState);
                                    }
                                    adapter.update(listItem);
                                } else {
                                    ((TextView) v.findViewById(R.id.textView_deviceConnection))
                                            .setText(DeviceState.getValueFromInt(state).toString());
                                }

                                lv_devices.getFirstVisiblePosition();
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    };

    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String name = intent.getStringExtra(Constants.NAME_EXTRA);
            String type = intent.getStringExtra(Constants.TYPE_EXTRA);
            String number = intent.getStringExtra(Constants.NUMBER_EXTRA);
            boolean isPaired = intent.getBooleanExtra(Constants.PAIRED_EXTRA, false);

            final DevicesListItem d = new DevicesListItem();
            d.setName(name);
            d.setNumber(number);
            d.setType(type);
            d.setIsPaired(isPaired);
            d.setStatus("DISCONNECTED");

            (new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    addDeviceToAdapter(adapter, d);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    updateDeviceAdapter();
                }
            }).execute();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        USACyclingApplication.getObjectGraph().inject(this);
        super.onCreate(savedInstanceState);
        connectionWatchStruct.setConnectionCheck();
        closeDialogNoInetHandler.postDelayed(closeDialogNoInetRunnable, 2000);
        setContentView(R.layout.activity_device_scan);
        ButterKnife.bind(this);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        adapter = new DevicesListAdapter(this, R.layout.device);
        lv_devices.setAdapter(adapter);
        registerForContextMenu(lv_devices);
        Intent scanIntent = null;
        scanIntent = !buildProperties.isMockRequired()
                ? new Intent(DeviceScanActivity.this, DeviceSearchService.class)
                : new Intent(DeviceScanActivity.this, DeviceSearchServiceMock.class);
        startService(scanIntent);
        android.os.Process.getThreadPriority(android.os.Process.myTid());
        IntentFilter deviceStateChangeFilter = new IntentFilter(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        registerReceiver(deviceStateChangeReceiver, deviceStateChangeFilter);

        IntentFilter deviceFoundFilter = new IntentFilter(Constants.ACTION_DEVICE_FOUND);
        registerReceiver(deviceFoundReceiver, deviceFoundFilter);

        updateAdapterReceiver = CommonBroadacastReceivers.getUpdateListViewAdapterReceiver(this, this.adapter);
        IntentFilter updateAdapters = new IntentFilter(Constants.ACTION_UPDATE_ADAPTER);
        registerReceiver(updateAdapterReceiver, updateAdapters);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(deviceFoundReceiver);
        unregisterReceiver(deviceStateChangeReceiver);
        unregisterReceiver(updateAdapterReceiver);
        closeDialogNoInetHandler.removeCallbacks(closeDialogNoInetRunnable);
        stopService(new Intent(this, buildProperties.isMockRequired() ? DeviceSearchServiceMock.class : DeviceSearchService.class));
        connectionWatchStruct.cancelConnectionCheck();
        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listView) {
            MenuInflater inflater = getMenuInflater();
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) menuInfo;
            inflater.inflate(R.menu.menu_device_context, menu);
            String devNumber = ((TextView) info.targetView.findViewById(R.id.textView_devNo)).getText().toString();
            if (!unitOfWork.getDeviceRepository().getAllActiveDevices().contains(devNumber)) {
                menu.findItem(R.id.disconnect).setVisible(false);
            }

            if (unitOfWork.getDeviceRepository().getAllPairedDeviceIds().contains(devNumber)) {
                menu.findItem(R.id.add_fav).setVisible(false);
                menu.findItem(R.id.remove_fav).setVisible(true);
            } else {
                menu.findItem(R.id.add_fav).setVisible(true);
                menu.findItem(R.id.remove_fav).setVisible(false);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final String deviceNumber = (String) ((TextView) info.targetView.findViewById(R.id.textView_devNo)).getText();
        final String deviceName = (String) ((TextView) info.targetView.findViewById(R.id.textView_devName)).getText();
        final String deviceType = (String) ((TextView) info.targetView.findViewById(R.id.textView_devType)).getText();
        boolean isPaired = ((TextView) info.targetView.findViewById(R.id.textView_deviceStatus)).getText().toString().equals(DeviceStatus.PAIRED.toString());
        String deviceStatus = (String) ((TextView) info.targetView.findViewById(R.id.textView_deviceConnection)).getText();
        DevicesListItem d = new DevicesListItem(deviceName, deviceType, deviceNumber, isPaired, deviceStatus);

        switch (item.getItemId()) {
            case R.id.add_fav:
                if (unitOfWork.getDeviceRepository().insertDevice(d, true)) {
                    settings.setDeviceLastStatus(d.getNumber(),
                            (d.getType().equals(DeviceType.HEARTRATE.toString()) || d.getType().equals(DeviceType.BIKE_POWER.toString())
                                    ? DeviceState.DEAD.getIntValue()
                                    : BSXDeviceConnection.DISCONNECTED.getConnectionId()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cToast.makeText("Device added to favorites", Toast.LENGTH_SHORT);
                            adapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cToast.makeText("Could not add device to favourites", Toast.LENGTH_SHORT);
                        }
                    });
                }
                return true;
            case R.id.remove_fav:
                if (unitOfWork.getDeviceRepository().getAllActiveDevices().contains(deviceNumber)) {
                    cToast.makeText("Collecting data. Please disconnect from device first.", Toast.LENGTH_SHORT);
                    return false;
                } else {
                    if (unitOfWork.getDeviceRepository().unpairDeviceByNumber(deviceNumber)) {
                        ((TextView) info.targetView.findViewById(R.id.textView_deviceStatus)).setText(DeviceStatus.NOT_PAIRED.toString());
                        lv_devices.getFirstVisiblePosition();
                        cToast.makeText("Device removed from favourites", Toast.LENGTH_SHORT);
                    }
                    return true;
                }
            case R.id.rename:
                if (unitOfWork.getDeviceRepository().getAllActiveDevices().contains(deviceNumber)) {
                    cToast.makeText("Collecting data. Please disconnect from device first.", Toast.LENGTH_SHORT);
                    return false;
                } else {
                    renameDialog = new RenameDeviceAlertDialog(this);
                    renameDialog.createDialog(d);
                }
                return true;
            case R.id.disconnect:
                Class<?> serviceClass = null;
                if (deviceType.equals(DeviceType.HEARTRATE.toString())) {
                    serviceClass = !buildProperties.isMockRequired() ? HeartRateMonitorService.class : HeartRateMonitorServiceMock.class;
                } else if (deviceType.equals(DeviceType.BIKE_POWER.toString())) {
                    serviceClass = !buildProperties.isMockRequired() ? BikePowerWattsMonitorService.class : BikePowerWattsMonitorServiceMock.class;
                } else {
                    serviceClass = !buildProperties.isMockRequired() ? BSXInsightsService.class : BSXInsightsServiceMock.class;
                }

                info.targetView.setBackgroundColor(ActivityCompat.getColor(getApplicationContext(), android.R.color.transparent));
                ((TextView) info.targetView.findViewById(R.id.textView_serviceStatus)).setText(ServiceState.DISABLED.toString());

                try {
                    Intent i = new Intent();
                    i.setAction(serviceClass.getSimpleName() + Constants.ACTION_UNREGISTER_DEVICE);
                    i.putExtra(Constants.DEVICE_NUMBER_EXTRA, deviceNumber);
                    sendBroadcast(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                unitOfWork.getDeviceRepository().updateDeviceActiveStatus(deviceNumber, false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void addDeviceToAdapter(DevicesListAdapter adapter, DevicesListItem d) {
        boolean adapterContainsValue = false;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (((DevicesListItem) adapter.getItem(i)).getNumber().equals(d.getNumber())) {
                adapterContainsValue = true;
                break;
            }
        }

        if (!adapterContainsValue) {
            adapter.add(d);
        }
    }

    private void updateDeviceAdapter() {
        (new AsyncTask<DevicesListAdapter, Void, Void>() {
            DevicesListAdapter adapter;

            @Override
            protected Void doInBackground(DevicesListAdapter... adapters) {
                adapter = adapters[0];
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                adapter.notifyDataSetChanged();
            }
        }).execute(adapter);
    }
}
