package com.example.tiantianfeng.posh_mobile_pilot;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import audio_service.Record_Service;
import battery_service.Battery_Service;
import ble_service.BLE_Advertise_Service;
import ble_service.BLE_Scan_Service;
import ble_service.Bluetooth_Scan;

public class MainActivity extends AppCompatActivity {

    private PendingIntent pendingIntent;

    public static final long ALARM_TRIGGER_AT_TIME = SystemClock.elapsedRealtime() + 10000;

    private AlarmManager alarmManager;

    /* Debug Parameters **/
    private final int ENABLE_BT             = 1;
    private final int ENABLE_DISCOVERABLE   = 2;
    private final int ENABLE_RECORD         = 3;
    private final int ENABLE_POCKET         = 4;
    private final int ENABLE_BLE            = 5;
    private final int ENABLE_DEFAULT        = 99;

    private String DEBUG                = "TILEs";

    private final int MY_PERMISSIONS_REQUEST    = 1;

    private PowerManager.WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission(Manifest.permission.BLUETOOTH);
        requestPermission(Manifest.permission.BATTERY_STATS);
        requestPermission(Manifest.permission.BLUETOOTH_ADMIN);
        requestPermission(Manifest.permission.BLUETOOTH_PRIVILEGED);
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requestPermission(Manifest.permission.SET_ALARM);
        requestPermission(Manifest.permission.WAKE_LOCK);
        requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        isIgnoreBatteryOption(MainActivity.this);

        init();


    }

    private void requestPermission(String permission) {

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission)) {


            } else {

            }

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SET_ALARM},
                    MY_PERMISSIONS_REQUEST);
        } else {

            Log.d("TILES", permission + " granted");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TILES", "Granted");

                } else if (grantResults.length > 0) {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Debug", "Not grant");


                }
                return;
            }
        }
    }

    public static void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivityForResult(intent, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == MY_PERMISSIONS_REQUEST){
                //TODO something
                Log.d("Tiles", "Permitted");
                startRecordingService();
            }
        }else if (resultCode == RESULT_CANCELED){
            if (requestCode == MY_PERMISSIONS_REQUEST){
                Log.d("Tiles", "Not Permitted");
            }
        }
    }

    private void init() {

        androidIDLog();
        enPower();

        int DEBUG_MODE = ENABLE_BLE;

        switch (DEBUG_MODE) {

            case ENABLE_RECORD:
                startRecordingService();
                break;

            case ENABLE_POCKET:
                Intent pocketsphinx_Test = new Intent(this, Pocketsphinx_Test.class);
                startService(pocketsphinx_Test);
                break;

            case ENABLE_BLE:
                Intent bleAdvertiseService  = new Intent(this, BLE_Advertise_Service.class);
                Intent bleScanService       = new Intent(this, BLE_Scan_Service.class);
                Intent batteryService       = new Intent(this, Battery_Service.class);

                startService(bleAdvertiseService);
                startService(bleScanService);
                startService(batteryService);

                break;

            case ENABLE_BT:
                startBTService();
                break;

            case ENABLE_DISCOVERABLE:
                discoverableBT();
                break;

            default:
                break;
        }

    }

    private void androidIDLog() {
        String data = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(DEBUG, data);

        String s = new String(data.getBytes());
        Log.d(DEBUG, s);
    }

    private void enPower() {

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyWakelockTag");
        wakeLock.acquire();

        Log.d("TILES", Integer.toString(Build.VERSION.SDK_INT));

    }

    private void startRecordingService() {
        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(getApplicationContext().ALARM_SERVICE);

        Intent alarm_Intent = new Intent(getApplicationContext(), Alarm_Receive.class);

        Intent record_Intent = new Intent(getApplicationContext(), Record_Service.class);
        pendingIntent = PendingIntent.getService(MainActivity.this, 1, record_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);
        }
        else {
            if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);
            }
        }


    }

    private void startBTService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent bt_Intent = new Intent(this, Bluetooth_Scan.class);
        pendingIntent = PendingIntent.getService(MainActivity.this, 1, bt_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    private void discoverableBT() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alarmManager.cancel(pendingIntent);
        wakeLock.release();
    }
}
