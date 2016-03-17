package com.cit.usacycling.ant.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.mqtt.MQTTClient;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.SharedSettings;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class IoTBasicConnectionSetActivity extends Activity {

    @Inject
    SharedSettings settings;

    @Inject
    BuildProperties buildProperties;

    @Inject
    CToast cToast;

    private boolean credentialsChanged;
    private boolean dataQoSChanged;
    private boolean statusQoSChanged;

    private String[] definedDevices;
    private String[] definedTokens;

    private String updateId;
    private String updatePass;

    @Inject
    MQTTClient client;

    @Bind(R.id.rgDataQoS)
    RadioGroup dataQoSRgroup;

    @Bind(R.id.rgStatusQoS)
    RadioGroup statusQoSRgroup;

    @Bind(R.id.rbQoS0)
    RadioButton dqos0;

    @Bind(R.id.rbQoS1)
    RadioButton dqos1;

    @Bind(R.id.rbQoS2)
    RadioButton dqos2;

    @Bind(R.id.rbSQoS0)
    RadioButton sqos0;

    @Bind(R.id.rbSQoS1)
    RadioButton sqos1;

    @Bind(R.id.rbSQoS2)
    RadioButton sqos2;

    private int updateDataQoS;
    private int updateStatusQoS;

    @Bind(R.id.btnExit)
    Button exitBtn;
    @Bind(R.id.linearLayout)
    LinearLayout linearLayout;

    @Bind(R.id.noDeviceLayout)
    LinearLayout noDeviceLayout;

    @Bind(R.id.tryAgainBtn)
    Button retryButton;

    private boolean isExitRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        USACyclingApplication.getObjectGraph().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_io_tbasic_connection_set);
        ButterKnife.bind(this);
        ActionBar aBar = getActionBar();
        if (aBar != null) {
            aBar.setDisplayHomeAsUpEnabled(false);
        }

        credentialsChanged = false;
        dataQoSChanged = false;
        statusQoSChanged = false;
        isExitRequested = false;

        updateBroadcastingDevices(this);
    }

    @Override
    public void onBackPressed() {
        if (isExitRequested && settings.getIoTCredentials() != null) {
            isExitRequested = false;
            super.onBackPressed();
        } else if (isExitRequested) {
            isExitRequested = false;
            cToast.makeText("Set credentials and save before exit", Toast.LENGTH_SHORT);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int selectedId = item.getItemId();
        if (updateId == null) {
            updateId = definedDevices[0];
            updatePass = definedTokens[0];
        }
        if (selectedId == R.id.save) {
            if (!statusQoSChanged && !dataQoSChanged && !credentialsChanged) {
                cToast.makeText("Nothing to save", Toast.LENGTH_SHORT);
            } else {

                if (credentialsChanged) {
                    credentialsChanged = false;
                    buildProperties.setSelectedDeviceId(updateId);
                    buildProperties.setSelectedDevicePass(updatePass);
                    settings.setIoTCredentials(updateId, updatePass);
                    client.setUsernameAndPassword(buildProperties.getUserIdValue(), updatePass);
                    cToast.makeText("Credentials saved", Toast.LENGTH_SHORT);
                }

                if (statusQoSChanged) {
                    statusQoSChanged = false;
                    buildProperties.setStatusMessageQoS(updateStatusQoS);
                    settings.setStatusMessageQoS(updateStatusQoS);
                    cToast.makeText("Status QoS saved", Toast.LENGTH_SHORT);
                }
                if (dataQoSChanged) {
                    dataQoSChanged = false;
                    buildProperties.setDataMessageQoS(updateDataQoS);
                    settings.setDataMessageQoS(updateDataQoS);
                    cToast.makeText("Data QoS saved", Toast.LENGTH_SHORT);
                }
            }
        }

        return super.onMenuItemSelected(featureId, item);
    }

    private Menu menu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_iot_settings, menu);

        return true;
    }

    private void markDataQoSRB(int dataQoS) {
        if (dataQoS == 0) {
            dqos0.setChecked(true);
        } else if (dataQoS == 1) {
            dqos1.setChecked(true);
        } else {
            dqos2.setChecked(true);
        }
    }

    private void markStatusQoSRB(int statusQoS) {
        if (statusQoS == 0) {
            sqos0.setChecked(true);
        } else if (statusQoS == 1) {
            sqos1.setChecked(true);
        } else {
            sqos2.setChecked(true);
        }
    }

    int requestIds = 1;
    ProgressDialog completeDialog;

    private Activity getCurrentActivity() {
        return this;
    }

    private void updateBroadcastingDevices(final Activity activity) {
        noDeviceLayout.setVisibility(View.GONE);
        if (menu != null) {
            menu.findItem(R.id.save).setEnabled(true);
        }

        if (activity != null) {
            try {
                if (completeDialog == null || !completeDialog.isShowing()) {
                    completeDialog = new ProgressDialog(getCurrentActivity());
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            completeDialog.setTitle("Extracting android devices");
                            completeDialog.setMessage("Please wait ...");
                            completeDialog.setCancelable(false);
                            completeDialog.setIndeterminate(true);
                            completeDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "EXIT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    completeDialog.dismiss();
                                    finish();
                                }
                            });
                        }
                    });

                    completeDialog.show();
                }

            } catch (WindowManager.BadTokenException e) {
                Log.e("MQTT Settings", "Could not show progress dialog because activity has been destroyed and context lost");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (settings.getIoTCredentials() == null) {
            settings.setIotSettingsReconnectRequired(true);
        }
        super.onDestroy();
    }

