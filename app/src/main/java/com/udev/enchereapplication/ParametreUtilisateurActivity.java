package com.udev.enchereapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public class ParametreUtilisateurActivity extends AppCompatActivity {

    public static final String PROPRIETE_USER = "sauv_prop";
    private EditText pseudoEditText;
    private SeekBar nivEnchereSeekBar;
    private Switch plafondSwitch;
    private RadioGroup radioGroup;
    private TextView tView;
    private CheckBox cgv;
    private ProprietesUtilisateur proprietesUtilisateur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parametre_user);
        pseudoEditText = findViewById(R.id.pseudo);
        nivEnchereSeekBar = findViewById(R.id.niveau_enchere);
        plafondSwitch = findViewById(R.id.tooglePlafond);
        radioGroup = findViewById(R.id.plafond);
        cgv=findViewById(R.id.acceptation_conditions);
        radioGroup.setVisibility(plafondSwitch.isChecked()?View.VISIBLE:View.GONE);
        plafondSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    radioGroup.setVisibility(View.VISIBLE);
                } else {
                    radioGroup.setVisibility(View.GONE);
//                    radioGroup.clearCheck();
                }
            }

        });

        tView = findViewById(R.id.percent);
        tView.setText(String.format(Locale.FRANCE,"%d%%", nivEnchereSeekBar.getProgress()));
        nivEnchereSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int percentage = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                percentage = progress;
                tView.setText(String.format(Locale.FRANCE,"%d%%", percentage));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //write custom code to on start progress
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                tView.setText(String.format(Locale.FRANCE,"%d%%", percentage));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        Properties pref_a_charger = new Properties();
        loadProperties(pref_a_charger);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onPause() {
//        Properties propriete_a_sauver = saveProperties();
//        propriete_a_sauver.list(System.out);
        super.onPause();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void loadProperties(Properties donnee_user) {
        if(donnee_user!=null) {
            try (FileInputStream inputParam = openFileInput(PROPRIETE_USER)) {
                donnee_user.load(inputParam);
                pseudoEditText.setText(donnee_user.getProperty("pseudo"));
                nivEnchereSeekBar.setProgress(Integer.parseInt(donnee_user.getProperty("niv_enchere", "0")));
                plafondSwitch.setChecked(Boolean.parseBoolean(donnee_user.getProperty("plafond_actif", "false")));
                cgv.setChecked(Boolean.parseBoolean(donnee_user.getProperty("cgv", "false")));
                if (plafondSwitch.isChecked() && donnee_user.getProperty("id_plafond") != null) {
                    radioGroup.setVisibility(View.VISIBLE);
                    radioGroup.check(Integer.parseInt(donnee_user.getProperty("id_plafond")));
                }
                proprietesUtilisateur = new ProprietesUtilisateur(donnee_user);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void handleOk(View view) {
        String pseudo = pseudoEditText.getText().toString();
        if(pseudo.trim().isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un pseudo", Toast.LENGTH_SHORT).show();
        }else if(plafondSwitch.isChecked() && getValeurRadio(radioGroup.getCheckedRadioButtonId()).equals("0")){
            Toast.makeText(this,"Veuillez choisir un plafond d'enchère max",Toast.LENGTH_SHORT).show();
        }else if(!cgv.isChecked()){
            Toast.makeText(this,"Veuillez accepter les condition général d'utilisation pour continuer",Toast.LENGTH_SHORT).show();
        }
        else{
            Properties donnee_user = saveProperties();
            donnee_user.list(System.out);
            proprietesUtilisateur = new ProprietesUtilisateur(donnee_user);
            Intent intent = new Intent(view.getContext(), EnchereEnCoursActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Properties saveProperties() {
        Properties donnee_user = new Properties();
        donnee_user.setProperty("pseudo",pseudoEditText.getText().toString());
        donnee_user.setProperty("niv_enchere", String.valueOf(nivEnchereSeekBar.getProgress()));
        donnee_user.setProperty("plafond_actif", String.valueOf(plafondSwitch.isChecked()));
        if(plafondSwitch.isChecked()) {
            donnee_user.setProperty("id_plafond", String.valueOf(radioGroup.getCheckedRadioButtonId()));
            donnee_user.setProperty("plafond_multiplicateur", getValeurRadio(radioGroup.getCheckedRadioButtonId()));
        }
        donnee_user.setProperty("cgv", String.valueOf(cgv.isChecked()));
        try(FileOutputStream fichierSauv = openFileOutput(PROPRIETE_USER,MODE_PRIVATE)){
            donnee_user.store(fichierSauv,"fichier de sauvegarde des parametres utilisateurs");
            Toast.makeText(this,"Les préférences on bien été enregistrées",Toast.LENGTH_SHORT).show();
            return donnee_user;
        }catch (IOException e) {
            Log.e("Main_Activity", Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }

    private String getValeurRadio(int checkedRadioButtonId) {
        switch (checkedRadioButtonId){
            case R.id.x2:
                return "2";
            case R.id.x3:
                return "3";
            case R.id.x4:
                return "4";
            case R.id.x5:
                return "5";
            default:
                return "0";
        }
    }

    public void handleAnnuler(View view) {
        this.finish();
    }
}