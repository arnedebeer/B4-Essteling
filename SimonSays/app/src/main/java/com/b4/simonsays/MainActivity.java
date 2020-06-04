package com.b4.simonsays;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.b4.simonsays.mqtt.MqttManager;

public class MainActivity extends AppCompatActivity {


    private final String LOG_TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


//        MqttManager.getInstance().connect(this.getApplicationContext());

       /* Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);*/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_help){
            showHelpDialog(this);
        }

        return super.onOptionsItemSelected(item);
    }

    public static void showHelpDialog(Context context) {
        AlertDialog.Builder helpDialog = new AlertDialog.Builder(context);
        helpDialog.setTitle(R.string.dialog_help_title);
        helpDialog.setMessage(R.string.dialog_help_message);
        helpDialog.setPositiveButton(R.string.dialog_help_confirm, (dialog, which) -> dialog.dismiss());
        helpDialog.create().show();
    }
}