package com.cit.usacycling.ant.background.websocket.requests;

import com.cit.usacycling.ant.background.websocket.Sendable;

/**
 * Created by nikolay.nikolov on 25.2.2016
 */
public class GetDroidBroadcastingDevicesRequest implements Sendable {
    private final String SERVICE_NAME = "GetDroidBroadcastingDevicesService";
    private final String TYPE = "service";
    private ServiceRequest request;

    public GetDroidBroadcastingDevicesRequest(String requestId) {
        request = new ServiceRequest(
                SERVICE_NAME,
                requestId,
                TYPE,
                null,
                null);
    }

    @Override
    public String toString() {
        return request.toString();
    }
}
