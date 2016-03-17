package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.cit.usacycling.ant.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ExceptionActivity extends Activity {

    @Bind(R.id.tv_error)
    TextView mErrorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exception);
        ButterKnife.bind(this, findViewById(android.R.id.content));

        mErrorTextView.setText(getIntent().getStringExtra("error"));
    }
}
