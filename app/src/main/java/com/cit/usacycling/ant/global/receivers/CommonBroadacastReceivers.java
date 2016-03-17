package com.cit.usacycling.ant.global.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.ui.DevicesListAdapter;

/**
 * Created by nikolay.nikolov on 1.2.2016
 */
public class CommonBroadacastReceivers {
    public static BroadcastReceiver getUpdateListViewAdapterReceiver(final Activity activity, final DevicesListAdapter adapter) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_UPDATE_ADAPTER))
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
            }
        };
    }
}