//    @Override
//    public void notifyForResponse(String id, final int statusCode, String message, Activity context) {
//        JSONArray content = null;
//        try {
//            content = new JSONObject(message).getJSONArray("content");
//        } catch (JSONException e) {
//            Log.e("MQTT Settings", e.toString());
//            e.printStackTrace();
//        } finally {
//            if (content == null) {
//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        updateBroadcastingDevices(getCurrentActivity());
//                    }
//                });
//            }
//
//            if (content != null && content.length() == 0) {
//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        cToast.makeText("There are no broadcasting devices for the current configuration", Toast.LENGTH_SHORT);
//                        menu.findItem(R.id.save).setEnabled(false);
//                        noDeviceLayout.setVisibility(View.VISIBLE);
//                        retryButton.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                updateBroadcastingDevices(getCurrentActivity());
//                            }
//                        });
//                        completeDialog.dismiss();
//                        exitBtn.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Intent intent = new Intent(Intent.ACTION_MAIN);
//                                intent.addCategory(Intent.CATEGORY_HOME);
//                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(intent);
//                            }
//                        });
//                    }
//                });
//            }
//        }
//
//        if (content == null || content.length() == 0) {
//            return;
//        }
//
//        if (ResponseCode.OK.compare(statusCode)) {
//            final String[] deviceIds = new String[content.length()];
//            final String[] devicePasswordValues = new String[content.length()];
//            final Map<String, String> devicePassMap = new HashMap<>();
//            for (int i = 0; i < content.length(); i++) {
//                try {
//                    JSONObject json = (JSONObject) content.get(i);
//                    deviceIds[i] = json.getString("identifier");
//                    devicePasswordValues[i] = json.getString("token");
//                    devicePassMap.put(json.getString("identifier"), json.getString("token"));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        completeDialog.dismiss();
//                        cToast.makeText("Android devices extracted successfully", Toast.LENGTH_SHORT);
//                    } catch (IllegalArgumentException e) {
//                        Log.e("MQTT Settings", "Could not dismiss dialog. Activity context lost.");
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        Log.e("MQTT Settings", e.toString());
//                        e.printStackTrace();
//                    }
//                }
//            });
//            buildProperties.setDeviceIds(deviceIds);
//            buildProperties.setDevicePasswordValues(devicePasswordValues);
//
//            Arrays.sort(deviceIds, new AlphanumComparator());
//
//            runOnUiThread(new Runnable() {
//                              @Override
//                              public void run() {
//                                  final List<RadioButton> definedCredentials = new ArrayList<>();
//                                  definedDevices = buildProperties.getDeviceIds();
//                                  definedTokens = buildProperties.getDevicePasswordValues();
//                                  updateDataQoS = settings.getDataMessageQoS();
//                                  updateStatusQoS = settings.getStatusMessageQoS();
//
//                                  markDataQoSRB(updateDataQoS);
//                                  markStatusQoSRB(updateStatusQoS);
//
//                                  String[] currentCredentials = settings.getIoTCredentials();
//                                  RadioGroup rg = new RadioGroup(IoTBasicConnectionSetActivity.this);
//                                  for (int i = 0; i < definedDevices.length; i++) {
//                                      RadioButton rb = new RadioButton(getApplicationContext());
//                                      if (currentCredentials != null && currentCredentials[0].equals(definedDevices[i])) {
//                                          rb.setChecked(true);
//                                      }
//                                      rb.setText(deviceIds[i]);
//
//                                      rb.setTextColor(ContextCompat.getColor(getCurrentActivity(), android.R.color.black));
//                                      definedCredentials.add(rb);
//                                      rb.setBackgroundColor(ContextCompat.getColor(getCurrentActivity(), android.R.color.transparent));
//                                      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                                              LinearLayout.LayoutParams.WRAP_CONTENT,
//                                              LinearLayout.LayoutParams.WRAP_CONTENT);
//                                      layoutParams.setMargins(0, 0, 0, 50);
//                                      rb.setLayoutParams(layoutParams);
//
//                                      rg.addView(rb);
//                                  }
//
//                                  linearLayout.addView(rg);
//                                  if (currentCredentials == null) {
//                                      definedCredentials.get(0).setChecked(true);
//                                      credentialsChanged = true;
//                                  } else {
//                                      updateId = currentCredentials[0];
//                                      updatePass = currentCredentials[1];
//                                  }
//
//                                  rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//                                      @Override
//                                      public void onCheckedChanged(RadioGroup group, int checkedId) {
//                                          String[] currentCredentials = settings.getIoTCredentials();
//                                          RadioButton rButton = (RadioButton) group.getChildAt(group.indexOfChild(group.findViewById(group.getCheckedRadioButtonId())));
//                                          String selectedText = rButton.getText().toString();
//
//                                          if (currentCredentials == null || !currentCredentials[0].equals(selectedText)) {
//                                              credentialsChanged = true;
//                                          }
//
//                                          updateId = selectedText;
//                                          updatePass = devicePassMap.get(selectedText);
//
//                                          for (int i = 0; i < definedCredentials.size(); i++) {
//                                              RadioButton rb = definedCredentials.get(i);
//                                              rb.setChecked(false);
//                                          }
//
//                                          for (int i = 0; i < definedCredentials.size(); i++) {
//                                              RadioButton rb = definedCredentials.get(i);
//                                              if (updateId.equals(rb.getText().toString())) {
//                                                  rb.setChecked(true);
//                                              }
//                                          }
//                                      }
//                                  });
//
//                                  dataQoSRgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//                                      @Override
//                                      public void onCheckedChanged(RadioGroup group, int checkedId) {
//                                          if (dqos0.isChecked() && updateDataQoS != 0) {
//                                              dataQoSChanged = true;
//                                              updateDataQoS = 0;
//                                          }
//
//                                          if (dqos1.isChecked() && updateDataQoS != 1) {
//                                              dataQoSChanged = true;
//                                              updateDataQoS = 1;
//                                          }
//
//                                          if (dqos2.isChecked() && updateDataQoS != 2) {
//                                              dataQoSChanged = true;
//                                              updateDataQoS = 2;
//                                          }
//                                      }
//                                  });
//
//                                  statusQoSRgroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//                                      @Override
//                                      public void onCheckedChanged(RadioGroup group, int checkedId) {
//                                          if (sqos0.isChecked() && updateStatusQoS != 0) {
//                                              statusQoSChanged = true;
//                                              updateStatusQoS = 0;
//                                          }
//
//                                          if (sqos1.isChecked() && updateStatusQoS != 1) {
//                                              statusQoSChanged = true;
//                                              updateStatusQoS = 1;
//                                          }
//
//                                          if (sqos2.isChecked() && updateStatusQoS != 2) {
//                                              statusQoSChanged = true;
//                                              updateStatusQoS = 2;
//                                          }
//                                      }
//                                  });
//
//                                  exitBtn.setOnClickListener(new View.OnClickListener() {
//                                      @Override
//                                      public void onClick(View v) {
//                                          isExitRequested = true;
//                                          onBackPressed();
//                                      }
//                                  });
//
//                                  completeDialog.dismiss();
//                              }
//                          }
//            );
//        } else {
//            context.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    cToast.makeText("Could not extract android devices due to error: Code: " + statusCode, Toast.LENGTH_SHORT);
//                    updateBroadcastingDevices(getCurrentActivity());
//                }
//            });
//        }
//    }
}

