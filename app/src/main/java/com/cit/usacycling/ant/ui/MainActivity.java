package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.DbProvider;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.background.mqtt.MQTTService;
import com.cit.usacycling.ant.background.services.BSXInsightsService;
import com.cit.usacycling.ant.background.services.BikePowerWattsMonitorService;
import com.cit.usacycling.ant.background.services.HeartRateMonitorService;
import com.cit.usacycling.ant.enums.BSXDeviceConnection;
import com.cit.usacycling.ant.enums.DeviceStatus;
import com.cit.usacycling.ant.enums.ServiceState;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.DataPushManager;
import com.cit.usacycling.ant.global.SharedSettings;
import com.cit.usacycling.ant.global.StatusPushManager;
import com.cit.usacycling.ant.global.receivers.CommonBroadacastReceivers;
import com.cit.usacycling.ant.mock.services.BSXInsightsServiceMock;
import com.cit.usacycling.ant.mock.services.BikePowerWattsMonitorServiceMock;
import com.cit.usacycling.ant.mock.services.HeartRateMonitorServiceMock;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Handler connectionHandler = new Handler();
    private final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private RenameDeviceAlertDialog renameDialog;

    private final Runnable connectionRunnable = new Runnable() {
        @Override
        public void run() {
            startMQTTService();
        }
    };

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (progress == null || !progress.isShowing()) {
                Resources resources = getResources();
                String connectingToIot = resources.getString(R.string.connecting_to_iot_msg);
                String pleaseWait = resources.getString(R.string.wait_msg);
                String exitLabel = resources.getString(R.string.exit_label);
                setProgressDialogOptions(connectingToIot, pleaseWait, true);
                progress.setButton(DialogInterface.BUTTON_NEGATIVE, exitLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                progress.show();
            }
        }
    };

    private final BroadcastReceiver iotStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra(Constants.MESSAGE_DATA);

            if (data.equals(Constants.CONNECTED)) {
                tvIotStatus.setTextColor(colorGreen);

                progress.dismiss();
                dialogHandler.removeCallbacks(progressRunnable);
            } else if (data.equals(Constants.NOT_CONNECTED)) {
                tvIotStatus.setTextColor(colorRed);
                dialogHandler.post(progressRunnable);
            }

            tvIotStatus.setText(data);
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
                                if (devType.equals(DeviceType.BIKE_POWER.toString()) || devType.equals(DeviceType.HEARTRATE.toString())) {
                                    ((TextView) v.findViewById(R.id.textView_deviceConnection))
                                            .setText(DeviceState.getValueFromInt(state).toString());
                                } else {
                                    String strState = BSXDeviceConnection.getStateById(state);
                                    DevicesListItem listItem = unitOfWork.getDeviceRepository().getDeviceByNumber(number);
                                    Log.d("STATE", strState);
                                    if (unitOfWork.getDeviceRepository().getAllActiveDevices().contains(number)) {
                                        unitOfWork.getDeviceRepository().updateDeviceStatus(number, strState);
                                        listItem.setStatus(strState);
                                    } else {
                                        Log.d("MAIN", "BSX CONNECTED");
                                        unitOfWork.getDeviceRepository().updateDeviceStatus(number, strState);
                                        listItem.setStatus(strState);
                                    }

                                    adapter.update(listItem);
                                    settings.setDeviceLastStatus(number, state);
                                    ((TextView) v.findViewById(R.id.textView_deviceConnection))
                                            .setText(strState);
                                }

                                lv_devices.getFirstVisiblePosition();
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    activityMenu.findItem(R.id.action_scan).setEnabled(true);
                }
            });
        }
    };

    private final BroadcastReceiver antDeviceStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String serviceTag = intent.getStringExtra(Constants.SERVICE_TAG);
                    Class<?> targetClass = null;
                    String deviceType = null;
                    if (serviceTag.equals(HeartRateMonitorService.SERVICE_TAG)) {
                        targetClass = HeartRateMonitorService.class;
                        deviceType = DeviceType.HEARTRATE.toString();
                    } else if (serviceTag.equals(BikePowerWattsMonitorService.SERVICE_TAG)) {
                        targetClass = BikePowerWattsMonitorService.class;
                        deviceType = DeviceType.BIKE_POWER.toString();
                    }
                    String serviceStarter = settings.getItemStartedService(serviceTag);
                    Intent serviceIntent = new Intent(getApplicationContext(), targetClass);

                    stopService(serviceIntent, serviceTag);
                    serviceIntent.putExtra(Constants.DEVICE_NUMBER_EXTRA, serviceStarter);
                    serviceIntent.putExtra(Constants.DEVICE_TYPE_EXTRA, deviceType);
                    startService(serviceIntent);
                }
            });
        }
    };

    private final BroadcastReceiver reconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (renameDialog != null && (renameDialog.getDialog() != null)) {
                renameDialog.getDialog().dismiss();
            }

            tvIotStatus.setTextColor(colorRed);
            tvIotStatus.setText(Constants.NOT_CONNECTED);
            connectToIOTAsync();
        }
    };

    @Bind(R.id.listView)
    ListView lv_devices;
    @Bind(R.id.iotConnectionStatus)
    TextView tvIotStatus;
    @BindColor(R.color.device_list_red)
    int colorRed;
    @BindColor(R.color.device_list_green)
    int colorGreen;
    @BindColor(R.color.device_list_orange)
    int colorOrange;

    @Inject
    DbProvider db;
    @Inject
    UnitOfWork unitOfWork;
    @Inject
    BuildProperties buildProperties;
    @Inject
    SharedSettings settings;
    @Inject
    CToast cToast;

    private ProgressDialog progress;
    private BroadcastReceiver updateAdapterReceiver;
    private Menu activityMenu;
    private DevicesListAdapter adapter;
    private Handler dialogHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        USACyclingApplication.getObjectGraph().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        dialogHandler = new Handler();
        updateActionBarTitle();
        adapter = new DevicesListAdapter(this, R.layout.device);
        tvIotStatus.setText(Constants.NOT_CONNECTED);
        registerForContextMenu(lv_devices);
        settings.setIotSettingsReconnectRequired(false);

        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                unitOfWork.getMessageRepository().clearAllUnsentMessages();
                return null;
            }
        }).execute();

        registerReceivers();
        startServicesLocally();

        startRunningDevicesAfterForceClose(1000);
        if (settings.getForceClosedState()) {
            cToast.makeText("App recovered from unhandled exception. Server logging will be available soon.", Toast.LENGTH_SHORT);
            settings.setForceClosedState(false);
        }
    }

    private void startRunningDevicesAfterForceClose(long restartInterval) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                HashSet<DevicesListItem> activeDevices = unitOfWork.getDeviceRepository().getAllActiveDevicesDetailed();
                for (DevicesListItem item : activeDevices) {
                    String serviceTag;
                    if (item.getType().equals(DeviceType.BIKE_POWER.toString())) {
                        serviceTag = buildProperties.isMockRequired() ? BikePowerWattsMonitorServiceMock.SERVICE_TAG : BikePowerWattsMonitorService.SERVICE_TAG;
                    } else if (item.getType().equals(DeviceType.HEARTRATE.toString())) {
                        serviceTag = buildProperties.isMockRequired() ? HeartRateMonitorServiceMock.SERVICE_TAG : HeartRateMonitorService.SERVICE_TAG;
                    } else {
                        serviceTag = buildProperties.isMockRequired() ? BSXInsightsServiceMock.SERVICE_TAG : BSXInsightsService.SERVICE_TAG;
                    }

                    Intent addDeviceIntent = new Intent(serviceTag + Constants.ACTION_REGISTER_DEVICE);
                    addDeviceIntent.putExtra(Constants.DEVICE_NUMBER_EXTRA, item.getNumber());
                    sendBroadcast(addDeviceIntent);
                }
            }
        }, restartInterval);
    }

    private void updateActionBarTitle() {
        String[] credentials = settings.getIoTCredentials();
        String devId = buildProperties.getSelectedDeviceId();
        if (getActionBar() != null) {
            getActionBar().setTitle(buildProperties.getApplicationName() + "/id:" + devId);
        }
    }

    private void startServicesLocally() {
    /*
        Start MQTT Service to keep it connected.
        So when data is broadcasted by any other service, MQTT Serivce will publish it to IOTF
     */
        stopService(new Intent(this, MQTTService.class), MQTTService.SERVICE_TAG);
        connectToIOTAsync();
        startService(new Intent(this, DataPushManager.class), DataPushManager.SERVICE_TAG);
        startService(new Intent(this, StatusPushManager.class), StatusPushManager.SERVICE_TAG);

        if (!buildProperties.isMockRequired()) {
            startService(new Intent(this, HeartRateMonitorService.class), HeartRateMonitorService.SERVICE_TAG);
            startService(new Intent(this, BikePowerWattsMonitorService.class), BikePowerWattsMonitorService.SERVICE_TAG);
            startService(new Intent(this, BSXInsightsService.class), BSXInsightsService.SERVICE_TAG);
        } else {
            startService(new Intent(this, HeartRateMonitorServiceMock.class), HeartRateMonitorServiceMock.SERVICE_TAG);
            startService(new Intent(this, BikePowerWattsMonitorServiceMock.class), BikePowerWattsMonitorServiceMock.SERVICE_TAG);
            startService(new Intent(this, BSXInsightsServiceMock.class), BSXInsightsServiceMock.SERVICE_TAG);
        }
    }

    private void registerReceivers() {
        IntentFilter deviceStateChangeFilter = new IntentFilter(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        registerReceiver(deviceStateChangeReceiver, deviceStateChangeFilter);

        IntentFilter antDeviceIntentFilter = new IntentFilter(Constants.ACTION_ANT_DEVICE_STATUS);
        registerReceiver(antDeviceStatusReceiver, antDeviceIntentFilter);

        IntentFilter intentF = new IntentFilter(Constants.ACTION_CHANGE_IOT_STATUS);
        registerReceiver(iotStatusReceiver, intentF);

        IntentFilter reconnectReceiverFilter = new IntentFilter(Constants.ACTION_RECONNECT);
        registerReceiver(reconnectReceiver, reconnectReceiverFilter);

        updateAdapterReceiver = CommonBroadacastReceivers.getUpdateListViewAdapterReceiver(this, this.adapter);
        IntentFilter updateAdapters = new IntentFilter(Constants.ACTION_UPDATE_ADAPTER);
        registerReceiver(updateAdapterReceiver, updateAdapters);
    }

    private void setProgressDialogOptions(String title, String message, boolean cancelable) {
        progress = new ProgressDialog(this);
        progress.setTitle(title);
        progress.setMessage(message);
        progress.setCancelable(cancelable);
    }

    private void connectToIOTAsync() {

        dialogHandler.post(progressRunnable);
        connectionHandler.removeCallbacks(connectionRunnable);
        connectionHandler.post(connectionRunnable);
    }

    @Override
    protected void onResume() {
//        if (settings.iotSettingsReconnectRequired()) {
//            settings.setIotSettingsReconnectRequired(false);
//            stopServicesLocally();
//            startServicesLocally();
//        }
//
//        if (buildProperties.areCredentialsChanged()) {
//            buildProperties.credentialsChangeAqquired();
//            stopServicesLocally();
//            startServicesLocally();
//        }

        if (adapter != null) {
            adapter.set(unitOfWork.getDeviceRepository().getAllPairedDevices());
            lv_devices.setAdapter(adapter);
        }

//        updateActionBarTitle();
        super.onResume();
    }

    /*
        Stops all the services that are started.
        Do not stop the services in onStop or onPause as we want these services to be running in background.
    */
    @Override
    protected void onDestroy() {
        stopServicesLocally();
        unregisterReceivers();
        cToast.cancel();
        unitOfWork.getDeviceRepository().deactivateAllDevices();
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int selectedId = item.getItemId();
        if (selectedId == R.id.action_scan) {
            Intent scanIntent = new Intent(getApplicationContext(), DeviceScanActivity.class);
            getCurrentMainActivity().startActivity(scanIntent);
        } else if (selectedId == R.id.action_statistics) {
            Intent statisticsIntent = new Intent(getApplicationContext(), StatisticsActivity.class);
            getCurrentMainActivity().startActivity(statisticsIntent);
        } else if (selectedId == R.id.action_configure_bpm) {
            Intent setBPMPowerIntent = new Intent(getApplicationContext(), ConfigureBPMActivity.class);
            getCurrentMainActivity().startActivity(setBPMPowerIntent);
        } else if (selectedId == R.id.action_configure_iot) {
            if (unitOfWork.getDeviceRepository().getAllActiveDevices().size() == 0) {
                Intent configureIoTIntent = new Intent(getApplicationContext(), MqttSettingsActivity.class);
                getCurrentMainActivity().startActivity(configureIoTIntent);
            } else {
                cToast.makeText("Collecting data. Please disconnect from device first.", Toast.LENGTH_SHORT);
            }
        } else if (selectedId == R.id.action_configure_power_meters) {
            Intent configureIoTIntent = new Intent(getApplicationContext(), BpmParametersActivity.class);
            getCurrentMainActivity().startActivity(configureIoTIntent);
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        activityMenu = menu;

        menu.findItem(R.id.action_configure_iot).setVisible(false);
        if (!buildProperties.isMockRequired()) {
            menu.findItem(R.id.action_configure_bpm).setVisible(false);
        }

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_device_context_favs, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String deviceNumber = (String) ((TextView) info.targetView.findViewById(R.id.textView_devNo)).getText();
        String deviceName = (String) ((TextView) info.targetView.findViewById(R.id.textView_devName)).getText();
        String deviceType = (String) ((TextView) info.targetView.findViewById(R.id.textView_devType)).getText();
        boolean isPaired = ((TextView) info.targetView.findViewById(R.id.textView_deviceStatus)).getText().toString().equals(DeviceStatus.PAIRED.toString());
        String deviceStatus = (String) ((TextView) info.targetView.findViewById(R.id.textView_deviceConnection)).getText();
        DevicesListItem d = new DevicesListItem(deviceName, deviceType, deviceNumber, isPaired, deviceStatus);

        switch (item.getItemId()) {
            case R.id.remove_fav:
                if (unitOfWork.getDeviceRepository().getAllActiveDevices().contains(deviceNumber)) {
                    cToast.makeText("Collecting data. Please disconnect from device first.", Toast.LENGTH_SHORT);
                    return false;
                } else {
                    if (unitOfWork.getDeviceRepository().unpairDeviceByNumber(deviceNumber)) {
                        cToast.makeText("Device removed from favorites", Toast.LENGTH_SHORT);
                        adapter.set(unitOfWork.getDeviceRepository().getAllPairedDevices());
                        lv_devices.setAdapter(adapter);
                    }
                    return true;
                }
            case R.id.rename:
                if (unitOfWork.getDeviceRepository().getAllActiveDevices().contains(deviceNumber)) {
                    cToast.makeText("Collecting data. Please disconnect from device first.", Toast.LENGTH_SHORT);
                    return false;
                } else {
                    renameDialog = new RenameDeviceAlertDialog(MainActivity.this);
                    renameDialog.createDialog(d);
                    return true;
                }
            default:
                return super.onContextItemSelected(item);
        }
    }

    private Activity getCurrentMainActivity() {
        return this;
    }

    private void stopServicesLocally() {
        Intent intentMQTT = new Intent(MainActivity.this, MQTTService.class);
        stopService(intentMQTT, MQTTService.class.getSimpleName());

        Intent pushDataIntent = new Intent(MainActivity.this, DataPushManager.class);
        stopService(pushDataIntent, DataPushManager.SERVICE_TAG);
        Intent statusPushDataIntent = new Intent(MainActivity.this, StatusPushManager.class);
        stopService(statusPushDataIntent, StatusPushManager.SERVICE_TAG);

        if (buildProperties.isMockRequired()) {
            Intent intent = new Intent(MainActivity.this, HeartRateMonitorServiceMock.class);
            stopService(intent, HeartRateMonitorServiceMock.SERVICE_TAG);
            Intent bikeIntent = new Intent(MainActivity.this, BikePowerWattsMonitorServiceMock.class);
            stopService(bikeIntent, BikePowerWattsMonitorServiceMock.SERVICE_TAG);
            Intent bsxService = new Intent(MainActivity.this, BSXInsightsServiceMock.class);
            stopService(bsxService, BSXInsightsServiceMock.SERVICE_TAG);
        } else {
            Intent intent = new Intent(MainActivity.this, HeartRateMonitorService.class);
            stopService(intent, HeartRateMonitorService.SERVICE_TAG);
            Intent bikeIntent = new Intent(MainActivity.this, BikePowerWattsMonitorService.class);
            stopService(bikeIntent, BikePowerWattsMonitorService.SERVICE_TAG);
            Intent bsxService = new Intent(MainActivity.this, BSXInsightsService.class);
            stopService(bsxService, BSXInsightsService.SERVICE_TAG);
        }
    }

    private void unregisterReceivers() {
        unregisterReceiver(deviceStateChangeReceiver);
        unregisterReceiver(antDeviceStatusReceiver);
        unregisterReceiver(iotStatusReceiver);
        unregisterReceiver(reconnectReceiver);
        unregisterReceiver(updateAdapterReceiver);
    }

    private void stopService(Intent intent, String serviceTag) {
        stopService(intent);
        settings.stopService(serviceTag);
    }

    private void startService(Intent intent, String serviceTag) {
        startService(intent);
        settings.startService(serviceTag);
    }

    private void startMQTTService() {
        final Intent intentMQTT = new Intent(MainActivity.this, MQTTService.class);
        Log.d(TAG, "Starting MQTTService");

        startService(intentMQTT, MQTTService.SERVICE_TAG);
    }

    @OnItemClick(R.id.listView)
    public void onDeviceListItemClick(AdapterView<?> parent, View view, int position, long id) {
        DevicesListItem selected = DevicesListAdapter.devicesListItems.get(position);
        if (selected.getType().equals(DeviceType.HEARTRATE.toString())) {
            if (!unitOfWork.getDeviceRepository().getAllActiveDevices().contains(selected.getNumber())) {
                toggleServiceFromListItemOn(!buildProperties.isMockRequired() ? HeartRateMonitorService.class : HeartRateMonitorServiceMock.class, selected, view);
            } else {
                toggleServiceFromListItemOff(!buildProperties.isMockRequired() ? HeartRateMonitorService.class : HeartRateMonitorServiceMock.class, selected, view);
            }
        } else if (selected.getType().equals(DeviceType.BIKE_POWER.toString())) {
            if (!unitOfWork.getDeviceRepository().getAllActiveDevices().contains(selected.getNumber())) {
                toggleServiceFromListItemOn(!buildProperties.isMockRequired() ? BikePowerWattsMonitorService.class : BikePowerWattsMonitorServiceMock.class, selected, view);
            } else {
                toggleServiceFromListItemOff(!buildProperties.isMockRequired() ? BikePowerWattsMonitorService.class : BikePowerWattsMonitorServiceMock.class, selected, view);
            }
        } else {
            if (!unitOfWork.getDeviceRepository().getAllActiveDevices().contains(selected.getNumber())) {
                if (!btAdapter.isEnabled()) {
                    cToast.makeText("Enable bluetooth adapter before starting BSX device", Toast.LENGTH_SHORT);
                } else {
                    toggleServiceFromListItemOn(!buildProperties.isMockRequired() ? BSXInsightsService.class : BSXInsightsServiceMock.class, selected, view);
                }
            } else {
                toggleServiceFromListItemOff(!buildProperties.isMockRequired() ? BSXInsightsService.class : BSXInsightsServiceMock.class, selected, view);
            }
        }
    }

    private void toggleServiceFromListItemOn(Class<?> serviceClass, DevicesListItem devicesListItem, View listItemView) {
        Intent i = new Intent();
        i.setAction(serviceClass.getSimpleName() + Constants.ACTION_REGISTER_DEVICE);
        i.putExtra(Constants.DEVICE_NUMBER_EXTRA, devicesListItem.getNumber());
        sendBroadcast(i);

        if (serviceClass.getSimpleName().equals("BSXInsightsService")) {
            setViewWidgetBackgroundColor(listItemView, colorOrange);
            unitOfWork.getDeviceRepository().updateDeviceStatus(devicesListItem.getNumber(), "CONNECTING");
            devicesListItem.setStatus("CONNECTING");
        } else {
            setViewWidgetBackgroundColor(listItemView, colorGreen);
        }
        setTextToTextView(R.id.textView_serviceStatus, listItemView, ServiceState.ENABLED.toString());
        unitOfWork.getDeviceRepository().updateDeviceActiveStatus(devicesListItem.getNumber(), true);
        activityMenu.findItem(R.id.action_scan).setEnabled(false);
        adapter.notifyDataSetChanged();
    }

    private void setTextToTextView(int tvId, View view, String text) {
        TextView tv = (TextView) view.findViewById(tvId);
        tv.setText(text);
    }

    private void setViewWidgetBackgroundColor(View view, int color) {
        view.setBackgroundColor(color);
    }

    private void toggleServiceFromListItemOff(Class<?> serviceClass, DevicesListItem devicesListItem, View listItemView) {
        setViewWidgetBackgroundColor(listItemView, android.R.color.transparent);
        setTextToTextView(R.id.textView_serviceStatus, listItemView, ServiceState.DISABLED.toString());

        try {
            Intent i = new Intent();
            i.setAction(serviceClass.getSimpleName() + Constants.ACTION_UNREGISTER_DEVICE);
            i.putExtra(Constants.DEVICE_NUMBER_EXTRA, devicesListItem.getNumber());
            sendBroadcast(i);
            unitOfWork.getDeviceRepository().updateDeviceActiveStatus(devicesListItem.getNumber(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
