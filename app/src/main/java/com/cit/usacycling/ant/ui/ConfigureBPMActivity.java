package com.cit.usacycling.ant.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.background.services.BikePowerWattsMonitorService;
import com.cit.usacycling.ant.background.services.HeartRateMonitorService;
import com.cit.usacycling.ant.global.ConnectionWatchStruct;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.SharedSettings;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ConfigureBPMActivity extends Activity {

    @Inject
    UnitOfWork unitOfWork;

    @Inject
    SharedSettings settings;

    @Bind(R.id.llBPM1)
    LinearLayout pm1;

    @Bind(R.id.llBPM2)
    LinearLayout pm2;

    @Bind(R.id.llBPM3)
    LinearLayout pm3;

    @Bind(R.id.llBPM4)
    LinearLayout pm4;

    @Bind(R.id.tvPowerMeter1)
    TextView powerMeterOneLabel;
    @Bind(R.id.ibPM1Up)
    ImageButton powerMeterOneUp;
    @Bind(R.id.ibPM1Down)
    ImageButton powerMeterOneDown;
    @Bind(R.id.tvBPM1PowerValue)
    TextView powerMeterOneValue;

    @Bind(R.id.tvPowerMeter2)
    TextView powerMeterTwoLabel;
    @Bind(R.id.ibPM2Up)
    ImageButton powerMeterTwoUp;
    @Bind(R.id.ibPM2Down)
    ImageButton powerMeterTwoDown;
    @Bind(R.id.tvBPM2PowerValue)
    TextView powerMeterTwoValue;

    @Bind(R.id.tvPowerMeter3)
    TextView powerMeterThreeLabel;
    @Bind(R.id.ibPM3Up)
    ImageButton powerMeterThreeUp;
    @Bind(R.id.ibPM3Down)
    ImageButton powerMeterThreeDown;
    @Bind(R.id.tvBPM3PowerValue)
    TextView powerMeterThreeValue;

    @Bind(R.id.tvPowerMeter4)
    TextView powerMeterFourLabel;
    @Bind(R.id.ibPM4Up)
    ImageButton powerMeterFourUp;
    @Bind(R.id.ibPM4Down)
    ImageButton powerMeterFourDown;
    @Bind(R.id.tvBPM4PowerValue)
    TextView powerMeterFourValue;

    @Bind(R.id.ibC1Down)
    ImageButton cadenceOneDown;
    @Bind(R.id.ibC1Up)
    ImageButton cadenceOneUp;
    @Bind(R.id.tvC1CadenceValue)
    TextView cadenceOneValue;

    @Bind(R.id.ibC2Down)
    ImageButton cadenceTwoDown;
    @Bind(R.id.ibC2Up)
    ImageButton cadenceTwoUp;
    @Bind(R.id.tvC2CadenceValue)
    TextView cadenceTwoValue;

    @Bind(R.id.ibC3Down)
    ImageButton cadenceThreeDown;
    @Bind(R.id.ibC3Up)
    ImageButton cadenceThreeUp;
    @Bind(R.id.tvC3CadenceValue)
    TextView cadenceThreeValue;

    @Bind(R.id.ibC4Down)
    ImageButton cadenceFourDown;
    @Bind(R.id.ibC4Up)
    ImageButton cadenceFourUp;
    @Bind(R.id.tvC4CadenceValue)
    TextView cadenceFourValue;

    @Bind(R.id.tvConsole)
    TextView console;

    @Bind(R.id.consoleScrollView)
    ScrollView consoleScroll;

    private final ConnectionWatchStruct connectionWatchStruct = new ConnectionWatchStruct(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        USACyclingApplication.getObjectGraph().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_bpm);
        connectionWatchStruct.setConnectionCheck();
        ButterKnife.bind(this);

        ActionBar aBar = getActionBar();
        if (aBar != null) {
            aBar.setDisplayHomeAsUpEnabled(true);
        }
        registerReceivers();
        List<LinearLayout> powerButtonLayouts = Arrays.asList(pm1, pm2, pm3, pm4);
        List<DevicesListItem> activePMs = unitOfWork.getDeviceRepository().getAllActivePowerMeters();

        for (int i = 0; i < activePMs.size(); i++) {
            LinearLayout layout = powerButtonLayouts.get(i);
            final DevicesListItem item = activePMs.get(i);
            String label = item.getName() + " " + item.getNumber();
            String powerValue = String.valueOf(settings.getPMPowerValue(item.getNumber()));
            String cadenceValue = String.valueOf(settings.getCadenceValue(item.getNumber()));

            layout.setVisibility(View.VISIBLE);

            if (i == 0) {
                powerMeterOneLabel.setText(label);
                powerMeterOneValue.setText(powerValue + " W");
                cadenceOneValue.setText(cadenceValue + " RPM");
                powerMeterOneUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increasePMPowerValue(item.getNumber());
                        powerMeterOneValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterOneUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 380);
                        powerMeterOneValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                powerMeterOneDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreasePMPowerValue(item.getNumber());
                        powerMeterOneValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterOneDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 0);
                        powerMeterOneValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                cadenceOneUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increaseCadenceValue(item.getNumber());
                        cadenceOneValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceOneUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 350);
                        cadenceOneValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
                cadenceOneDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreaseCadenceValue(item.getNumber());
                        cadenceOneValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceOneDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 100);
                        cadenceOneValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
            } else if (i == 1) {
                powerMeterTwoLabel.setText(label);
                powerMeterTwoValue.setText(powerValue + " W");
                cadenceTwoValue.setText(cadenceValue + " RPM");
                powerMeterTwoUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increasePMPowerValue(item.getNumber());
                        powerMeterTwoValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterTwoUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 380);
                        powerMeterTwoValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                powerMeterTwoDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreasePMPowerValue(item.getNumber());
                        powerMeterTwoValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterTwoDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 0);
                        powerMeterTwoValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                cadenceTwoUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increaseCadenceValue(item.getNumber());
                        cadenceTwoValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceTwoUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 350);
                        cadenceTwoValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
                cadenceTwoDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreaseCadenceValue(item.getNumber());
                        cadenceTwoValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceTwoDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 100);
                        cadenceTwoValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
            } else if (i == 2) {
                powerMeterThreeLabel.setText(label);
                powerMeterThreeValue.setText(powerValue + " W");
                cadenceThreeValue.setText(cadenceValue + " RPM");
                powerMeterThreeUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increasePMPowerValue(item.getNumber());
                        powerMeterThreeValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterThreeUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 380);
                        powerMeterThreeValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                powerMeterThreeDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreasePMPowerValue(item.getNumber());
                        powerMeterThreeValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterThreeDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 0);
                        powerMeterThreeValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                cadenceThreeUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increaseCadenceValue(item.getNumber());
                        cadenceThreeValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceThreeUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 350);
                        cadenceThreeValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
                cadenceThreeDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreaseCadenceValue(item.getNumber());
                        cadenceThreeValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceThreeDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 100);
                        cadenceThreeValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
            } else if (i == 3) {
                powerMeterFourLabel.setText(label);
                powerMeterFourValue.setText(powerValue + " W");
                cadenceFourValue.setText(cadenceValue + " RPM");
                powerMeterFourUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increasePMPowerValue(item.getNumber());
                        powerMeterFourValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterFourUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 380);
                        powerMeterFourValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                powerMeterFourDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreasePMPowerValue(item.getNumber());
                        powerMeterFourValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                    }
                });
                powerMeterFourDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setPMPowerValue(item.getNumber(), 0);
                        powerMeterFourValue.setText(String.valueOf(settings.getPMPowerValue(item.getNumber())) + " W");
                        return true;
                    }
                });

                cadenceFourUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.increaseCadenceValue(item.getNumber());
                        cadenceFourValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceFourUp.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 350);
                        cadenceFourValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
                cadenceFourDown.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settings.decreaseCadenceValue(item.getNumber());
                        cadenceFourValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                    }
                });
                cadenceFourDown.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        settings.setCadenceValue(item.getNumber(), 100);
                        cadenceFourValue.setText(String.valueOf(settings.getCadenceValue(item.getNumber())) + " RPM");
                        return true;
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(antDeviceStatusReceiver);
        unregisterReceiver(deviceStateChangeReceiver);
        connectionWatchStruct.cancelConnectionCheck();
        super.onDestroy();
    }

    private void registerReceivers() {
        IntentFilter deviceStateChangeFilter = new IntentFilter(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        registerReceiver(deviceStateChangeReceiver, deviceStateChangeFilter);

        IntentFilter antDeviceIntentFilter = new IntentFilter(Constants.ACTION_ANT_DEVICE_STATUS);
        registerReceiver(antDeviceStatusReceiver, antDeviceIntentFilter);

        IntentFilter commandArrivedReceiver = new IntentFilter(Constants.ACTION_MATCH_BURNED_CMD);
        registerReceiver(matchBurnedCommandReceiver, commandArrivedReceiver);
    }

    private final BroadcastReceiver deviceStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String number = intent.getStringExtra(Constants.NUMBER_EXTRA);
            final int state = intent.getIntExtra(Constants.STATE_EXTRA, 0);
            settings.setDeviceLastStatus(number, state);
        }
    };

    @SuppressLint("SimpleDateFormat")
    private final BroadcastReceiver matchBurnedCommandReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, final Intent intent) {
            String consoleText = console.getText().toString();
            String msg = intent.getStringExtra(Constants.MATCH_BURNED_EXTRA);
            try {
                JSONObject obj = new JSONObject(msg);
                String newText = obj.get("deviceId") + " burned a match. Total: " + obj.get("count");

                DateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss]");
                Date dateobj = new Date();
                String displayDate = dateFormat.format(dateobj);
                console.setText(consoleText + System.getProperty("line.separator") + displayDate + " " + newText);
                console.setTextColor(getResources().getColor(android.R.color.white));

                consoleScroll.post(new Runnable() {
                    public void run() {
                        consoleScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

    private void stopService(Intent intent, String serviceTag) {
        stopService(intent);
        settings.stopService(serviceTag);
    }
}
