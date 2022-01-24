package com.host900.goproxy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;

import snail007.proxysdk.Proxysdk;

public class MainActivity extends AppCompatActivity {


    String TAG = "HomeFragment";
    ArrayList<String> serviceIDs = new ArrayList<>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText editText = findViewById(R.id.input);
        SharedPreferences config = getSharedPreferences("config", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = config.edit();

        final String args = config.getString("args", "");
        editText.setText(args);
        editText.addTextChangedListener(watcher(editor, editText));
        editText.setHorizontallyScrolling(true);


        TextView tip = findViewById(R.id.tip);
        TextView ipaddrs = findViewById(R.id.ip_addrs);
        String sdkVersion = Proxysdk.version();
        TextView viewManual = findViewById(R.id.view_manual);
        TextView joinQQ = findViewById(R.id.join_qq_group);
        Log.d(TAG, Proxysdk.version());
        ipaddrs.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("ip", ((TextView) view).getText());
                cm.setPrimaryClip(mClipData);
                Toast.makeText(view.getContext(), R.string.ip_copied, Toast.LENGTH_LONG).show();
                return false;
            }
        });
        joinQQ.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("qq", ((TextView) view).getText());
                cm.setPrimaryClip(mClipData);
                Toast.makeText(view.getContext(), R.string.qqcopied, Toast.LENGTH_LONG).show();
                return false;
            }
        });
        //ui
        viewManual.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        viewManual.getPaint().setAntiAlias(true);//抗锯齿

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.apptitle));
        }
        tip.setText(getString(R.string.hint0) + " " + sdkVersion + " " + getString(R.string.hint1));
        ipaddrs.setText(getIpAddress(getBaseContext()));

        joinQQ.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        joinQQ.getPaint().setAntiAlias(true);//抗锯齿

        findViewById(R.id.btn_start).setOnClickListener(start(editText, this));
        findViewById(R.id.btn_stop).setOnClickListener(stop(this));

        viewManual.setOnClickListener(openURL("https://snail007.github.io/goproxy/manual/zh/#/"));
//        joinQQ.setOnClickListener(openURL("https://jq.qq.com/?_wv=1027&k=5G2EwxR"));

        return;
    }

    public View.OnClickListener stop(final Context ctx) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(ctx, true);
            }
        };
    }

    private void stopService(Context ctx, boolean show) {
        for (String serviceID : serviceIDs) {
            Proxysdk.stop(serviceID);
        }
        serviceIDs.clear();
        if (show) {
            Toast.makeText(ctx, R.string.stopdone, Toast.LENGTH_LONG).show();
        }
    }

    public View.OnClickListener start(final EditText editText, final Context ctx) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(ctx, false);
                String[] lines = editText.getText().toString().trim().split("\n");
                boolean isEmpty = true;
                for (int i = 0; i < lines.length; i++) {
                    String args = lines[i];
                    args = args.trim();
                    if (args.isEmpty()) {
                        continue;
                    }
                    isEmpty = false;
                    if (args.indexOf("proxy") == 0 && args.length() >= 5) {
                        args = args.substring(5);
                    }
                    String serviceID = String.format("%f-%d", new Random().nextDouble(), new Random().nextInt());
                    String err = Proxysdk.start(serviceID, args, "");
                    if (!err.isEmpty()) {
                        Toast.makeText(ctx, err, Toast.LENGTH_LONG).show();
                        return;
                    }
                    serviceIDs.add(serviceID);
                }
                if (isEmpty) {
                    Toast.makeText(ctx, R.string.argsisempty, Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(ctx, R.string.startok, Toast.LENGTH_LONG).show();
            }
        };
    }

    public View.OnClickListener openURL(final String u) {

        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse(u);
                intent.setData(content_url);
//                startActivity(Intent.createChooser(intent, "请选择浏览器"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(this, false);
    }

    public TextWatcher watcher(final SharedPreferences.Editor editor, final EditText editText) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                editor.putString("args", editText.getText().toString());
                // Log.d(TAG, editText.getText().toString());
                editor.commit();
            }
        };
    }

    public static String getIpAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            // 3/4g网络
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                //  wifi网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());
                return ipAddress;
            } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
                // 本地网络
                return getLocalIp();
            }
        }
        return null;
    }

    private static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }


    // 获取IP
    private static String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return "0.0.0.0";
    }
}