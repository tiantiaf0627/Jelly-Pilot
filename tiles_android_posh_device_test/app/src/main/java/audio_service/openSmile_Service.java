package audio_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by tiantianfeng on 9/11/17.
 */

public class openSmile_Service extends Service {

    private MediaRecorder mRecorder;

    private Handler mHandler = new Handler();
    private int i = 0;

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 3;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("TILEs", "onStart:Record_Services");

        mHandler.postDelayed(mTickExecutor, 3000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mTickExecutor);
    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent record_Intent = new Intent(getApplicationContext(), openSmile_Service.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, record_Intent, PendingIntent.FLAG_ONE_SHOT);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }

            Intent openSmile_Intent = new Intent(getApplicationContext(), com.audeering.opensmile.androidtemplate.OpenSmile.class);

            startService(openSmile_Intent);
            stopSelf();


        }
    };

}
