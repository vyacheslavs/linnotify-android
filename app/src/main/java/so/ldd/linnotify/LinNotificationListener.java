package so.ldd.linnotify;

import static java.util.Collections.singleton;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.intentfilter.androidpermissions.PermissionManager;
import com.intentfilter.androidpermissions.models.DeniedPermission;
import com.intentfilter.androidpermissions.models.DeniedPermissions;

import java.util.Arrays;

public class LinNotificationListener extends NotificationListenerService {

    public LinNotificationListener() {
    }

    @Override
    public void onCreate () {
        Log.d("LinNotificationListener", "STARTED!");
    }

    @Override
    public void onDestroy() { Log.d("LinNotificationListener", "DESTROYED!");
    }

    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        String NOTIFICATION_CHANNEL_ID = "so.ldd.linnotify";
        String channelName = "My Background Service";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = null;
            channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("LinNotify")
                .setContentText("Idle...")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    public static String bundle2string(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        String string = "Bundle{";
        for (String key : bundle.keySet()) {
            string += " " + key + " => " + bundle.get(key) + ";";
        }
        string += " }Bundle";
        return string;
    }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        // Implement what you want here
        Log.d("LinNotificationListener", "new notification " + bundle2string(sbn.getNotification().extras));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        // Implement what you want here
        Log.d("LinNotificationListener", "removed notification " + bundle2string(sbn.getNotification().extras));
    }
}