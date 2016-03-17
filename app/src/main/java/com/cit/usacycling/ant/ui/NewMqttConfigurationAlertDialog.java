package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.MqttConfiguration;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.global.CToast;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class NewMqttConfigurationAlertDialog {
    private final String BTN_LABEL_CANCEL = "CANCEL";
    private final String BTN_LABEL_RENAME = "SAVE";

    private Activity context;
    private AlertDialog newConfigurationDialog;

    @Inject
    CToast cToast;
    @Inject
    UnitOfWork unitOfWork;

    EditText orgIdBox;
    EditText userIdBox;
    EditText hostBox;
    EditText portBox;
    EditText deviceIdBox;
    EditText deviceTokenBox;
    EditText dataQoSBox;
    EditText statusQoSBox;

    private DialogInterface.OnClickListener positiveClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String devId = deviceIdBox.getText().toString().trim();
            try {
                if (unitOfWork.getMqttConfigurationRepository().getConfigurationByDeviceId(devId) == null) {
                    unitOfWork.getMqttConfigurationRepository().insertConfiguration(new MqttConfiguration(
                            devId,
                            deviceTokenBox.getText().toString().trim(),
                            orgIdBox.getText().toString().trim(),
                            userIdBox.getText().toString().trim(),
                            hostBox.getText().toString().trim(),
                            portBox.getText().toString().trim(),
                            Integer.parseInt(statusQoSBox.getText().toString()),
                            Integer.parseInt(dataQoSBox.getText().toString()),
                            0
                    ));

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cToast.makeText("New configuration saved", Toast.LENGTH_SHORT);
                        }
                    });
                } else {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cToast.makeText("Configuration with this device id already exists", Toast.LENGTH_SHORT);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cToast.makeText("New configuration could not be saved", Toast.LENGTH_SHORT);
                    }
                });
            }
        }
    };

    private DialogInterface.OnClickListener negativeClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };

    public NewMqttConfigurationAlertDialog(Activity context) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = context;
    }

    public void createDialog() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogLayout = inflater.inflate(R.layout.dialog_new_mqtt_configuration, null);

        orgIdBox = ((EditText) dialogLayout.findViewById(R.id.etOrgId));
        userIdBox = ((EditText) dialogLayout.findViewById(R.id.etUserId));
        hostBox = ((EditText) dialogLayout.findViewById(R.id.etHost));
        portBox = ((EditText) dialogLayout.findViewById(R.id.etPort));
        deviceIdBox = ((EditText) dialogLayout.findViewById(R.id.etDeviceId));
        deviceTokenBox = ((EditText) dialogLayout.findViewById(R.id.etDeviceToken));
        dataQoSBox = ((EditText) dialogLayout.findViewById(R.id.etDataQoS));
        statusQoSBox = ((EditText) dialogLayout.findViewById(R.id.etStatusQoS));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogLayout);
        builder.setCancelable(false);
        builder.setPositiveButton(BTN_LABEL_RENAME, positiveClick);
        builder.setNegativeButton(BTN_LABEL_CANCEL, negativeClick);
        newConfigurationDialog = builder.create();
        newConfigurationDialog.show();
    }
}
