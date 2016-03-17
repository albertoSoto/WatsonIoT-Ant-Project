package com.cit.usacycling.ant.dagger;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.cit.usacycling.ant.USACyclingExceptionHandler;
import com.cit.usacycling.ant.background.db.DbProvider;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.background.db.UnitOfWorkInterface;
import com.cit.usacycling.ant.background.devices.BSXInsightDevice;
import com.cit.usacycling.ant.background.devices.BikePowerDevice;
import com.cit.usacycling.ant.background.devices.HeartRateDevice;
import com.cit.usacycling.ant.background.mqtt.MQTTCallbackHandler;
import com.cit.usacycling.ant.background.mqtt.MQTTClient;
import com.cit.usacycling.ant.background.mqtt.MQTTService;
import com.cit.usacycling.ant.background.mqtt.TopicFactory;
import com.cit.usacycling.ant.background.services.DeviceSearchService;
import com.cit.usacycling.ant.background.websocket.WSClient;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.ConnectionWatchStruct;
import com.cit.usacycling.ant.global.DataCollector;
import com.cit.usacycling.ant.global.DataPushManager;
import com.cit.usacycling.ant.global.InternetConnectionObserver;
import com.cit.usacycling.ant.global.SharedSettings;
import com.cit.usacycling.ant.global.StatusPushManager;
import com.cit.usacycling.ant.mock.devices.BSXInsightDeviceMock;
import com.cit.usacycling.ant.mock.devices.BikePowerDeviceMock;
import com.cit.usacycling.ant.mock.devices.HeartRateDeviceMock;
import com.cit.usacycling.ant.mock.services.DeviceSearchServiceMock;
import com.cit.usacycling.ant.ui.BpmParametersActivity;
import com.cit.usacycling.ant.ui.ConfigureBPMActivity;
import com.cit.usacycling.ant.ui.DeviceScanActivity;
import com.cit.usacycling.ant.ui.DevicesListAdapter;
import com.cit.usacycling.ant.ui.IoTBasicConnectionSetActivity;
import com.cit.usacycling.ant.ui.MainActivity;
import com.cit.usacycling.ant.ui.MqttSettingsActivity;
import com.cit.usacycling.ant.ui.NewMqttConfigurationAlertDialog;
import com.cit.usacycling.ant.ui.RenameDeviceAlertDialog;
import com.cit.usacycling.ant.ui.StatisticsActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by nikolay.nikolov on 2.11.2015
 */

@Module(
        library = true,
        injects = {
                BikePowerDevice.class,
                BikePowerDeviceMock.class,
                BSXInsightDevice.class,
                BSXInsightDeviceMock.class,
                BpmParametersActivity.class,
                CToast.class,
                ConfigureBPMActivity.class,
                ConnectionWatchStruct.class,
                DataPushManager.class,
                DbProvider.class,
                BuildProperties.class,
                DevicesListAdapter.class,
                DeviceScanActivity.class,
                DeviceSearchService.class,
                DeviceSearchServiceMock.class,
                IoTBasicConnectionSetActivity.class,
                InternetConnectionObserver.class,
                MainActivity.class,
                MqttSettingsActivity.class,
                MQTTClient.class,
                MQTTService.class,
                MQTTCallbackHandler.class,
                NewMqttConfigurationAlertDialog.class,
                HeartRateDevice.class,
                HeartRateDeviceMock.class,
                RenameDeviceAlertDialog.class,
                SharedSettings.class,
                StatusPushManager.class,
                StatisticsActivity.class,
                USACyclingExceptionHandler.class,
                UnitOfWork.class,
                WSClient.class
        }
)
public class ApplicationModule {
    private Application app;

    public ApplicationModule(Application app) {
        this.app = app;
    }

    @Provides
    @Singleton
    public Context provideApplicationContext() {
        return app;
    }

    @Provides
    @Singleton
    public DataCollector provideDataCollector() {
        return new DataCollector();
    }

    @Provides
    @Singleton
    public SharedSettings provideSharedSettings() {
        return new SharedSettings();
    }

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences() {
        return app.getSharedPreferences(app.getPackageName(), Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public TopicFactory provideTopicFactory() {
        return new TopicFactory();
    }

    @Provides
    @Singleton
    public MQTTClient provideMQTTClient() {
        return new MQTTClient();
    }

    @Provides
    @Singleton
    public DbProvider provideDb() {
        return new DbProvider(provideApplicationContext());
    }

    @Provides
    @Singleton
    public BuildProperties provideBuildProperties() {
        return new BuildProperties();
    }

    @Provides
    @Singleton
    public CToast provideCToast() {
        return new CToast(provideApplicationContext());
    }

    @Provides
    @Singleton
    public InternetConnectionObserver provideInetConnObserver() {
        return new InternetConnectionObserver(provideApplicationContext());
    }

    @Provides
    @Singleton
    public UnitOfWorkInterface provideUnitOfWork() {
        return new UnitOfWork();
    }
}


