package com.bb.cordova.geoalarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        String json = intent.getStringExtra("json");
        final Alarm alarm = Alarm.fromJson(json);
        if (alarm == null) {
            return;
        }

        // re register

        int id = intent.getIntExtra("id", 0);
        alarm.delayTimeMillis = AlarmManager.INTERVAL_DAY;
        alarm.register(context, id);

        if (!alarm.equalTime(System.currentTimeMillis())) {
            return;
        }

        // get location

        try {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                if (alarm.checkLocaiton(location)) {
                                    Notification notification = new NotificationCompat.Builder(context)
                                            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                                            .setSmallIcon(context.getApplicationInfo().icon)
                                            .setContentTitle(alarm.title)
                                            .setContentText(alarm.text)
                                            .build();
                                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                    mNotificationManager.notify(0, notification);
                                }
                            }
                        }
                    });
        } catch (SecurityException securityException) {

        }
    }
}
