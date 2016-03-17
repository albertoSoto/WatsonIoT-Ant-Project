package com.cit.usacycling.ant.background.mqtt;

import com.cit.usacycling.ant.global.Constants;

/**
 * Created by nikolay.nikolov on 19.10.2015
 * <p/>
 * Build topic strings used by the application.
 */
public class TopicFactory {

    public static final String ANT_DATA_EVENT = "antdata";

    public String getEventTopic(String event) {
        return Constants.Topic.EVENT_PREFIX + event + Constants.Topic.EVENT_FORMAT;
    }

    public String getCommandTopic(String command) {
        return "iot-2/cmd/" + command + "/fmt/json";
    }
}