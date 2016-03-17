package com.cit.usacycling.ant.global;

import android.util.Log;

import com.cit.usacycling.ant.background.DeviceDataTypeStruct;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by nikolay.nikolov on 3.11.2015
 */
public class DataCollector {
    public final String TAG = this.getClass().getSimpleName();

    private HashMap<DeviceDataTypeStruct, Queue<JSONObject>> dataContainer;

    public DataCollector() {
        this.dataContainer = new HashMap<>();
    }

    public void addData(DeviceDataTypeStruct struct, JSONObject data) {
        if (dataContainer.containsKey(struct)) {
            addDataInternal(struct, data);
        } else {
            dataContainer.put(struct, new ConcurrentLinkedQueue<JSONObject>());
            addDataInternal(struct, data);
        }
    }

    public List<JSONObject> pollData(DeviceDataTypeStruct struct) throws JSONException {
        List<JSONObject> list = new ArrayList<>();
        Queue<JSONObject> currentQueue = dataContainer.get(struct);
        if (currentQueue != null) {
            JSONObject object = null;
            while ((object = currentQueue.poll()) != null) {
                list.add(object);
            }
        }

        return list;
    }

    public Set<DeviceDataTypeStruct> getDataContainerKeys() {
        return dataContainer.keySet();
    }

    private void addDataInternal(DeviceDataTypeStruct struct, JSONObject data) {
        try {
            dataContainer.get(struct).add(data);
        } catch (Exception e) {
            Log.d(TAG, "Failed adding data\n" + data);
            e.printStackTrace();
        }
    }
}
