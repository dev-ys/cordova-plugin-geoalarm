package com.bb.cordova.geoalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences storage = context.getSharedPreferences(GeoalarmPlugin.TAG, Context.MODE_PRIVATE);
        int index1 = storage.getInt("index1", 0);
        int index2 = storage.getInt("index2", 0);

        for (int i=index1; i<index2; i++) {
            String json = storage.getString(String.format("alarm_%d", i), null);
            Alarm alarm = Alarm.fromJson(json);
            if (alarm != null) {
                alarm.register(context, i);
            }
        }
    }
}