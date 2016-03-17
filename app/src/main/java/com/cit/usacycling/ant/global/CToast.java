package com.cit.usacycling.ant.global;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.cit.usacycling.ant.USACyclingApplication;

/**
 * Created by nikolay.nikolov on 24.2.2016
 */
public class CToast {
    private Toast toast;
    private Context context;

    public CToast(Context context) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = context;
    }

    @SuppressLint("ShowToast")
    public Toast makeText(CharSequence text, int duration) {
        if (this.toast == null) {
            this.toast = Toast.makeText(context, text, duration);
            this.toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 15, 15);
            TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
            if (v != null) v.setGravity(Gravity.CENTER);
        } else {
            this.toast.setText(text);
            this.toast.setDuration(duration);
        }

        this.toast.show();
        return this.toast;
    }

    public void cancel() {
        if (this.toast != null) {
            this.toast.cancel();
        }
    }
}
