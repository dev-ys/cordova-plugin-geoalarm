package com.bb.cordova.geoalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.Manifest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class GeoalarmPlugin extends CordovaPlugin {
    public static final String TAG = "GeoalarmPlugin";

    public static CordovaWebView webView = null;

    private Context context;
    private Action executedAction;
    private SharedPreferences storage;

    private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeoalarmPlugin.webView = webView;
        context = this.cordova.getActivity().getApplicationContext();
        storage = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }

    @Override
    public boolean execute(final String action, final JSONArray args,
                           final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "GeoalarmPlugin execute action: " + action + " args: " + args.toString());
        executedAction = new Action(action, args, callbackContext);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (action.equals("addAlarm")) {

                    for (int i = 0; i < args.length(); i++) {
                        String json = args.optJSONObject(i).toString();
                        addAlarm(json);
                    }

                } else if (action.equals("initialize")) {

                    initialize(callbackContext);

                }
            }
        });

        return true;
    }

    public boolean execute(Action action) throws JSONException {
        return execute(action.action, action.args, action.callbackContext);
    }

    private void addAlarm(String json) {
        Alarm alarm = Alarm.fromJson(json);
        if (alarm != null) {
            int index2 = storage.getInt("index2", 0);
            alarm.register(context, index2);
            storage.edit()
                    .putString(String.format("alarm_%d", index2), json)
                    .putInt("index2", index2 + 1)
                    .apply();
        }
    }

    private void initialize(CallbackContext callbackContext) {

        // remove all alarms

        int index1 = storage.getInt("index1", 0);
        int index2 = storage.getInt("index2", 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        for (int i=index1; i<index2; i++) {
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, i, intent, 0);
            alarmManager.cancel(alarmIntent);
            storage.edit().remove(String.format("alarm_%d", index1));
        }

        if (index2 > 10000) {
            index2 = 0;
        }

        storage.edit()
                .putInt("index1", index2)
                .putInt("index2", index2)
                .apply();


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            String packageName = context.getPackageName();
//            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
//                Intent i = new Intent();
//                i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                i.setData(Uri.parse("package:" + packageName));
//                context.startActivity(i);
//            }
//        }

        // location permission

        String[] permissions = { Manifest.permission.ACCESS_FINE_LOCATION };
        if (!hasPermissions(permissions)) {
            PermissionHelper.requestPermissions(this, 0, permissions);
        } else {
            callbackContext.success();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!PermissionHelper.hasPermission(this, permission)) return false;
        }

        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;

        if (executedAction != null) {
            for (int r:grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    executedAction.callbackContext.sendPluginResult(result);
                    executedAction = null;
                    return;
                }
            }
            Log.d(TAG, "Permission Granted!");
            execute(executedAction);
            executedAction = null;
        }
    }
}
