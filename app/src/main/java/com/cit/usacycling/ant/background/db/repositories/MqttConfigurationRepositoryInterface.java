package com.cit.usacycling.ant.background.db.repositories;

import com.cit.usacycling.ant.background.db.MqttConfiguration;

import java.util.HashSet;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public interface MqttConfigurationRepositoryInterface {
    boolean insertConfiguration(MqttConfiguration message);

    HashSet<MqttConfiguration> getAllConfigurations();

    MqttConfiguration getConfigurationByDeviceId(String deviceId);

    boolean updateConfiguration(MqttConfiguration configuration);

    boolean deleteConfiguration(String deviceId);
}
