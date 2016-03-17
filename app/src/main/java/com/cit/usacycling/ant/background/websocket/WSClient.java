package com.cit.usacycling.ant.background.websocket;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.CToast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 28.1.2016
 */
public class WSClient {
    private static final String TAG = WSClient.class.getSimpleName();
    private WebSocketClient mWebSocketClient;
    private boolean isConnected = false;
    private Activity context;
    private Notifiable notifiable;

    @Inject
    BuildProperties buildProperties;
    @Inject
    CToast cToast;

    public WSClient(Activity context, Notifiable notifiable) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = context;
        this.notifiable = notifiable;
    }

    public boolean send(Sendable request) {
        if (isConnected) {
            Log.i(TAG, "Sending: " + request.toString());
            this.mWebSocketClient.send(request.toString());
            return true;
        }

        return false;
    }

    public void connect(String host, String port) {
        this.connect(host, port, null);
    }

    public void connect(String host, String port, JSONObject params) {
        try {
            String uriString = host;
            if (port != null && !port.trim().equals("")) {
                uriString += ":" + port;
            }
            String paramString = "";

            if (params != null) {
                paramString = parseParams(params);
            }
            Log.i(TAG, "Connecting web socket client...");
            initializeWebSocketClient(uriString + paramString);
            mWebSocketClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Could not connect web socket client");
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cToast.makeText("ERROR: Could not connect web socket client", Toast.LENGTH_SHORT);
                }
            });
            e.printStackTrace();
        }
    }

    public void disconnect() {
        Log.i(TAG, "Closing web socket client...");
        this.mWebSocketClient.close();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connectClientToBaseHostWithAuthParams() {
        JSONObject wsParams = new JSONObject();
        try {
            wsParams.put("auth", "test");
            wsParams.put("name", "android");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.connect(buildProperties.getBackendAddress(), null, wsParams);
    }

    private void initializeWebSocketClient(String uri) {
        try {
            URI targetURI = new URI(uri);
            mWebSocketClient = new WebSocketClient(targetURI) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i("WebSocket", "Opened");
                    isConnected = true;
                }

                @Override
                public void onMessage(String s) {
                    Log.i(TAG, "Response arrived: " + s);
                    JSONObject json = null;
                    try {
                        json = new JSONObject(s);
                        if (notifiable != null) {
                            notifiable.notifyForResponse(json.getString("id"), json.getInt("code"), s, context);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i(TAG, "Closed: " + s);
                    isConnected = false;
                }

                @Override
                public void onError(Exception e) {
                    Log.i(TAG, "Error: " + e.getMessage());
//                    context.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            cToast.makeText("Could not connect web socket to back-end. Check internet connection.", Toast.LENGTH_SHORT);
//                        }
//                    });
                }
            };
        } catch (URISyntaxException e) {
            Log.e(TAG, "Unable to initialize web socket client");
            e.printStackTrace();
        }
    }

    private String parseParams(JSONObject params) {
        String paramString = "";
        JSONArray keys = params.names();

        for (int i = 0; i < keys.length(); i++) {
            if (i == 0) {
                paramString += "?";
            }

            try {
                String currentKey = (String) keys.get(i);
                paramString += (currentKey + "=" + params.get(currentKey));

                if (i < keys.length() - 1) {
                    paramString += "&";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return paramString;
    }
}
