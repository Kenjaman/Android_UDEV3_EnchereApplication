package com.udev.enchereapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class EnchereEnCoursActivity extends AppCompatActivity {
    private static final Object REQUEST_TAG = new Object();
    private static final String ADRESSE_SERVEUR = "https://adjuge.herokuapp.com/enchere";
    private ImageView imageProduit;
    private TextView description;
    private TextView prixDepart;
    private TextView prixActuel;
    private TextView acheteur;
    private TextView tempsRestant;
    private Button boutonSurenchere;
    private ImageButton boutonUpdate;
    private ProprietesUtilisateur proprietesUtilisateur;
    private RequestQueue requestQueue;
    private static Enchere enchere;
    private Timer timer;
    private Timer timerCompteur;
    private int nbEnchere=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enchere_en_cours);
        imageProduit = findViewById(R.id.imageProduit);
        description = findViewById(R.id.description);
        prixDepart = findViewById(R.id.prix_base);
        prixActuel = findViewById(R.id.prix_encours);
        acheteur = findViewById(R.id.meilleur_enchere);
        tempsRestant = findViewById(R.id.temps_restant);
        boutonSurenchere = findViewById(R.id.bouton_enchere);
        boutonUpdate = findViewById(R.id.bouton_actu);
        requestQueue = Volley.newRequestQueue(this);



