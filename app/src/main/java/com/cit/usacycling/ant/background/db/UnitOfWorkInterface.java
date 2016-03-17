package com.cit.usacycling.ant.background.db;

import com.cit.usacycling.ant.background.db.repositories.DeviceRepositoryInterface;
import com.cit.usacycling.ant.background.db.repositories.MessageRepositoryInterface;
import com.cit.usacycling.ant.background.db.repositories.MqttConfigurationRepositoryInterface;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public interface UnitOfWorkInterface {
    MessageRepositoryInterface getMessageRepository();

    DeviceRepositoryInterface getDeviceRepository();

    MqttConfigurationRepositoryInterface getMqttConfigurationRepository();
}
