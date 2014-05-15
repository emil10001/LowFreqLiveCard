package com.feigdev.hearty.app;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import com.feigdev.ble.lib.BleHeartService;
import com.feigdev.ble.lib.HeartRate;
import com.feigdev.witness.Reporter;
import com.feigdev.witness.Witness;
import com.google.android.glass.timeline.LiveCard;

/**
 * Created by ejf3 on 5/11/14.
 */
public class HeartyLiveCardService extends Service implements Reporter {
    private static final String TAG = "HeartyLiveCardService";

    private LiveCard mLiveCard;
    private RemoteViews mLiveCardView;

    private final Handler mHandler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        Witness.register(BluetoothDevice.class, this);
        Witness.register(HeartRate.class, this);

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

        startService(new Intent(this, BleHeartService.class));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Witness.remove(BluetoothDevice.class, this);
        Witness.remove(HeartRate.class, this);

        Log.d(TAG, "onDestroy");

        if (mLiveCard != null && mLiveCard.isPublished()) {
            //Stop the handler from queuing more Runnable jobs

            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

    @Override
    public void notifyEvent(final Object o) {
        if (o instanceof HeartRate) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    HeartRate heartRate = ((HeartRate) o);
                    Log.d(TAG, "heart rate " + heartRate.getHeartRate());
                    notifyHeartRate(heartRate);
                }
            });
        }
    }

    private void notifyHeartRate(HeartRate heartRate) {
        // Set up initial RemoteViews values
        mLiveCardView.setTextViewText(R.id.heart_rate, String.valueOf(heartRate.getHeartRate()));
        mLiveCard.setViews(mLiveCardView);
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }


}
