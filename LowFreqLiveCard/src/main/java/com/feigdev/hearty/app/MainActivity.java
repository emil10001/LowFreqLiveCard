package com.feigdev.hearty.app;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.preview.support.wearable.notifications.RemoteInput;
import android.preview.support.wearable.notifications.WearableNotifications;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;
import com.feigdev.ble.lib.BleHeartService;
import com.feigdev.ble.lib.BleUtils;
import com.feigdev.ble.lib.HeartRate;
import com.feigdev.reusableandroidutils.platform.PlatformUtils;
import com.feigdev.witness.Reporter;
import com.feigdev.witness.Witness;

public class MainActivity extends Activity implements Reporter {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 23484;
    private static final int WEAR_REQUEST = 239;
    private Handler handler = new Handler();
    private TextView tv;
    public static final String EXTRA_REPLY = "reply";
    private static final String ACTION_RESPONSE = "com.feigdev.hearty.app.REPLY";
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PlatformUtils.isGlass()) {
            Log.d(TAG, "running on Glass");
            stopService(new Intent(this, HeartyLiveCardService.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);
            tv = (TextView) findViewById(R.id.name);

            BluetoothAdapter bleAdapter = BleUtils.getAdapter(this);
            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            if (bleAdapter == null || !bleAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    processResponse(intent);
                }
            };
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PlatformUtils.isGlass())
            return;

        Witness.register(BluetoothDevice.class, this);
        Witness.register(HeartRate.class, this);

        registerReceiver(mReceiver, new IntentFilter(ACTION_RESPONSE));
        startService(new Intent(this, BleHeartService.class));
    }

    @Override
    public void onPause() {
        if (PlatformUtils.isGlass())
            return;

        Witness.remove(BluetoothDevice.class, this);
        Witness.remove(HeartRate.class, this);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_ENABLE_BT == requestCode) {
            handleBleEnableResult(resultCode, data);
        } else if (WEAR_REQUEST == requestCode) {
            processResponse(data);
        }
    }

    private void handleBleEnableResult(int resultCode, Intent data) {
        Log.d(TAG, "handleBleEnableResult: " + resultCode);
        if (Activity.RESULT_OK == resultCode) {
            if (null == data.getExtras() || null == data.getExtras().keySet())
                return;

            for (String extra : data.getExtras().keySet()) {
                Log.d(TAG, extra + ": " + data.getExtras().get(extra));
            }
        }
    }

    private void updateText(String name) {
        Log.d(MainActivity.class.toString(), "updateText " + name);
        tv.setText(name);
    }

    // for Android Wear
    private void notifyHeartRate(HeartRate heartRate) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(Integer.toString(heartRate.getHeartRate()))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        Intent intent = new Intent(ACTION_RESPONSE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, WEAR_REQUEST, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pendingIntent);
        Notification notification = new WearableNotifications.Builder(builder)
                .setMinPriority()
                .addRemoteInputForContentIntent(
                        new RemoteInput.Builder(EXTRA_REPLY)
                                .setLabel(getString(R.string.reply)).build()
                )
                .build();
        NotificationManagerCompat.from(this).notify(0, notification);

    }

    @Override
    public void notifyEvent(final Object o) {
        if (o instanceof BluetoothDevice) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BluetoothDevice device = ((BluetoothDevice) o);
                    Log.d(MainActivity.class.toString(), "found device " + device.getName());
                    updateText(device.getName());
                }
            });
        } else if (o instanceof HeartRate) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    HeartRate heartRate = ((HeartRate) o);
                    Log.d(MainActivity.class.toString(), "heart rate " + heartRate.getHeartRate());
                    notifyHeartRate(heartRate);
                }
            });
        }

    }

    private void processResponse(Intent intent) {
        String text = intent.getStringExtra(EXTRA_REPLY);
        if (text != null && !text.equals("")) {
            Log.d(MainActivity.class.toString(), "response was " + text);
        }
    }


}
