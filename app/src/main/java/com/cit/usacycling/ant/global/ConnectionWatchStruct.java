package com.cit.usacycling.ant.global;

import android.app.Activity;
import android.os.Handler;
import android.widget.Toast;

import com.cit.usacycling.ant.USACyclingApplication;

import javax.inject.Inject;

public class ConnectionWatchStruct {
    @Inject
    InternetConnectionObserver connectionObserver;

    @Inject
    CToast cToast;

    Activity context;

    public ConnectionWatchStruct(Activity activityContext) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = activityContext;
    }

    final Handler inetConnHandler = new Handler();
    final Runnable inetConnRunnable = new Runnable() {
        @Override
        public void run() {
            if (!connectionObserver.isNetworkConnectionAvailable()) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cToast.makeText("No internet connection", Toast.LENGTH_SHORT);
                    }
                });
            }
            inetConnHandler.postDelayed(inetConnRunnable, 2500);
        }
    };

    public boolean isNetworkConnectionAvailable() {
        return this.connectionObserver.isNetworkConnectionAvailable();
    }

    public ConnectionWatchStruct() {
        USACyclingApplication.getObjectGraph().inject(this);
    }

    public void setConnectionCheck() {
        inetConnHandler.post(inetConnRunnable);
    }

    public void cancelConnectionCheck() {
        inetConnHandler.removeCallbacks(inetConnRunnable);
    }

}
