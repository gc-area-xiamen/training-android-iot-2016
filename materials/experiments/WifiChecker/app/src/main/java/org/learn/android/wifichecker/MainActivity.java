package org.learn.android.wifichecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private TextView tvMain;
    private WifiManager mWifiManager;
    public MainActivity() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(intent);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        tvMain = (TextView)findViewById(R.id.txt_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

        @Override
        protected void onResume() {
            super.onResume();
            registerReceiver(mReceiver, mFilter);
        }

        @Override
        protected void onPause() {
            super.onPause();
            unregisterReceiver(mReceiver);
        }

    private void updateTxtMain(String newstr) {
        tvMain.setText(tvMain.getText().toString() + "\n" +newstr);
    }

    private void updateWifiState(int state) {
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            updateTxtMain("Wifi Enabled");
        } else {
            updateTxtMain("Wifi Disabled");
        }
    }
    private void updateAccessPoints() {
        List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }
                updateTxtMain("Scan " + result.SSID);
            }
        }
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + (i >> 24 & 0xFF);
    }
    private void updateConnectionState(DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            return;
        }
        if (state == null) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            updateTxtMain("Wifi RSSI changed to " + info.getRssi());
        } else if (state == DetailedState.OBTAINING_IPADDR) {
            updateTxtMain("IP assigning...");
        } else if (state == DetailedState.CONNECTED) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            updateTxtMain(info.getSSID() + " " + intToIp(info.getIpAddress()));
        } else if (state == DetailedState.DISCONNECTED) {
            updateTxtMain("Disconnected");
        } else if (state == DetailedState.FAILED) {
            updateTxtMain("Failed");
        } else {
            updateTxtMain("Other state " + state);
        }
    }
    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.NETWORK_IDS_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            updateConnectionState(WifiInfo.getDetailedStateOf((SupplicantState)
                    intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            updateConnectionState(((NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO)).getDetailedState());
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        }
    }
}
