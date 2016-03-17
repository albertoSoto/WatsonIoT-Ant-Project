package com.cit.usacycling.ant.background;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;

/**
 * Created by nikolay.nikolov on 02.03.2016
 */
public class CalibrationMessageWrapper {
    private AntPlusBikePowerPcc.CalibrationMessage message;
    private String deviceId;

    public CalibrationMessageWrapper(AntPlusBikePowerPcc.CalibrationMessage message, String deviceId) {
        this.message = message;
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        String calibrationId = "Not available";
        if (message.calibrationId != null) {
            calibrationId = message.calibrationId.toString();
        }

        String manufacturerSpecData = "Not available";
        if (message.manufacturerSpecificData != null) {
            manufacturerSpecData = new String(message.manufacturerSpecificData);
        }

        String offset = "Not available";
        if (message.ctfOffset != null) {
            offset = message.ctfOffset.toString();
        }

        String calibrationData = "Not available";
        if (message.calibrationData != null) {
            calibrationData = message.calibrationData.toString();
        }

        String result =
                "DeviceId: %s %n" +
                        "Calibration Id: %s %n" +
                        "Offset: %s %n" +
                        "Manufacturer data: %s %n" +
                        "Calibration data: %s %n";

        return String.format(result, deviceId, calibrationId, offset, manufacturerSpecData, calibrationData);
    }
}
