package com.example.apiwatcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class MainWatcher extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private HandlerThread thread;
    private NotificationManager manager;

    final String filename = "response";
    final int MAX_RESPONSE_SIZE = 10000;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    public static boolean strcmp(String a, String b, int nbytes){
        System.out.println(a.length() + " " + b.length() + " " + nbytes);
        for (int i = 0; i < nbytes; i++){
            if (a.codePointAt(i) != b.codePointAt(i))
                return false;
        }

        return true;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            try{
                Intent data = (Intent)msg.obj;
                String Url = data.getStringExtra("url");
                String Body = data.getStringExtra("body");
                HashMap<String, String> headers = (HashMap<String, String>)data.getSerializableExtra("headers");
                int duration = data.getIntExtra("duration", 10);
                int method = data.getIntExtra("method", Request.Method.GET);

                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                while (true) {
                    Thread.sleep(duration * 1000);

                    StringRequest req = new StringRequest(method, Url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String respo) {
                            try {
                                FileInputStream fin = getApplicationContext().openFileInput(filename);
                                byte content[] = new byte[MAX_RESPONSE_SIZE];
                                int nbytes = fin.read(content);
                                fin.close();
                                String cmp = new String(content);


                                if (!strcmp(cmp, respo, nbytes)){
                                    FileOutputStream fout = getApplicationContext().openFileOutput(filename, MODE_PRIVATE);
                                    fout.write(respo.getBytes());
                                    fout.close();

                                    Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

                                    Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                                        .setContentTitle("API Watcher")
                                        .setContentText("Change Detected")
                                        .setSmallIcon(R.drawable.ic_launcher_background)
                                        .setContentIntent(pendingIntent)
                                        .setColor(getResources().getColor(R.color.design_default_color_primary_variant))
                                        .build();
                                    manager.notify(2, notification);
                                }else{

                                }

                            } catch (Exception e) {

                            }

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "Error: " + error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            return headers;
                        }

                        @Override
                        public byte[] getBody() throws AuthFailureError {
                            return Body.getBytes();
                        }
                    };

                    req.setShouldCache(false);
                    queue.add(req);


                }


            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }

            stopSelf(msg.arg1);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        Toast.makeText(this, "Service stopped watching", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service started watching", Toast.LENGTH_SHORT).show();
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        serviceHandler.sendMessage(msg);

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("API Watcher")
                .setContentText("API Watcher is running")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);



        return START_STICKY;
    }

    @Override
    public void onCreate() {
        thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}