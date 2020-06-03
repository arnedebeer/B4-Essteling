package com.b4.simonsays;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.b4.simonsays.mqtt.MessageListener;
import com.b4.simonsays.mqtt.MqttManager;
import com.b4.simonsays.mqtt.MqttSettings;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class GameActivity extends AppCompatActivity implements MessageListener {

    private final String LOG_TAG = this.getClass().getName();

    private MqttManager mqttManager;

    private enum GameStates {
        START,
        SHOWING_SEQUENCE,
        WON,
        WAITING_FOR_INPUT,
        WAITING_FOR_RESPONSE,
        WAITING_FOR_SEQUENCE
    }

    private GameStates gameState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Button redButton = findViewById(R.id.redButton);
        Button yellowButton = findViewById(R.id.yellowButton);
        Button greenButton = findViewById(R.id.greenButton);
        Button blueButton = findViewById(R.id.blueButton);

        redButton.setOnClickListener(e -> buttonPressed(MqttSettings.RED_BUTTON_PRESSED_MESSAGE));
        yellowButton.setOnClickListener(e -> buttonPressed(MqttSettings.YELLOW_BUTTON_PRESSED_MESSAGE));
        greenButton.setOnClickListener(e -> buttonPressed(MqttSettings.GREEN_BUTTON_PRESSED_MESSAGE));
        blueButton.setOnClickListener(e -> buttonPressed(MqttSettings.BLUE_BUTTON_PRESSED_MESSAGE));

        gameState = GameStates.START;

        mqttManager = MqttManager.getInstance();
        mqttManager.setMessageListener(this);
    }

    private void buttonPressed(String message) {
        if (gameState.equals(GameStates.WAITING_FOR_INPUT)) {
            this.mqttManager.publishToTopic(MqttSettings.getFullAppTopic(), message);
            this.gameState = GameStates.WAITING_FOR_RESPONSE;
        }
    }

    @Override
    public void onMessageArrived(MqttMessage message) {

        switch (message.toString()) {
            case MqttSettings.SHOWING_SEQUENCE_MESSAGE:
//            case MqttSettings.WAITING_FOR_INPUT_MESSAGE:
            case MqttSettings.WAITING_FOR_SEQUENCE_MESSAGE:
                gameState = GameStates.valueOf(message.toString());
                showGameState();
                break;

            case MqttSettings.CORRECT_MESSAGE:
                gameState = GameStates.WAITING_FOR_INPUT;
                break;

            case MqttSettings.WON_MESSAGE:
            case MqttSettings.WRONG_MESSAGE:
                showEndDialog();
                break;
        }
    }

    private void showGameState() {
        Toast.makeText(this, "Current gamestate: " + this.gameState, Toast.LENGTH_SHORT).show();
    }

    private void showEndDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        View view = View.inflate(this, R.layout.dialog_game_end, null);
        builder.setView(view);

        view.findViewById(R.id.button_confirm).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        builder.create().show();
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
            MainActivity.showHelpDialog(this);
        }

        return super.onOptionsItemSelected(item);
    }
}