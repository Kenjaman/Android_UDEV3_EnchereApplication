package com.udev.enchereapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnchereTermineService extends Service {

    private static final Object REQUEST_TAG = new Object();
    private static final String TAG = "ServiceFinEnchere";
    private static final String CHANNEL_ID = "EnchereApp_channel";

    private ExecutorService executorService;

    private String pseudo;
    private String url_enchere_service;
    private int temps_restant_service;
    private String produit;
    private Context ctx;

    private RequestQueue requestQueue;
//    private Timer timerAvantAppel;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"Lancement du service");

        this.requestQueue = Volley.newRequestQueue(this.getApplicationContext());
        this.executorService = Executors.newFixedThreadPool(1);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Appel à onStartCommand");
        pseudo = intent.getStringExtra("pseudo");
        temps_restant_service = intent.getIntExtra("tempsRestant",0);
        url_enchere_service = intent.getStringExtra("url_enchere");
        produit = intent.getStringExtra("produit");
        ctx = this;
        this.executorService.execute(new EnchereTermineAppel(url_enchere_service,temps_restant_service, startId));
//        this.chargementInfos(url_enchere_service);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "Destruction du service");
        this.executorService.shutdown();
    }

    private void chargementInfos(String url){
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            createNotificationChannel();
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContentTitle("Enchère terminée");
                            if(response.getString("acheteur").equals(pseudo)){
                                builder.setContentText("Vous avez remporté l'enchere de "+produit+" pour un total de "+response.getInt("prix_actuel")+"€")
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText("Vous avez remporté l'enchere de "+produit+" pour un total de "+response.getInt("prix_actuel")+"€"));
//                                Toast.makeText(ctx,"Vous avez remporté l'enchere de "+produit+" pour un total de "+response.getInt("prix_actuel")+"€",Toast.LENGTH_LONG).show();
                            }else {
                                builder.setContentText("L'enchere de " + produit + " a été remportée par " + response.getString("acheteur") + " pour un total de " + response.getInt("prix_actuel") + "€")
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText("L'enchere de " + produit + " a été remportée par " + response.getString("acheteur") + " pour un total de " + response.getInt("prix_actuel") + "€"));//                                Toast.makeText(ctx, "L'enchere de " + produit + " a été remportée par " + response.getString("acheteur") + " pour un total de " + response.getInt("prix_actuel") + "€", Toast.LENGTH_LONG).show();
                            }
                            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                            NotificationManagerCompat notifManger = NotificationManagerCompat.from(ctx);
                            notifManger.notify(3,builder.build());
                            Log.d(TAG,notifManger.getNotificationChannel(CHANNEL_ID)+" devrais s'afficheeeeeeeeeeeeeeeer");
                            executorService.shutdown();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse!=null){
                    if(error.networkResponse.statusCode == 404) {
                        Log.e(TAG,"Erreur 404");
                    }
                    else if(error.networkResponse.statusCode == 400) {
                        Log.e(TAG,"Erreur 400");
                    }
                }
            }
        });

        request.setTag(REQUEST_TAG);
        requestQueue.add(request);

    }

    private class EnchereTermineAppel implements Runnable {

        private String url_enchere;
        private int temps_restant;
        private int startId;

        public EnchereTermineAppel(String url_enchere, int temps_restant, int startId) {
            this.url_enchere = url_enchere;
            this.temps_restant = temps_restant;
            this.startId = startId;
        }

        @Override
        public void run() {
            try {
                Log.i("Runnable","Run lancer on attend :"+temps_restant +" secondes");
                Thread.sleep(temps_restant*1000);
                Log.i("Runnable","Tuuuuut fini d'attendre");
                chargementInfos(url_enchere);
            } catch (InterruptedException e) {
                Log.i("Runnable","Runnable interromput");
            } finally {
                stopSelf(this.startId);
            }
        }
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "EnchereApp";
            String description = "Notification d'EnchereApplication";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
