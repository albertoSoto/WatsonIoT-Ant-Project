package com.cit.usacycling.ant.background.callbacks;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;

/**
 * Created by nikolay.nikolov on 02.03.2016
 */
public interface AntBikePowerCalibrationCallbackReceiver {
    void onCalibrationMessageArrived(AntPlusBikePowerPcc.CalibrationMessage message, String deviceId);
}
