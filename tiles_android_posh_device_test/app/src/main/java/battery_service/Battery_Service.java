package battery_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by tiantianfeng on 10/13/17.
 */

public class Battery_Service extends Service {

    private String DEBUG_FILE   = "TILEs_File";

    private final int CSV_DATA_ENTRY_SIZE = 2;

    private String batteryLevel;
    private String batteryTimestamp;
    private Handler mHandler = new Handler();

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 5;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getBatteryLevel()
    {
        Intent intent  = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        Log.d("TILEs", String.valueOf(percent) + "%");

        return String.valueOf(percent) + "%";
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("TILEs", "onStart:Battery_Services");

        batteryLevel = getBatteryLevel();

        mHandler.postDelayed(mTickExecutor, 10000);
        
    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            saveDataToCSV();

            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent battery_Intent = new Intent(getApplicationContext(), Battery_Service.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, battery_Intent, PendingIntent.FLAG_ONE_SHOT);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }

            stopSelf();


        }
    };

    private void saveDataToCSV() {

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/Battery_Log.csv";

        Calendar calendar = Calendar.getInstance();
        batteryTimestamp = Integer.toString(calendar.get(Calendar.MONTH) + 1) + "-" + Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))
                + "-" + Integer.toString(calendar.get(Calendar.YEAR))   + "-" + Integer.toString(calendar.get(Calendar.HOUR))
                + "-" + Integer.toString(calendar.get(Calendar.MINUTE)) + "-" + Integer.toString(calendar.get(Calendar.SECOND));


        try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

            String [] data = new String[CSV_DATA_ENTRY_SIZE];

            data[0] = batteryTimestamp;
            data[1] = batteryLevel;

            writer.writeNext(data);

            writer.close();

        } catch (IOException e) {

        }

        try (CSVReader reader = new CSVReader(new FileReader(filepath), ',');) {

            List<String[]> rows = reader.readAll();

            for (String[] row: rows) {

                Log.d(DEBUG_FILE, Arrays.toString(row));

            }
        } catch (IOException e) {

        }

    }

}
