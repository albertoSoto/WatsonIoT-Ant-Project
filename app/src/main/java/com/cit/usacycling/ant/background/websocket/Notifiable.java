package com.cit.usacycling.ant.background.websocket;

import android.app.Activity;

/**
 * Created by nikolay.nikolov on 29.1.2016
 */
public interface Notifiable {
    void notifyForResponse(String id, int statusCode, String message, Activity context);
}
