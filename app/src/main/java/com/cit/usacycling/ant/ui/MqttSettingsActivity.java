package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.MqttConfiguration;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.SharedSettings;

import java.util.HashSet;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MqttSettingsActivity extends Activity {

    @Bind(R.id.newConfigButton)
    Button newConfigButton;

    @Bind(R.id.rgOptions)
    RadioGroup optionsRadioGroup;

    @Inject
    UnitOfWork unitOfWork;

    @Inject
    SharedSettings settings;

    @Inject
    BuildProperties buildProperties;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        USACyclingApplication.getObjectGraph().inject(this);

        setContentView(R.layout.activity_mqtt_settings);
        ButterKnife.bind(this);
        setNewConfigButtonClickListener();

        HashSet<MqttConfiguration> allConfigurations = unitOfWork.getMqttConfigurationRepository().getAllConfigurations();

        for (MqttConfiguration conf : allConfigurations) {
            RadioButton rb = new RadioButton(MqttSettingsActivity.this);
            rb.setTag(conf.getDeviceId());
            rb.setText(conf.getDeviceId());
            rb.setChecked(conf.getIsChecked() == 1);
            optionsRadioGroup.addView(rb);
        }

        optionsRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String selectedDeviceId = group.findViewById(checkedId).getTag().toString();
                MqttConfiguration updateConf = unitOfWork.getMqttConfigurationRepository().getConfigurationByDeviceId(selectedDeviceId);

                buildProperties.setDataMessageQoS(updateConf.getDataMessageQoS());
                buildProperties.setStatusMessageQoS(updateConf.getStatusMessageQoS());
                buildProperties.setUserIdValue(updateConf.getUserIdValue());
                buildProperties.setSelectedDeviceId(updateConf.getDeviceId());
                buildProperties.setSelectedDevicePass(updateConf.getDeviceToken());
                buildProperties.setPortValue(updateConf.getPortValue());
                buildProperties.setOrganizationId(updateConf.getOrganizationId());
                buildProperties.setHostValue(buildProperties.getOrganizationId() + updateConf.getHostPostfix());

                updateConf.setIsChecked(1);
                unitOfWork.getMqttConfigurationRepository().updateConfiguration(updateConf);
            }
        });
    }


    private void setNewConfigButtonClickListener() {
        newConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewMqttConfigurationAlertDialog addConfigDialog = new NewMqttConfigurationAlertDialog(MqttSettingsActivity.this);
                addConfigDialog.createDialog();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_iot_settings, menu);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int selectedId = item.getItemId();

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onDestroy() {
        if (settings.getIoTCredentials() == null) {
            settings.setIotSettingsReconnectRequired(true);
        }
        super.onDestroy();
    }
}
