package com.cit.usacycling.ant.background.websocket.requests;

import android.util.Log;

import com.cit.usacycling.ant.background.websocket.Sendable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nikolay.nikolov on 25.2.2016
 */
public class UpdateTrackingDeviceRequest implements Sendable {
    private final String SERVICE_NAME = "UpdateTrackingDeviceService";
    private final String TYPE = "service";
    private ServiceRequest request;
    private String params;

    public UpdateTrackingDeviceRequest(String requestId) {
        request = new ServiceRequest(
                SERVICE_NAME,
                requestId,
                TYPE,
                null,
                null);
    }

    public void parseParams(String identifier, String name, int deviceType, String deviceDescription) {
        try {
            this.params = new JSONObject().put("body", new JSONObject()
                    .put("identifier", identifier)
                    .put("name", name)
                    .put("deviceType", deviceType)
                    .put("deviceDescription", deviceDescription)).toString();
            this.request.setParams(params);
        } catch (JSONException e) {
            Log.e(UpdateTrackingDeviceRequest.class.getSimpleName(), "Error parsing request parameters");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return request.toString();
    }
}
