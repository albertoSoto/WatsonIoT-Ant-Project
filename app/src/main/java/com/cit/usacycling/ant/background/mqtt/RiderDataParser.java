package com.cit.usacycling.ant.background.mqtt;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nikolay.nikolov on 3.2.2016
 */
public class RiderDataParser {
    
    public static String getRiderDataJson(int matchesBurnedCount) {
        JSONObject json = new JSONObject();

        try {
            json.put("matches", new JSONObject().put("total", matchesBurnedCount));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }
}
