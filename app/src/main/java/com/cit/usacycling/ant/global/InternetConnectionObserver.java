package com.cit.usacycling.ant.global;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.cit.usacycling.ant.USACyclingApplication;

/**
 * Created by nikolay.nikolov on 24.2.2016
 */
public class InternetConnectionObserver {
    private Context context;

    public InternetConnectionObserver(Context context) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = context;
    }

    public boolean isNetworkConnectionAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}

