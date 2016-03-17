package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.CalibrationMessageWrapper;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.ConnectionWatchStruct;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.SharedSettings;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class BpmParametersActivity extends Activity {

    @Inject
    UnitOfWork unitOfWork;
    @Inject
    CToast cToast;
    @Inject
    SharedSettings settings;

    @Bind(R.id.btnSetSlope)
    Button btnSetSlope;

    @Bind(R.id.btnManualCalibration)
    Button btnManualCalibration;

    @Bind(R.id.messagesLayout)
    LinearLayout messagesLayout;

    private final ConnectionWatchStruct connectionWatchStruct = new ConnectionWatchStruct(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        USACyclingApplication.getObjectGraph().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bpm_parameters);
        connectionWatchStruct.setConnectionCheck();
        ButterKnife.bind(this);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        fillLastCalibrationMessagesForPM();
        IntentFilter statusFilter = new IntentFilter(Constants.ACTION_PM_CONF_STATUS);
        registerReceiver(statusBroadcastReceiver, statusFilter);

        IntentFilter calibrationMessageReceiverFilter = new IntentFilter(Constants.ACTION_MANUAL_CALIBRATION_RESULT);
        registerReceiver(calibrationMessageReceiver, calibrationMessageReceiverFilter);

        setBtnClickListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(statusBroadcastReceiver);
        unregisterReceiver(calibrationMessageReceiver);
        connectionWatchStruct.cancelConnectionCheck();
    }

    private void setBtnClickListeners() {
        btnManualCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendManualCalibrationRequest = new Intent(Constants.ACTION_MANUAL_CALIBRATION);
                sendBroadcast(sendManualCalibrationRequest);
            }
        });

        btnSetSlope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final List<DevicesListItem> runningPM = unitOfWork.getDeviceRepository().getAllActivePowerMeters();

                if (runningPM.size() > 0) {
                    LayoutInflater inflater = getLayoutInflater();
                    View layout = inflater.inflate(R.layout.dialog_slope_set, null);
                    LinearLayout btnLayout = (LinearLayout) layout.findViewById(R.id.btnLayout);
                    btnLayout.setVisibility(View.VISIBLE);
                    final ArrayList<LinearLayout> layouts = new ArrayList<LinearLayout>(
                            Arrays.asList(
                                    (LinearLayout) layout.findViewById(R.id.llPowerOne),
                                    (LinearLayout) layout.findViewById(R.id.llPowerTwo),
                                    (LinearLayout) layout.findViewById(R.id.llPowerThree),
                                    (LinearLayout) layout.findViewById(R.id.llPowerFour)
                            ));

                    ArrayList<EditText> tBoxes = new ArrayList<EditText>();

                    for (int index = 0; index < runningPM.size(); index++) {
                        DevicesListItem d = runningPM.get(index);
                        LinearLayout l = layouts.get(index);
                        l.setVisibility(View.VISIBLE);
                        EditText tBox = (EditText) l.findViewWithTag("et");
                        String setSlope = settings.getPowerMeterSlope(runningPM.get(index).getNumber());
                        if (setSlope == null || setSlope.equals("")) {
                            tBox.setHint(getResources().getString(R.string.not_provided_slope_hint));
                        } else {
                            tBox.setText(setSlope);
                        }

                        tBoxes.add((EditText) l.findViewWithTag("et"));
                        ((TextView) l.findViewWithTag("tv")).setText(d.getName());
                    }

                    final AlertDialog.Builder builder =
                            new AlertDialog.Builder(BpmParametersActivity.this)
                                    .setTitle(getString(R.string.set_slopes_dialog_title))
                                    .setCancelable(false).setView(layout);

                    final AlertDialog dialog = builder.create();
                    ((Button) btnLayout.findViewById(R.id.btnSet)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean invalid = false;
                            ArrayList<String> slopes = new ArrayList<String>();
                            for (int i = 0; i < runningPM.size(); i++) {
                                EditText et = ((EditText) layouts.get(i).findViewWithTag("et"));
                                String newSlope = et.getText().toString().trim();
                                String setSlope = settings.getPowerMeterSlope(runningPM.get(i).getNumber());
                                if (newSlope.equals("") || newSlope.equals(setSlope)) {
                                    continue;
                                }

                                try {
                                    BigDecimal bd = new BigDecimal(newSlope);
                                    slopes.add(runningPM.get(i).getNumber() + " _ " + newSlope);
                                    settings.setPowerMeterSlope(runningPM.get(i), newSlope);
                                } catch (Exception e) {
                                    Log.e(BpmParametersActivity.class.getSimpleName(), e.toString());
                                    invalid = true;
                                    break;
                                }
                            }

                            if (!invalid) {
                                dialog.dismiss();
                                Intent sendAutoZeroRequest = new Intent(Constants.ACTION_SET_CTF_SLOPE);
                                sendAutoZeroRequest.putStringArrayListExtra(Constants.PM_SLOPES_EXTRA, slopes);
                                sendBroadcast(sendAutoZeroRequest);
                            } else {
                                cToast.makeText("There are invalid parameters", Toast.LENGTH_SHORT);
                            }
                        }
                    });

                    ((Button) btnLayout.findViewById(R.id.btnCancel)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        }
                    });

                    dialog.show();
                } else {
                    cToast.makeText("No running power meters", Toast.LENGTH_SHORT);
                }
            }
        });
    }

    private BroadcastReceiver statusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String statusMessage = intent.getStringExtra(Constants.STATE_EXTRA);
            if (!statusMessage.equals("")) {
                cToast.makeText(statusMessage, Toast.LENGTH_SHORT);
            }
        }
    };

    private BroadcastReceiver calibrationMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceId = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            AntPlusBikePowerPcc.CalibrationMessage calibrationMessage = intent.getParcelableExtra(Constants.PM_MANUAL_CALIBRATION_EXTRA);
            CalibrationMessageWrapper wrapper = new CalibrationMessageWrapper(calibrationMessage, deviceId);
            View view = messagesLayout.findViewWithTag("tvDevice" + deviceId);
            settings.setLastCalibrationMessageForDevice(deviceId, wrapper.toString());
            if (view != null) {
                ((TextView) view).setText(wrapper.toString());
            } else {

                TextView tv = new TextView(BpmParametersActivity.this);
                tv.setTag("tvDevice" + deviceId);
                tv.setText(wrapper.toString());
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.setMargins(0, 0, 0, 15);
                tv.setLayoutParams(llp);

                messagesLayout.addView(tv);
            }
        }
    };

    private void fillLastCalibrationMessagesForPM() {
        List<DevicesListItem> activePMs = unitOfWork.getDeviceRepository().getAllActivePowerMeters();

        for (DevicesListItem device : activePMs) {
            String deviceId = device.getNumber();
            String msg = settings.getLastCalibrationMessageForDevice(deviceId);
            if (msg != null) {
                TextView tv = new TextView(BpmParametersActivity.this);
                tv.setTag("tvDevice" + deviceId);
                tv.setText(settings.getLastCalibrationMessageForDevice(deviceId));
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.setMargins(0, 0, 0, 15);
                tv.setLayoutParams(llp);

                messagesLayout.addView(tv);
            }
        }
    }
}
