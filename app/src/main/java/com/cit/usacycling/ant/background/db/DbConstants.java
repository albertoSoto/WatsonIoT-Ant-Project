package com.cit.usacycling.ant.background.db;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class DbConstants {
    public static final String DATABASE_NAME = "USACyclingDevices.db";
    public static final String MESSAGES_TABLE_NAME = "messages";
    public static final String DEVICES_TABLE_NAME = "devices";
    public static final String MQTT_CONFIG_TABLE_NAME = "mqtt_configuration";
    public static final String DEVICES_COLUMN_NAME = "name";
    public static final String DEVICES_COLUMN_TYPE = "type";
    public static final String DEVICES_COLUMN_PAIRED = "paired";
    public static final String DEVICES_COLUMN_ACTIVE = "active";
    public static final String DEVICES_COLUMN_STATUS = "status";
    public static final String DEVICES_COLUMN_NUMBER = "number";
    public static final String MESSAGES_COLUMN_PAYLOAD = "payload";
    public static final String MESSAGES_COLUMN_SENT_STATUS = "sent";

    public static final String MQTT_COLUMN_DEVICE_ID = "device_id";
    public static final String MQTT_COLUMN_DEVICE_TOKEN = "device_token";
    public static final String MQTT_COLUMN_ORG_ID = "org_id";
    public static final String MQTT_COLUMN_USER_ID = "user_id";
    public static final String MQTT_COLUMN_HOST = "host";
    public static final String MQTT_COLUMN_PORT = "port";
    public static final String MQTT_COLUMN_STATUS_QOS = "status_qos";
    public static final String MQTT_COLUMN_DATA_QOS = "data_qos";
    public static final String MQTT_COLUMN_IS_CHECKED = "is_checked";

    public static final String DISCONNECTED_STATUS = "DISCONNECTED";

    public static final String CREATE_DEVICES_TABLE_QUERY =
            "CREATE TABLE " + DEVICES_TABLE_NAME +
                    "(" +
                    DEVICES_COLUMN_NUMBER + " TEXT PRIMARY KEY NOT NULL," +
                    DEVICES_COLUMN_NAME + " TEXT NOT NULL," +
                    DEVICES_COLUMN_TYPE + " TEXT NOT NULL," +
                    DEVICES_COLUMN_PAIRED + " INTEGER NOT NULL," +
                    DEVICES_COLUMN_ACTIVE + " INTEGER NOT NULL," +
                    DEVICES_COLUMN_STATUS + " TEXT NOT NULL" +
                    ")";

    public static final String CREATE_MESSAGES_TABLE_QUERY =
            "CREATE TABLE " + MESSAGES_TABLE_NAME +
                    "(" +
                    DEVICES_COLUMN_NUMBER + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    MESSAGES_COLUMN_PAYLOAD + " TEXT NOT NULL," +
                    MESSAGES_COLUMN_SENT_STATUS + " INTEGER NOT NULL" +
                    ")";

    public static final String CREATE_MQTT_CONFIGURATION_TABLE_QUERY =
            "CREATE TABLE " + MQTT_CONFIG_TABLE_NAME +
                    "(" +
                    MQTT_COLUMN_DEVICE_ID + " TEXT PRIMARY KEY NOT NULL," +
                    MQTT_COLUMN_DEVICE_TOKEN + " TEXT NOT NULL," +
                    MQTT_COLUMN_ORG_ID + " TEXT NOT NULL," +
                    MQTT_COLUMN_USER_ID + " TEXT NOT NULL," +
                    MQTT_COLUMN_HOST + " TEXT NOT NULL," +
                    MQTT_COLUMN_PORT + " TEXT NOT NULL," +
                    MQTT_COLUMN_STATUS_QOS + " INTEGER NOT NULL," +
                    MQTT_COLUMN_DATA_QOS + " INTEGER NOT NULL," +
                    MQTT_COLUMN_IS_CHECKED + " INTEGER NOT NULL" +
                    ")";


}
