package com.cit.usacycling.ant.background.websocket.requests;

import android.util.Log;

import com.cit.usacycling.ant.background.websocket.Sendable;

/**
 * Created by nikolay.nikolov on 29.1.2016
 */
public class ServiceRequest implements Sendable {
    private static Integer counter = 1;

    private String id;
    private String type;
    private String params;
    private String name;
    private String expectedResponse;
    private String actualResponse;

    public ServiceRequest() {
    }

    public ServiceRequest(String name, String id, String type, String params, String expectedResponse) {
        super();
        this.id = id;
        this.type = type;
        this.params = params;
        this.name = name;
        this.expectedResponse = expectedResponse;
    }

    public ServiceRequest(String name, String id, String params, String expectedResponse) {
        this(name, id, "service", params, expectedResponse);
    }

    public ServiceRequest(String name, String params) {
        this(name, (counter++).toString(), "service", params, "");
    }

    @Override
    public String toString() {
        String str = "{ \"id\":\"" + id + "\", \"name\":\"" + name + "\", \"type\":\"" + type + "\"";
        if (params != null) {
            str += ", \"params\":" + params;
        }

        str += "}";
        Log.d("JSON", str);

        return str;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getActualResponse() {
        return actualResponse;
    }

    public void setActualResponse(String actualResponse) {
        this.actualResponse = actualResponse;
    }
}
