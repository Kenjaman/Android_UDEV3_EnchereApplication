package com.udev.enchereapplication;

import android.text.BoringLayout;

import java.util.Properties;

public class ProprietesUtilisateur {
    private String pseudo;
    private double tx_surenchere;
    private boolean plafond_actif = true;
    private int multip_max;

    public ProprietesUtilisateur(Properties properties){
        this.pseudo = properties.getProperty("pseudo");
        this.tx_surenchere = Double.parseDouble(properties.getProperty("niv_enchere"))/100;
        this.plafond_actif = Boolean.parseBoolean(properties.getProperty("plafond_actif"));
        this.multip_max = Integer.parseInt(properties.getProperty("plafond_multiplicateur"));
    }

    public boolean peuSurencherir(Enchere enchere){
        if(plafond_actif){
            if(this.tx_surenchere*enchere.getPrix_base()>this.multip_max*enchere.getPrix_base())
                return false;
            else
                return true;
        }else
            return true;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public double getTx_surenchere() {
        return tx_surenchere;
    }

    public void setTx_surenchere(double tx_surenchere) {
        this.tx_surenchere = tx_surenchere;
    }

    public boolean isPlafond_actif() {
        return plafond_actif;
    }

    public void setPlafond_actif(boolean plafond_actif) {
        this.plafond_actif = plafond_actif;
    }

    public int getMultip_max() {
        return multip_max;
    }

    public void setMultip_max(int multip_max) {
        this.multip_max = multip_max;
    }
}
