package com.cit.usacycling.ant.background.db.repositories;

import android.content.ContentValues;
import android.database.Cursor;

import com.cit.usacycling.ant.background.db.DbConstants;
import com.cit.usacycling.ant.background.db.DbProvider;
import com.cit.usacycling.ant.background.db.MqttConfiguration;

import java.util.HashSet;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class MqttConfigurationRepository implements MqttConfigurationRepositoryInterface {
    private DbProvider dbProvider;

    public MqttConfigurationRepository(DbProvider dbProvider) {
        this.dbProvider = dbProvider;
    }

    @Override
    public boolean insertConfiguration(MqttConfiguration message) {
        MqttConfiguration existingConfig = getConfigurationByDeviceId(message.getDeviceId());
        if (existingConfig != null) {
            return false;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(DbConstants.MQTT_COLUMN_DEVICE_ID, message.getDeviceId());
        contentValues.put(DbConstants.MQTT_COLUMN_DEVICE_TOKEN, message.getDeviceToken());
        contentValues.put(DbConstants.MQTT_COLUMN_HOST, message.getHostPostfix());
        contentValues.put(DbConstants.MQTT_COLUMN_ORG_ID, message.getOrganizationId());
        contentValues.put(DbConstants.MQTT_COLUMN_USER_ID, message.getUserIdValue());
        contentValues.put(DbConstants.MQTT_COLUMN_PORT, message.getPortValue());
        contentValues.put(DbConstants.MQTT_COLUMN_STATUS_QOS, message.getStatusMessageQoS());
        contentValues.put(DbConstants.MQTT_COLUMN_DATA_QOS, message.getDataMessageQoS());

        try {
            this.dbProvider.getWritable().insert(DbConstants.MQTT_CONFIG_TABLE_NAME, null, contentValues);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public HashSet<MqttConfiguration> getAllConfigurations() {
        HashSet<MqttConfiguration> result = new HashSet<>();
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.MQTT_CONFIG_TABLE_NAME, null);
            if (data.moveToFirst()) {
                do {
                    String deviceId = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_DEVICE_ID));
                    String deviceToken = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_DEVICE_TOKEN));
                    String port = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_PORT));
                    String orgId = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_ORG_ID));
                    String host = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_HOST));
                    String userId = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_USER_ID));
                    int dataQoS = data.getInt(data.getColumnIndex(DbConstants.MQTT_COLUMN_DATA_QOS));
                    int statusQoS = data.getInt(data.getColumnIndex(DbConstants.MQTT_COLUMN_STATUS_QOS));
                    result.add(new MqttConfiguration(deviceId, deviceToken, orgId, userId, host, port, statusQoS, dataQoS, 0));
                } while (data.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return result;
    }

    @Override
    public MqttConfiguration getConfigurationByDeviceId(String deviceId) {
        MqttConfiguration configuration = null;
        Cursor data = null;

        try {
            data = this.dbProvider.getReadable().rawQuery("SELECT * FROM " + DbConstants.MQTT_CONFIG_TABLE_NAME + " WHERE " + DbConstants.MQTT_COLUMN_DEVICE_ID + " LIKE ?", new String[]{deviceId});
            if (data.moveToFirst()) {
                String deviceToken = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_DEVICE_TOKEN));
                String port = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_PORT));
                String orgId = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_ORG_ID));
                String host = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_HOST));
                String userId = data.getString(data.getColumnIndex(DbConstants.MQTT_COLUMN_USER_ID));
                int dataQoS = data.getInt(data.getColumnIndex(DbConstants.MQTT_COLUMN_DATA_QOS));
                int statusQoS = data.getInt(data.getColumnIndex(DbConstants.MQTT_COLUMN_STATUS_QOS));
                configuration = new MqttConfiguration(deviceId, deviceToken, orgId, userId, host, port, statusQoS, dataQoS, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (data != null) {
                data.close();
            }
        }

        return configuration;
    }

    @Override
    public boolean updateConfiguration(MqttConfiguration configuration) {
        try {
            ContentValues values = new ContentValues();
            values.put(DbConstants.MQTT_COLUMN_DEVICE_TOKEN, configuration.getDeviceToken());
            values.put(DbConstants.MQTT_COLUMN_PORT, configuration.getPortValue());
            values.put(DbConstants.MQTT_COLUMN_ORG_ID, configuration.getOrganizationId());
            values.put(DbConstants.MQTT_COLUMN_HOST, configuration.getHostPostfix());
            values.put(DbConstants.MQTT_COLUMN_USER_ID, configuration.getUserIdValue());
            values.put(DbConstants.MQTT_COLUMN_DATA_QOS, configuration.getDataMessageQoS());
            values.put(DbConstants.MQTT_COLUMN_STATUS_QOS, configuration.getStatusMessageQoS());

            this.dbProvider.getWritable().update(DbConstants.MQTT_CONFIG_TABLE_NAME, values, DbConstants.MQTT_COLUMN_DEVICE_ID + " LIKE ?", new String[]{configuration.getDeviceId()});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteConfiguration(String deviceId) {
        try {
            return this.dbProvider.getWritable().delete(DbConstants.MQTT_CONFIG_TABLE_NAME, DbConstants.MQTT_COLUMN_DEVICE_ID + " LIKE ?", new String[]{deviceId}) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
