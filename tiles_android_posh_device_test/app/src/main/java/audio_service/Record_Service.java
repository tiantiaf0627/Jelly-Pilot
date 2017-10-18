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

public class Record_Service extends Service implements AudioProcessor {

    private MediaRecorder mRecorder;

    private Handler mHandler = new Handler();
    private int i = 0;
    private File mOutputFile;
    private boolean isRecording = false;

    /*
    *   Pitch Detection
    * */
    static final boolean PITCH_DETECTOR_DEBUG_ENABLE = true;

    private double pitch;
    private PitchDetector pitchDetector;


    /*
    *   Sound Detection
    * */
    static final boolean SOUND_DETECTOR_DEBUG_ENABLE = true;
    private boolean isSoundDetected = false;

    private AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(16000, 1024, 0);
    private double threshold = SilenceDetector.DEFAULT_SILENCE_THRESHOLD;
    private SilenceDetector silenceDetector;

    private int SOUND_DETECTION_DURATION = 15000;
    private Handler soundCountDownHandler = new Handler();

    private final String SOUND_DETECTED = "SOUND_DETECTED";
    private final String SOUND_UNDETECTED = "SOUND_UNDETECTED";

    private Thread pitchThread, soundThread;

    private String batteryLevel;

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

        batteryLevel = getBatteryLevel();


        if(SOUND_DETECTOR_DEBUG_ENABLE) {

            silenceDetector = new SilenceDetector(threshold, false);

            dispatcher.addAudioProcessor(silenceDetector);
            dispatcher.addAudioProcessor(this);

            soundThread = new Thread(dispatcher, "Sound Thread");
            soundThread.start();

        }

        if(PITCH_DETECTOR_DEBUG_ENABLE)
        {
            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {

                @Override
                public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent e){
                    if(pitchDetectionResult.isPitched()){
                        Log.d("TILEs", "Pitch Detected: " + pitchDetectionResult.getPitch());
                        pitch = pitchDetectionResult.getPitch();
                    }
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            pitchThread = new Thread(dispatcher, "Pitch Thread");
            pitchThread.start();

        }

        soundCountDownHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                dispatcher.stop();
                soundThread.interrupt();
                pitchThread.interrupt();

                startRecording();
                isRecording = true;


            }
        }, SOUND_DETECTION_DURATION);

    }

    public void processPitch(float pitchInHz) {

    }

    @Override
    public boolean process(AudioEvent audioEvent) {

        if(silenceDetector.currentSPL() > threshold) {
            Log.d("TILEs", "Sound Detected: " + (int) silenceDetector.currentSPL() + " db SPL\n");
            isSoundDetected = true;
        }

        return true;
    }

    private void handleSound(){
        if(silenceDetector.currentSPL() > threshold){

        }

    }

    @Override
    public void processingFinished() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    private void startRecording() {

        mRecorder = new MediaRecorder();
        mRecorder.reset();

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            mRecorder.setAudioEncodingBitRate(48000);
        } else {
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioEncodingBitRate(64000);
        }

        mRecorder.setAudioSamplingRate(8000);
        mOutputFile = getOutputFile();
        mRecorder.setOutputFile(mOutputFile.getAbsolutePath());

        try {
            mRecorder.prepare();
            mRecorder.start();
            mHandler.postDelayed(mTickExecutor, 30000);

            Log.d("TILEs","Started recording to "+ mOutputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("TILEs", "prepare() failed " + e.getMessage());
        }

    }

    protected void stopRecording(boolean saveFile) {

        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;

        mHandler.removeCallbacks(mTickExecutor);

        if (!saveFile && mOutputFile != null) {
            mOutputFile.delete();
        }

        Log.i("TILEs", "File Recorded");


    }

    private File getOutputFile() {

        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, "TILEs");

        Calendar calendar = Calendar.getInstance();

        String filename = Integer.toString(calendar.get(Calendar.MONTH) + 1) + "-" + Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))
                + "-" + Integer.toString(calendar.get(Calendar.YEAR))   + "-" + Integer.toString(calendar.get(Calendar.HOUR))
                + "-" + Integer.toString(calendar.get(Calendar.MINUTE)) + "-" + Integer.toString(calendar.get(Calendar.SECOND))
                + "-" + Integer.toString((int)pitch) + "-" + batteryLevel + "-";

        if (isSoundDetected) {
            filename += SOUND_DETECTED;
        } else {
            filename += SOUND_UNDETECTED;
        }

        if (!file.exists()) {
            file.mkdirs();
            Log.i("TILEs", "TILEs Folder Made");
        }

        return new File(file.getAbsolutePath().toString() + "/" + filename + ".wav");
    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            stopRecording(true);
            isRecording = false;


            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent record_Intent = new Intent(getApplicationContext(), Record_Service.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, record_Intent, PendingIntent.FLAG_ONE_SHOT);
            //alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }

            stopSelf();


        }
    };

    public String getBatteryLevel()
    {
        Intent intent  = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        Log.d("TILEs", String.valueOf(percent) + "%");
        return String.valueOf(percent) + "%";
    }

}
