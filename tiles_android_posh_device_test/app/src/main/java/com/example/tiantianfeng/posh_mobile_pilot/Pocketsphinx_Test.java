package com.example.tiantianfeng.posh_mobile_pilot;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by tiantianfeng on 9/18/17.
 */

public class Pocketsphinx_Test extends Service implements RecognitionListener {

    private SpeechRecognizer speechRecognizer;
    edu.cmu.pocketsphinx.Feature feature;

     /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "PC";

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("TILEs", "Speech setup");
        runRecognizerSetup();
    }

    private void runRecognizerSetup() {
        new AsyncTask<Void, Void, Exception>() {

            @Override
            protected Exception doInBackground(Void... params) {

                Log.d("TILEs", "Speech Run in background");
                try {
                    Log.d("TILEs", "Speech Run in background");
                    Assets assets = new Assets(Pocketsphinx_Test.this);
                    Log.d("TILEs", "Speech Run in background");
                    File assetDir = assets.syncAssets();
                    Log.d("TILEs", assetDir.toString());
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {

                } else {
                    switchSearch(KWS_SEARCH);
                }
            }

        }.execute();

    }

    private void switchSearch(String searchName) {

        speechRecognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            speechRecognizer.startListening(searchName);
        else
            speechRecognizer.startListening(searchName, 10000);

    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        speechRecognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir)// To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();

        Log.d("TILEs", "Start Listen");
        speechRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        speechRecognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        speechRecognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        speechRecognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        // Create language model search
        File languageModel = new File(assetsDir, "weather.dmp");
        speechRecognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

        // Phonetic search
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        speechRecognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onEndOfSpeech() {

        if (!speechRecognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);

        Log.d("TILEs", "End Speech");

    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Log.d("TILEs", text);

    }


    @Override
    public void onTimeout() {
        Log.d("TILEs", "Timeout");
        switchSearch(KWS_SEARCH);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("TILEs", "Begin Speech");

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(PHONE_SEARCH))
            switchSearch(PHONE_SEARCH);
        else if (text.equals(FORECAST_SEARCH))
            switchSearch(FORECAST_SEARCH);

    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.shutdown();
        }
    }
}
