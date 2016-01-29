package com.arvin.wifidemo;

import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Created by arvin.li on 2016/1/26.
 */
public class CustomDialog {

    private Context context;

    private Dialog dialog;

    private LinearLayout linkedContainer, needLinkContainer;
    private LinearLayout frequencyContainer, securityContainer, ipAddressContainer;
    private ToggleButton showPwdBtn;
    private EditText pwdEdit;
    private Button cancelBtn, linkBtn;
    private TextView SSIDText, securityText, ipAddressText, frequencyText;

    public static final int NOSAVED = 0;
    public static final int SAVED = 1;
    public static final int LINKED = 2;

    private int currentMode;

    public CustomDialog(Context context) {

        this.context = context;

        init();
    }

    private void init() {

        dialog = new Dialog(context, R.style.CustomDialogTheme);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.link_dialog);

        linkedContainer = (LinearLayout) dialog.findViewById(R.id.linkedContainer);
        needLinkContainer = (LinearLayout) dialog.findViewById(R.id.needLinkContainer);
        frequencyContainer = (LinearLayout) dialog.findViewById(R.id.frequencyContainer);
        securityContainer = (LinearLayout) dialog.findViewById(R.id.securityContainer);
        ipAddressContainer = (LinearLayout) dialog.findViewById(R.id.ipAddressContainer);
        showPwdBtn = (ToggleButton) dialog.findViewById(R.id.showPwdBtn);
        pwdEdit = (EditText) dialog.findViewById(R.id.pwdEdit);
        cancelBtn = (Button) dialog.findViewById(R.id.cancelBtn);
        linkBtn = (Button) dialog.findViewById(R.id.linkBtn);
        SSIDText = (TextView) dialog.findViewById(R.id.wifiSSID);
        securityText = (TextView) dialog.findViewById(R.id.security);
        frequencyText = (TextView) dialog.findViewById(R.id.frequency);
        ipAddressText = (TextView) dialog.findViewById(R.id.ipAddress);

        showPwdBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    pwdEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    pwdEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }

                pwdEdit.setSelection(pwdEdit.length());
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDialog();
            }
        });

        linkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                closeDialog();

                if (listener != null) {
                    if (LINKED == currentMode) {
                        listener.disconnet();
                    } else {
                        listener.link(pwdEdit.getText().toString());
                    }
                }
            }
        });
    }

    public void showDialog(int currentMode, String wifiSSID,
                           long[] keyMgmt, String frequency, String ipAddress) {

        this.currentMode = currentMode;
        SSIDText.setText(wifiSSID);

        if (currentMode == NOSAVED) {

            needLinkContainer.setVisibility(View.VISIBLE);
            linkedContainer.setVisibility(View.GONE);

            showPwdBtn.setChecked(false);
            pwdEdit.setText("");

            linkBtn.setText("连接");

        } else if (currentMode == SAVED || currentMode == LINKED) {

            needLinkContainer.setVisibility(View.GONE);
            linkedContainer.setVisibility(View.VISIBLE);

            StringBuffer sb = new StringBuffer();
            if (keyMgmt != null && keyMgmt.length > 0) {

                sb.append(WifiConfiguration.KeyMgmt.strings[((int) keyMgmt[0])]);

                for (int i = 1; i < keyMgmt.length; i++) {
                    sb.append("/" + WifiConfiguration.KeyMgmt.strings[((int) keyMgmt[i])]);
                }
            }

            securityText.setText(sb.toString());

            if (currentMode == SAVED) {
                frequencyContainer.setVisibility(View.GONE);
                ipAddressContainer.setVisibility(View.GONE);
                securityContainer.setVisibility(View.VISIBLE);

                linkBtn.setText("连接");

            } else {
                frequencyContainer.setVisibility(View.VISIBLE);
                ipAddressContainer.setVisibility(View.VISIBLE);
                securityContainer.setVisibility(View.VISIBLE);

                frequencyText.setText(frequency + "");
                ipAddressText.setText(ipAddress + "");

                linkBtn.setText("清除");
            }
        }

        if (dialog != null) {
            dialog.show();
        }
    }

    public void closeDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private ICustomDialogListener listener;

    public void setListener(ICustomDialogListener listener) {
        this.listener = listener;
    }

    interface ICustomDialogListener {
        void link(String pwdStr);

        void disconnet();
    }
}
