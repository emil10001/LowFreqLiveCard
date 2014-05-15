package com.feigdev.demo.app;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import com.google.android.glass.timeline.LiveCard;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by ejf3 on 5/11/14.
 */
public class LowFreqLiveCardService extends Service {
    private static final String TAG = "LowFreqLiveCardService";

    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView;

    private final Handler mHandler = new Handler();

    private static final Random random;

    static {
        Random tmpRandom;
        try {
            tmpRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            tmpRandom = new Random();
        }
        random = tmpRandom;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (mLiveCard == null) {

            // Get an instance of a live card
            mLiveCard = new LiveCard(this, TAG);

            // Inflate a layout into a remote view
            mLiveCardView = new RemoteViews(getPackageName(),
                    R.layout.hearty_layout);

            // Set up initial RemoteViews values
            mLiveCardView.setTextViewText(R.id.heart_rate, "?");

            Intent menuIntent = new Intent(this, MainActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(
                    this, 0, menuIntent, 0));

            mLiveCard.setViews(mLiveCardView);

            // Publish the live card
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);

            Log.d(TAG, "mLiveCard.publish " + mLiveCard.isPublished());

        } else if (!mLiveCard.isPublished()) {
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        }

        new CardRefresher().execute();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mLiveCard != null && mLiveCard.isPublished()) {
            //Stop the handler from queuing more Runnable jobs

            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

    private void notifyHeartRate(int heartRate) {
        // Set up initial RemoteViews values
        mLiveCardView.setTextViewText(R.id.heart_rate, String.valueOf(heartRate));
        mLiveCard.setViews(mLiveCardView);
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    private class CardRefresher extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            for (int i=0; i < 10; i++){
                if (null == mLiveCard)
                    break;

                notifyHeartRate(random.nextInt(40) + 50);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "couldn't sleep");
                }
            }
            return null;
        }
    }

}
