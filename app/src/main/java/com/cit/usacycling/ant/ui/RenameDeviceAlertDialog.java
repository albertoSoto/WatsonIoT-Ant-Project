package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.Constants;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 09.11.2015
 */
public class RenameDeviceAlertDialog {

    private final String BTN_LABEL_CANCEL = "CANCEL";
    private final String BTN_LABEL_RENAME = "RENAME";
    private final String RENAME_FAIL = "Renaming failed. Use letters, digits, spaces and unique name";
    private final String RENAME_FAIL_SERVER = "Server could not process request";
    private final String RENAME_SUCCESS = "Device renamed successfully";

    private DevicesListItem device;
    private Activity context;

    private AlertDialog renameDialog;

    private EditText editText;
    private String newName;

    @Inject
    CToast cToast;
    @Inject
    UnitOfWork unitOfWork;

    private DialogInterface.OnClickListener renamePositive = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            newName = editText.getText().toString().trim();
            if (device.getName().equals(newName)) {
                return;
            }

            device.setName(newName);

            if (!unitOfWork.getDeviceRepository().updateDeviceNameByNumber(device, newName)) {
                cToast.makeText(RENAME_FAIL, Toast.LENGTH_SHORT);
            } else {
                Intent i = new Intent(Constants.ACTION_UPDATE_ADAPTER);
                context.sendBroadcast(i);
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cToast.makeText(RENAME_SUCCESS, Toast.LENGTH_SHORT);
                    }
                });
            }
        }
    };

    private DialogInterface.OnClickListener renameNegative = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };

    public RenameDeviceAlertDialog(Activity context) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = context;
    }

    public void createDialog(final DevicesListItem providedDevice) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                device = providedDevice;
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View dialogLayout = inflater.inflate(R.layout.rename_dialog, null);

                editText = (EditText) dialogLayout.findViewById(R.id.edit_newName);
                editText.setText(unitOfWork.getDeviceRepository().getDeviceNameByNumber(device.getNumber()));
                editText.setSelectAllOnFocus(true);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(dialogLayout);
                builder.setCancelable(false);
                builder.setPositiveButton(BTN_LABEL_RENAME, renamePositive);
                builder.setNegativeButton(BTN_LABEL_CANCEL, renameNegative);
                renameDialog = builder.create();
                renameDialog.show();
            }
        });
    }

    public AlertDialog getDialog() {
        return renameDialog;
    }
}
