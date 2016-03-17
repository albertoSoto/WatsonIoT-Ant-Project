package com.cit.usacycling.ant.background.db;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.repositories.DeviceRepository;
import com.cit.usacycling.ant.background.db.repositories.DeviceRepositoryInterface;
import com.cit.usacycling.ant.background.db.repositories.MessageRepository;
import com.cit.usacycling.ant.background.db.repositories.MessageRepositoryInterface;
import com.cit.usacycling.ant.background.db.repositories.MqttConfigurationRepository;
import com.cit.usacycling.ant.background.db.repositories.MqttConfigurationRepositoryInterface;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class UnitOfWork implements UnitOfWorkInterface {
    private MessageRepositoryInterface messageRepository;
    private DeviceRepositoryInterface deviceRepository;
    private MqttConfigurationRepositoryInterface mqttConfigurationRepository;

    @Inject
    DbProvider dbProvider;

    public UnitOfWork() {
        USACyclingApplication.getObjectGraph().inject(this);
    }

    public MessageRepositoryInterface getMessageRepository() {
        if (this.messageRepository == null) {
            this.messageRepository = new MessageRepository(dbProvider);
        }

        return this.messageRepository;
    }

    public DeviceRepositoryInterface getDeviceRepository() {
        if (this.deviceRepository == null) {
            this.deviceRepository = new DeviceRepository(dbProvider);
        }

        return this.deviceRepository;
    }

    public MqttConfigurationRepositoryInterface getMqttConfigurationRepository() {
        if (this.mqttConfigurationRepository == null) {
            this.mqttConfigurationRepository = new MqttConfigurationRepository(dbProvider);
        }

        return this.mqttConfigurationRepository;
    }
}
