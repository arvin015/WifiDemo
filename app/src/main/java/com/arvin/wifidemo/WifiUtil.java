package com.arvin.wifidemo;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by arvin.li on 2016/1/26.
 */
public class WifiUtil {
    private Context context;

    public WifiManager wifiManager;

    private String linkedSSID;

    private String ipAddress;

    private String level;

    public WifiUtil(Context context) {
        this.context = context;

        init();
    }

    private void init() {

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (wifiIsOpen()) {
            if (wifiManager.getWifiState() > WifiManager.WIFI_STATE_ENABLING) {
                getWifiInfo();
            } else {
                autoLinkNetwork();
            }
        }
    }

    /**
     * WIFI是否打开
     *
     * @return
     */
    public boolean wifiIsOpen() {
        return wifiManager.isWifiEnabled();
    }

    /**
     * 打开WIFI
     */
    public void openWifi() {

        if (!wifiIsOpen()) {
            wifiManager.setWifiEnabled(true);
        }

        scanWifi();
    }

    /**
     * 关闭WIFI
     */
    public void closeWifi() {

        if (wifiIsOpen()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    /**
     * 扫描WIFI
     */
    public void scanWifi() {
        wifiManager.startScan();
    }

    /**
     * 获取WIFI列表
     *
     * @return
     */
    public List<ScanResult> getScanResultList() {
        return wifiManager.getScanResults();
    }

    /**
     * 自动连接网络
     */
    public void autoLinkNetwork() {

        List<WifiConfiguration> configurationList = getWifiConfigurationList();

        if (configurationList != null &&
                configurationList.size() > 0) {
            linkNetwork(subString(configurationList.get(0).SSID),
                    configurationList.get(0).networkId);
        }
    }

    /**
     * 获取连接WIFI信息
     *
     * @return
     */
    public void getWifiInfo() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        linkedSSID = subString(wifiInfo.getSSID());
        ipAddress = subString(wifiInfo.getMacAddress());
        level = wifiInfo.getLinkSpeed() + "Mbps";
    }

    /**
     * 连接网络
     *
     * @param SSID
     * @param pwdStr
     */
    public void linkNetwork(String SSID, String pwdStr) {

        WifiConfiguration configuration = wifiIsConfig(SSID);
        if (configuration == null) {

            configuration = new WifiConfiguration();
            configuration.SSID = "\"" + SSID + "\"";
            configuration.preSharedKey = "\"" + pwdStr + "\"";
            configuration.hiddenSSID = false;
            configuration.status = WifiConfiguration.Status.ENABLED;
            configuration.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            configuration.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            configuration.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
        }

        int networkId = wifiManager.addNetwork(configuration);
        linkNetwork(SSID, networkId);
    }

    /**
     * 连接网络
     *
     * @param networkId
     */
    public void linkNetwork(final String SSID, final int networkId) {

        if (listener != null)
            listener.connecting(SSID);

        wifiManager.enableNetwork(networkId, true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                boolean isConnected = false;

                getWifiInfo();
                if (getLinkedSSID() != null) {
                    isConnected = true;
                } else {
                    wifiManager.removeNetwork(networkId);
                }

                if (listener != null)
                    listener.connected(SSID, isConnected);
            }
        }, 1500);
    }

    /**
     * 当前WIFI是否配置
     *
     * @param SSID
     * @return
     */
    public WifiConfiguration wifiIsConfig(String SSID) {

        List<WifiConfiguration> configurationList = getWifiConfigurationList();

        if (configurationList != null) {
            for (WifiConfiguration configuration : configurationList) {
                if (subString(configuration.SSID).equals(SSID)) {
                    return configuration;
                }
            }
        }

        return null;
    }

    /**
     * 断开网络连接
     *
     * @param networdId
     */
    public void disconnectNetwork(int networdId) {
        wifiManager.disableNetwork(networdId);
        wifiManager.disconnect();
        wifiManager.removeNetwork(networdId);
    }

    public String getLinkedSSID() {
        return linkedSSID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getLevel() {
        return level;
    }

    /**
     * 获取当前WIFI列表中所有已保存配置的WIFI
     *
     * @return
     */
    public List<WifiConfiguration> getWifiConfigurationList() {

        List<WifiConfiguration> result = new ArrayList<>();

        List<WifiConfiguration> allWifiConfig = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : allWifiConfig) {
            for (ScanResult scanResult : getScanResultList()) {
                if (scanResult.SSID.equals(subString(configuration.SSID))) {
                    result.add(configuration);
                    break;
                }
            }
        }

        return result;
    }

    public String subString(String str) {
        return (str == null || str == "") ? null : str.substring(1, str.length() - 1);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 200) {
                if (listener != null) {
                    listener.update();
                }
            }
        }
    };

    private Timer timer;

    public void startTimer(int duration) {

        cancelTimer();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(200);
            }
        }, 0, duration);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private IWifiUtilListener listener;

    public void setListener(IWifiUtilListener listener) {
        this.listener = listener;
    }

    public interface IWifiUtilListener {
        void update();

        void connecting(String SSID);

        void connected(String SSID, boolean isConnected);
    }
}
