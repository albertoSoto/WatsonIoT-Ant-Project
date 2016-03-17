package com.cit.usacycling.ant.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.global.ConnectionWatchStruct;
import com.cit.usacycling.ant.global.SharedSettings;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StatisticsActivity extends Activity {

    @Bind(R.id.tvTotalMessagesValue)
    TextView totalMessages;

    @Bind(R.id.tvDataMessagesCountValue)
    TextView totalDataMessages;

    @Bind(R.id.tvStatusMessagesCountValue)
    TextView totalStatusMessages;

    @Bind(R.id.tvTotalMessagesReceivedValue)
    TextView totalReceivedMessages;

    @Bind(R.id.tvDataMessagesReceivedValue)
    TextView totalDataReceivedMessages;

    @Bind(R.id.tvStatusMessagesReceivedValue)
    TextView totalStatusReceivedMessages;

    @Bind(R.id.tvTotalMessagesLostValue)
    TextView totalLostMessages;

    @Bind(R.id.tvDataMessagesLostValue)
    TextView totalDataLostMessages;

    @Bind(R.id.tvStatusMessagesLostValue)
    TextView totalStatusLostMessages;

    @Inject
    SharedSettings settings;

    private final ConnectionWatchStruct connectionWatchStruct = new ConnectionWatchStruct(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        USACyclingApplication.getObjectGraph().inject(this);
        super.onCreate(savedInstanceState);
        connectionWatchStruct.setConnectionCheck();
        setContentView(R.layout.activity_statistics);
        ButterKnife.bind(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        loadValuesToTextViews();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int selectedId = item.getItemId();
        if (selectedId == R.id.action_refresh) {
            loadValuesToTextViews();
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionWatchStruct.cancelConnectionCheck();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return true;
    }

    private void loadValuesToTextViews() {
        totalMessages.setText(String.valueOf(settings.getTotalMessagesCount()));
        totalDataMessages.setText(String.valueOf(settings.getTotalDataMessagesCount()));
        totalStatusMessages.setText(String.valueOf(settings.getTotalStatusMessagesCount()));

        totalReceivedMessages.setText(String.valueOf(settings.getTotalDeliveredMessagesCount()));
        totalDataReceivedMessages.setText(String.valueOf(settings.getDeliveredDataMessagesCount()));
        totalStatusReceivedMessages.setText(String.valueOf(settings.getDeliveredStatusMessagesCount()));

        totalLostMessages.setText(String.valueOf(settings.getTotalLostMessagesCount()));
        totalDataLostMessages.setText(String.valueOf(settings.getLostDataMessagesCount()));
        totalStatusLostMessages.setText(String.valueOf(settings.getLostStatusMessagesCount()));
    }
}
