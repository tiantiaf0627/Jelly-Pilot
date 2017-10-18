package com.example.tiantianfeng.posh_mobile_pilot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.Log;

import audio_service.Record_Service;

/**
 * Created by tiantianfeng on 9/13/17.
 */

public class Alarm_Receive extends BroadcastReceiver {


    protected Context mContext;
    protected Intent mIntent;

    private PendingIntent pendingIntent;

    public static final long ALARM_TRIGGER_AT_TIME = SystemClock.elapsedRealtime();
    public static final long ALARM_INTERVAL = 1000 * 60 * 1;

    private AlarmManager alarmManager;



    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("TILEs", "Received Alarm");
        mContext = context;
        mIntent = intent;

        PackageManager pm = mContext.getPackageManager();
        ComponentName compName = new ComponentName(mContext, Alarm_Receive.class);
        pm.setComponentEnabledSetting(compName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent alarm_Intent = new Intent(mContext, Alarm_Receive.class);

        pendingIntent = PendingIntent.getBroadcast(mContext, 1, alarm_Intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME + ALARM_INTERVAL, pendingIntent);


        Intent record_Intent = new Intent(mContext, Record_Service.class);
        mContext.startService(record_Intent);

    }
}
