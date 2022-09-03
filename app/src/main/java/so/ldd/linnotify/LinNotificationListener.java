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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.intentfilter.androidpermissions.PermissionManager;
import com.intentfilter.androidpermissions.models.DeniedPermission;
import com.intentfilter.androidpermissions.models.DeniedPermissions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LinNotificationListener extends NotificationListenerService {

    static final String ACTION_START = "so.ldd.linnotify.LinNotificationListener.ACTION_START";
    static final int SERvICE_ID = 1024;

    private String desktopHost = "";
    private int desktopPort = 7777;

    public LinNotificationListener() {
    }

    @Override
    public void onCreate () {
    }

    @Override
    public void onDestroy() { Log.d("LinNotificationListener", "DESTROYED!");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {

        String action = intent.getAction();
        Log.d("LinNotificationListener", "ACTION: " + action);
        if (Objects.equals(action, ACTION_START)) {
            Intent notificationIntent = new Intent(this, MainActivity.class);

            String NOTIFICATION_CHANNEL_ID = "so.ldd.linnotify";
            String channelName = "LinNotify Service";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel;
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

            startForeground(SERvICE_ID, notification);
        }
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
        StringBuilder string = new StringBuilder("{\n");
        for (String key : bundle.keySet()) {
            string.append(" ").append(key).append(" => ").append(bundle.get(key)).append(";\n");
        }
        string.append(" }\n");
        return string.toString();
    }

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification(StatusBarNotification sbn, String url, boolean isRemoval) {
        try {
            Bundle extras = sbn.getNotification().extras;

            Drawable icon = LinNotifyApplication.getInstance().getPackageManager().getApplicationIcon(sbn.getPackageName());
            Bitmap bm = drawableToBitmap(icon);

            ByteArrayOutputStream bmStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, bmStream);
            byte[] bytes = bmStream.toByteArray();

            String jsonString = new JSONObject()
                    .put("title", extras.get(Notification.EXTRA_TITLE) == null ? null : extras.get(Notification.EXTRA_TITLE).toString())
                    .put("text", extras.get(Notification.EXTRA_TEXT) == null ? null : extras.get(Notification.EXTRA_TEXT).toString())
                    .put("id", sbn.getId())
                    .put("removal", isRemoval)
                    .put("icon", Base64.getEncoder().encodeToString(bytes))
                    .toString();

            Log.d("LinNotificationListener", "sending POST: " + jsonString);
            post(url, jsonString);
        } catch (JSONException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String hostname = prefs.getString("hostname", "");
        String portStr = prefs.getString("port", "");

        int port = Integer.parseInt(Objects.equals(portStr, "") ? "7777" : portStr);

        Log.d("LinNotificationListener", "new notification " + bundle2string(sbn.getNotification().extras));
        Log.d("LinNotificationListener", "new notification will be posted to " + hostname + ":" + port);
        Log.d("LinNotificationListener", "notification_id = " + sbn.getId());
        Log.d("LinNotificationListener", "notification_key = " + sbn.getKey());

        if (hostname.length()>0) {
            sendNotification(sbn, "http://" + hostname + ":" + port+"/notify", false);
        }
    }

    void post(String url, String json) {
        Thread thread = new Thread(() -> {
            try  {
                //Your code goes here
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                client.newCall(request).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String hostname = prefs.getString("hostname", "");
        String portStr = prefs.getString("port", "");
        int port = Integer.parseInt(Objects.equals(portStr, "") ? "7777" : portStr);

        Log.d("LinNotificationListener", "removed notification " + bundle2string(sbn.getNotification().extras));
        Log.d("LinNotificationListener", "new notification will be posted to " + hostname + ":" + port);

        if (hostname.length()>0) {
            sendNotification(sbn, "http://" + hostname + ":" + port+"/notify", true);
        }
    }
}