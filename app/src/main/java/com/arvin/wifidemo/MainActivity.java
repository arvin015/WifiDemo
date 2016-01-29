package com.arvin.wifidemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private Context context;

    private ToggleButton wifiBtn;
    private ListView wifiListView;

    private WifiAdapter wifiAdapter;

    private CustomDialog customDialog;

    private WifiUtil wifiUtil;

    private List<ScanResult> scanResultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = this;

        init();
    }

    private void init() {

        wifiUtil = new WifiUtil(context);
        wifiUtil.setListener(new WifiUtil.IWifiUtilListener() {
            @Override
            public void update() {
                updateWifiData();
            }

            @Override
            public void connecting(String SSID) {
                if (wifiAdapter != null)
                    wifiAdapter.updateConnectState(SSID, 1);
            }

            @Override
            public void connected(String SSID, boolean isConnected) {
                if (wifiAdapter != null)
                    wifiAdapter.updateConnectState(SSID, isConnected ? 2 : 3);
            }
        });

        wifiBtn = (ToggleButton) findViewById(R.id.wifiBtn);
        wifiListView = (ListView) findViewById(R.id.wifiList);

        wifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                final ScanResult scanResult = scanResultList.get(i);
                final WifiConfiguration configuration = wifiUtil.wifiIsConfig(scanResult.SSID);

                if (customDialog == null) {
                    customDialog = new CustomDialog(context);
                }

                customDialog.setListener(new CustomDialog.ICustomDialogListener() {
                    @Override
                    public void link(String pwdStr) {

                        wifiUtil.startTimer(5000);

                        wifiUtil.linkNetwork(scanResult.SSID, pwdStr);
                    }

                    @Override
                    public void disconnet() {
                        if (configuration != null) {
                            wifiUtil.disconnectNetwork(configuration.networkId);
                            updateWifiData();
                        }
                    }
                });

                int mode;

                String SSID = wifiUtil.getLinkedSSID();
                if ((SSID + "").equals(scanResult.SSID)) {
                    mode = CustomDialog.LINKED;
                } else {

                    if (configuration != null) {
                        mode = CustomDialog.SAVED;
                    } else {
                        mode = CustomDialog.NOSAVED;
                    }
                }

                customDialog.showDialog(mode, scanResult.SSID,
                        configuration == null ? null : configuration.allowedKeyManagement.toLongArray(),
                        wifiUtil.getLevel() + "", wifiUtil.getIpAddress() + "");
            }
        });

        wifiBtn.setChecked(wifiUtil.wifiIsOpen());

        if (wifiUtil.wifiIsOpen()) {

            wifiUtil.scanWifi();

            updateWifiData();

            wifiUtil.startTimer(5000);
        }

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (wifiBtn.isChecked()) {
                    wifiUtil.openWifi();

                    updateWifiData();

                    wifiUtil.startTimer(5000);

                } else {
                    wifiUtil.closeWifi();

                    wifiListView.setVisibility(View.GONE);

                    wifiUtil.cancelTimer();
                }
            }
        });
    }

    private void updateWifiData() {

        scanResultList = wifiUtil.getScanResultList();

        if (scanResultList != null) {

            wifiListView.setVisibility(View.VISIBLE);

            if (wifiAdapter == null) {
                wifiAdapter = new WifiAdapter(context);
                wifiListView.setAdapter(wifiAdapter);
            }

            wifiAdapter.setWifiList(scanResultList);
        }

        wifiUtil.getWifiInfo();

        if (wifiUtil.getLinkedSSID() == null) {
            wifiUtil.autoLinkNetwork();
        } else {
            wifiAdapter.updateConnectState(wifiUtil.getLinkedSSID(), 2);
        }
    }

    class WifiAdapter extends BaseAdapter {

        private Context context;
        private List<ScanResult> scanResultList = new ArrayList<>();

        private String currentLinkWifiSSID;

        private LayoutInflater inflater;

        private int connectState = -1;

        public WifiAdapter(Context context) {
            this.context = context;

            inflater = LayoutInflater.from(context);
        }

        public void setWifiList(List<ScanResult> scanResultList) {
            this.scanResultList = scanResultList;
            notifyDataSetChanged();
        }

        public void updateConnectState(String currentLinkWifiSSID, int connectState) {

            this.currentLinkWifiSSID = currentLinkWifiSSID;
            this.connectState = connectState;

            ScanResult result = null;

            if (scanResultList != null &&
                    currentLinkWifiSSID != null) {
                for (ScanResult scanResult : scanResultList) {
                    if (scanResult.SSID.equals(currentLinkWifiSSID)) {
                        result = scanResult;
                        break;
                    }
                }
            }

            if (result != null) {
                scanResultList.remove(result);
                scanResultList.add(0, result);
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return scanResultList.size();
        }

        @Override
        public Object getItem(int i) {
            return scanResultList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            ScanResult scanResult = scanResultList.get(i);

            ViewHolder viewHolder;

            if (view == null) {

                viewHolder = new ViewHolder();

                view = inflater.inflate(R.layout.wifi_item, null);

                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60);
                view.setLayoutParams(params);

                viewHolder.SSID = (TextView) view.findViewById(R.id.wifiSSID);
                viewHolder.level = (TextView) view.findViewById(R.id.wifiLevel);
                viewHolder.linkState = (TextView) view.findViewById(R.id.linkState);

                view.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            if (currentLinkWifiSSID != null) {
                if (scanResult.SSID.equals(currentLinkWifiSSID)) {
                    viewHolder.linkState.setVisibility(View.VISIBLE);

                    if (connectState == 1) {
                        viewHolder.linkState.setText("连接中");
                    } else if (connectState == 2) {
                        viewHolder.linkState.setText("已连接");
                    } else if (connectState == 3) {
                        viewHolder.linkState.setText("");
                    }

                } else {
                    viewHolder.linkState.setVisibility(View.GONE);
                }
            }

            viewHolder.SSID.setText(scanResult.SSID);
            viewHolder.level.setText(scanResult.level + "");

            return view;
        }

        class ViewHolder {
            TextView SSID, linkState, level;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiUtil != null)
            wifiUtil.cancelTimer();
    }
}
