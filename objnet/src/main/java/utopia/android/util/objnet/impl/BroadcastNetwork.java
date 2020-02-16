package utopia.android.util.objnet.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import utopia.android.util.objnet.INetwork;
import utopia.android.util.objnet.ObjectNetwork;

public class BroadcastNetwork extends BroadcastReceiver implements INetwork {
    private Context mContext;
    private String mAction;

    @Override
    public void onStartUp(Context context) {
        mContext = context;
        mAction = getActionByPackageName(mContext.getPackageName());
        IntentFilter filter = new IntentFilter();
        filter.addAction(mAction);
        mContext.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null || action.length() <= 0) {
            return;
        }
        if (action.equals(mAction)) {
            String requestData = intent.getStringExtra("data");
            String requestJson = new String(hex2bytes(requestData));
            String responseJson = ObjectNetwork.doResponse(requestJson);
            String responseData = bytes2hex(responseJson.getBytes());
            setResultData(responseData);
        }
    }

    @Override
    public void onShutDown() {
        mContext.unregisterReceiver(this);
    }

    public static String getActionByPackageName(String packageName) {
        return packageName + "/objnet";
    }

    public static String bytes2hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            String str = Integer.toHexString(0xff & b);
            if (str.length() < 2) {
                builder.append(0);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    public static byte[] hex2bytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (0xff & Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16));
        }
        return bytes;
    }
}
