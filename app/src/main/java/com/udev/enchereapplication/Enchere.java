package com.udev.enchereapplication;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class Enchere {

    private String produit;
    private URL url_enchere;
    private URL url_image;
    private double prix_base;
    private double prix_actuel;
    private String acheteur;
    private int temps_restant;
    private Timer timerEnchere;

    public Enchere(String produit, URL url_enchere, URL url_image, double prix_base, double prix_actuel, String acheteur, int temps_restant) {
        this.produit = produit;
        this.url_enchere = url_enchere;
        this.url_image = url_image;
        this.prix_base = prix_base;
        this.prix_actuel = prix_actuel;
        this.acheteur = acheteur;
        this.temps_restant = temps_restant;
        this.timerEnchere=new Timer(true);
    }

    public Enchere(JSONObject jsonObject){
        try {
            this.produit=jsonObject.getString("produit");
            this.prix_base=jsonObject.getDouble("prix_base");
            this.prix_actuel=jsonObject.getDouble("prix_actuel");
            this.url_enchere=new URL(jsonObject.getString("url_enchere"));
            this.url_image=new URL(jsonObject.getString("url_image"));
            this.acheteur=jsonObject.getString("acheteur");
            this.temps_restant=jsonObject.getInt("temps_restant");
            this.timerEnchere=new Timer("timerEnchere",true);
            lancerTimerEnchere();
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void lancerTimerEnchere(){
        timerEnchere.schedule(new TimerTask() {
            @Override
            public void run() {
                temps_restant--;
                Log.d(timerEnchere.toString(),"tick "+temps_restant);
                if(temps_restant<=1)
                    timerEnchere.cancel();
            }
        },0,1000);
    }
    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timerEnchere != null) {
            timerEnchere.cancel();
            timerEnchere = null;
        }
    }
    public Timer getTimerEnchere() {
        return timerEnchere;
    }


    public double getMontantEchere(double tx_surenchere){
        return prix_base*tx_surenchere;
    }
    public String getProduit() {
        return produit;
    }

    public void setProduit(String produit) {
        this.produit = produit;
    }

    public URL getUrl_enchere() {
        return url_enchere;
    }

    public void setUrl_enchere(URL url_enchere) {
        this.url_enchere = url_enchere;
    }

    public URL getUrl_image() {
        return url_image;
    }

    public void setUrl_image(URL url_image) {
        this.url_image = url_image;
    }

    public double getPrix_base() {
        return prix_base;
    }

    public void setPrix_base(double prix_base) {
        this.prix_base = prix_base;
    }

    public double getPrix_actuel() {
        return prix_actuel;
    }

    public void setPrix_actuel(double prix_actuel) {
        this.prix_actuel = prix_actuel;
    }

    public String getAcheteur() {
        return acheteur;
    }

    public void setAcheteur(String acheteur) {
        this.acheteur = acheteur;
    }

    public int getTemps_restant() {
        return temps_restant;
    }

    public void setTemps_restant(int temps_restant) {
        this.temps_restant = temps_restant;
    }




}