//        onNewIntent(this.getIntent()); //Seulement pour le mode SingleTop
    }

    //OnResume et nPause


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        try(FileInputStream input = openFileInput("sauv_prop")){
            Properties pref_user = new Properties();
            pref_user.load(input);
            proprietesUtilisateur = new ProprietesUtilisateur(pref_user);
            chargementEnchere(ADRESSE_SERVEUR);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause(){
        cancelRequest();
        stoptimertask();
        stoptimertaskCompteur();
        enchere.stoptimertask();
        super.onPause();
    }


    // Chargements Enchere et images
    public void chargementEnchere(final String url){
        cancelRequest();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(enchere!=null){// on nettoie l'ancienne enchere si il y en avait une
                            enchere.stoptimertask();
                            enchere = null;
                        }
                        enchere = new Enchere(response);
                        boutonSurenchere.setEnabled(proprietesUtilisateur.peuSurencherir(enchere)); // MAj en temps réel de la disponibilité du bouton de surenchère
                        Log.i("chargementEnchere","chargement de : "+enchere.toString());
                        if(enchere.getTemps_restant()<=0) { // Fin de l'enchère on relance la requète
                            Toast.makeText(description.getContext(),"Enchère suivante",Toast.LENGTH_SHORT).show();
                            chargementEnchere(ADRESSE_SERVEUR);
                            return;
                        }
                        if(url.equals(ADRESSE_SERVEUR)) {//si on charge une nouvelle enchere on charge l'image et on lance le timer de décompte local sinon non
                            chargementImage(enchere.getUrl_image());
                            startTimerMajAffichageCompteur();
                            nbEnchere=0;
                        }
                        affichageEnchere();
                        startTimer();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse!=null){
                    if(error.networkResponse.statusCode == 404) {
                        boutonSurenchere.setEnabled(false);
                        Toast.makeText(description.getContext(), "!!!!! l'enchere n'est plus disponible", Toast.LENGTH_LONG).show();
                    }
                    else if(error.networkResponse.statusCode == 400) {
                        boutonSurenchere.setEnabled(false);
                        Toast.makeText(description.getContext(), "!!!!! l'enchere est terminée", Toast.LENGTH_LONG).show();
                    }
                    chargementEnchere(ADRESSE_SERVEUR);
                }
                timerCompteur.cancel();
                stoptimertask();
                description.setText("Erreur de telechargement ... " +error.getMessage());
            }
        });
        if(enchere==null)
            description.setText("Chargement...");
        request.setTag(REQUEST_TAG);
        requestQueue.add(request);
    }


    public void chargementImage(URL urlImage){
        ImageRequest request = new ImageRequest(urlImage.toString(),new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                imageProduit.setImageBitmap(response);
            }
        }, 0, 0, Bitmap.Config.RGB_565, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                imageProduit.setImageResource(R.drawable.echec);
            }
        });
        imageProduit.setImageResource(R.drawable.chargement);
        request.setTag(REQUEST_TAG);
        request.setShouldCache(true);
        requestQueue.add(request);
    }



    // Gestion des boutons


    public void onClickSurEnchere(View view) {
        cancelRequest();
        nbEnchere++;
        if(nbEnchere==1){
            Intent intent = new Intent(this,EnchereTermineService.class);
            intent.putExtra("produit",enchere.getProduit());
            intent.putExtra("pseudo",proprietesUtilisateur.getPseudo());
            intent.putExtra("tempsRestant",enchere.getTemps_restant());
            intent.putExtra("url_enchere",enchere.getUrl_enchere().toString());
            this.startService(intent);
        }
        JSONObject jsonEnchere = createJsonObject();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, enchere.getUrl_enchere().toString(), jsonEnchere,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        enchere.stoptimertask();
                        enchere=null; //On nettoie l'ancienne enchere avant de charger la nouvelle
                        enchere = new Enchere(response);
                        affichageEnchere();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error.networkResponse!=null){
                    if(error.networkResponse.statusCode == 404)
                        Toast.makeText(description.getContext(),"l'enchere n'est plus disponible",Toast.LENGTH_LONG).show();
                    else if(error.networkResponse.statusCode == 400)
                        Toast.makeText(description.getContext(),"l'enchere est terminée",Toast.LENGTH_LONG).show();
                    chargementEnchere(ADRESSE_SERVEUR);
                }
                description.setText("Erreur de telechargement ... " +error.getMessage());
            }
        });
        if(acheteur.getText().toString().equals(proprietesUtilisateur.getPseudo().toString())) { // Déprécié : grisage du bouton a la place
            Toast.makeText(acheteur.getContext(), "Vous êtes deja le meilleurs encherisseur", Toast.LENGTH_LONG).show();
            return;
        }
        request.setTag(REQUEST_TAG);
        requestQueue.add(request);
    }

    public void onClickSurUpdate(View view){
        this.cancelRequest();
        stoptimertask();
        chargementEnchere(enchere.getUrl_enchere().toString());
    }

    //Gestion des Timers

    /**
     * Demarrage du timer gerant le délais de rechargement automatique des données du serveur
     */
    private void startTimer(){
        stoptimertask();
        timer = new Timer("Thread rechargement données serveur",true);
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Timer","Execution de la tache runInUiThread");
                        String ancienAcheteur = enchere.getAcheteur();
                        chargementEnchere(enchere.getUrl_enchere().toString());
                        if(!enchere.getAcheteur().equals(ancienAcheteur)) {
                            Log.d("Timer","changement d'acheteur");
                            affichageEnchere();
                        }
                    }
                });
            }
        };
        // Quand on se rapproche de la fin le temps de rechargement est accelerer, s'agirait de pas rater la vente
        if(enchere.getTemps_restant()<10)
            timer.schedule(task,1000);
        else
            timer.schedule(task,10000);
    }

    /**
     * Demarrage du timer responsable de la thread de rechargement de l'affichage du compteur
     */
    private void startTimerMajAffichageCompteur(){
        stoptimertaskCompteur();
        timerCompteur = new Timer("timerCompteur",true);
        TimerTask timerTaskEnchere = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("RunnerTaskAffichage compteur","Reaffichage timer ");
                        if(enchere.getTemps_restant()<=0) // On est sur que le timer du compteur s'arrete et se supprime quand on atteint 0 sec
                            timerCompteur.cancel();
                        tempsRestant.setText(Integer.toString(enchere.getTemps_restant())+ (enchere.getTemps_restant()>1?" secondes":"seconde"));
                    }
                });

            }
        };
        timerCompteur.scheduleAtFixedRate(timerTaskEnchere,0,1000);
    }

    private void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer=null;
        }
    }

    private void stoptimertaskCompteur() {
        //stop the timer, if it's not already null
        if (timerCompteur != null) {
            timerCompteur.cancel();
            timerCompteur=null;
        }
    }

    private void affichageEnchere(){
        description.setText(enchere.getProduit());
        prixDepart.setText(Double.toString(enchere.getPrix_base()));
        prixActuel.setText(Double.toString(enchere.getPrix_actuel()));
        acheteur.setText(enchere.getAcheteur());
        tempsRestant.setText(Integer.toString(enchere.getTemps_restant())+ (enchere.getTemps_restant()>1?" secondes":"seconde"));
        boutonSurenchere.setText("Encherir de "+Double.toString(enchere.getMontantEchere(proprietesUtilisateur.getTx_surenchere()))+" €");
        if(!proprietesUtilisateur.peuSurencherir(enchere)){
            boutonSurenchere.setEnabled(false);
            Toast.makeText(this,"Cette enchère est trop chère pour vous",Toast.LENGTH_LONG).show();
        }
        if(enchere.getAcheteur().equals(proprietesUtilisateur.getPseudo()))//Double securité contre la surenchere sur soi même
            boutonSurenchere.setEnabled(false);
    }

    private void cancelRequest(){
        if(this.requestQueue!=null){
            requestQueue.cancelAll(REQUEST_TAG);
        }
    }

    private JSONObject createJsonObject() {
        JSONObject jsonEnchere = new JSONObject();
        try {
            jsonEnchere.put("pseudo", proprietesUtilisateur.getPseudo());
            jsonEnchere.put("prix",Double.parseDouble(prixActuel.getText().toString()) + enchere.getMontantEchere(proprietesUtilisateur.getTx_surenchere()));
            return jsonEnchere;
        }catch (JSONException j){
            Log.e("JSON",j.getMessage(),j);
            return null;
        }

    }

//Seulement pour le mode SingleTop
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//
//    }
}