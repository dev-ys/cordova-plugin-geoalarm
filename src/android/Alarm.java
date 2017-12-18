package com.bb.cordova.geoalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;

import com.google.gson.annotations.Expose;

import java.util.Calendar;

public class Alarm {
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public float radius;
    @Expose public String time;
    @Expose public String title;
    @Expose public String text;

    public long delayTimeMillis = 0;

    public int getHour() {
        String timeInfo[] = time.split(":");
        return Integer.parseInt(timeInfo[0]);
    }

    public int getMinute() {
        String timeInfo[] = time.split(":");
        return Integer.parseInt(timeInfo[1]);
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public static Alarm fromJson(String json) {
        if (json == null) return null;
        return Gson.get().fromJson(json, Alarm.class);
    }

    public void register(Context context, int id) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, getHour());
        calendar.set(Calendar.MINUTE, getMinute());
        calendar.set(Calendar.SECOND, 0);
        long triggerAtMillis = calendar.getTimeInMillis() + delayTimeMillis;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("json", toJson());
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, id, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
        }
    }

    public boolean checkLocaiton(Location location) {
        Location center = new Location("alarm");
        center.setLatitude(latitude);
        center.setLongitude(longitude);
        float d = center.distanceTo(location);
        return center.distanceTo(location) < radius;
    }

    public boolean equalTime(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return getHour() == calendar.get(Calendar.HOUR_OF_DAY) && getMinute() == calendar.get(Calendar.MINUTE);
    }
}
