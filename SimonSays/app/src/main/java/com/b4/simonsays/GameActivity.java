package com.b4.simonsays;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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

    private boolean isEnded = false;

    private int score = 0;

    private TextView scoreTextView;
    private TextView gameStateTextView;

    private ImageButton redButton;
    private ImageButton yellowButton;
    private ImageButton greenButton;
    private ImageButton blueButton;

    private enum GameStates {
        SHOWING_SEQUENCE,
        WAITING_FOR_INPUT,
        WAITING_FOR_RESPONSE,
        WAITING_FOR_SEQUENCE,
        GAME_END
    }

    private GameStates gameState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        this.redButton = findViewById(R.id.redButton);
        this.yellowButton = findViewById(R.id.yellowButton);
        this.greenButton = findViewById(R.id.greenButton);
        this.blueButton = findViewById(R.id.blueButton);

        this.redButton.setOnClickListener(e -> buttonPressed(MqttSettings.RED_BUTTON_PRESSED_MESSAGE));
        this.yellowButton.setOnClickListener(e -> buttonPressed(MqttSettings.YELLOW_BUTTON_PRESSED_MESSAGE));
        this.greenButton.setOnClickListener(e -> buttonPressed(MqttSettings.GREEN_BUTTON_PRESSED_MESSAGE));
        this.blueButton.setOnClickListener(e -> buttonPressed(MqttSettings.BLUE_BUTTON_PRESSED_MESSAGE));

        this.mqttManager = MqttManager.getInstance();
        this.mqttManager.setMessageListener(this);

        this.gameStateTextView = findViewById(R.id.game_state_textView);

        if (savedInstanceState != null) {
            this.score = savedInstanceState.getInt("score");
            this.gameState = GameStates.valueOf(savedInstanceState.getString("gameState"));
            updateGameStateText();

            if (savedInstanceState.getBoolean("isEnded")) {
                showEndDialog();
            }
        } else {
            this.updateButtons(false);
            this.gameState = GameStates.WAITING_FOR_RESPONSE;

            // Notify the ESP the app is ready
            this.mqttManager.publishToTopic(MqttSettings.getFullAppTopic(), MqttSettings.APP_READY_MESSAGE);
        }

        this.scoreTextView = findViewById(R.id.score);
        this.scoreTextView.setText(getString(R.string.your_score, this.score));
    }

    private void buttonPressed(String message) {
        if (this.gameState.equals(GameStates.WAITING_FOR_INPUT)) {
            this.mqttManager.publishToTopic(MqttSettings.getFullAppTopic(), message);
            this.gameState = GameStates.WAITING_FOR_RESPONSE;
            updateGameStateText();
        }
    }

    @Override
    public void onMessageArrived(MqttMessage message) {

        switch (message.toString()) {
            case MqttSettings.SHOWING_SEQUENCE_MESSAGE:
            case MqttSettings.WAITING_FOR_SEQUENCE_MESSAGE:
            case MqttSettings.WAITING_FOR_INPUT_MESSAGE:
                this.gameState = GameStates.valueOf(message.toString());
                break;

            case MqttSettings.CORRECT_MESSAGE:
                addPoint();
                break;

            case MqttSettings.WON_MESSAGE:
            case MqttSettings.WRONG_MESSAGE:
                this.gameState = GameStates.GAME_END;
                showEndDialog();
                break;
        }

        updateButtons(this.gameState == GameStates.WAITING_FOR_INPUT);
        updateGameStateText();
    }

    private void updateButtons(boolean enable) {
        Log.d(LOG_TAG, String.format("Buttons %s!", enable ? "enabled" : "disabled"));

        this.redButton.setEnabled(enable);
        this.yellowButton.setEnabled(enable);
        this.greenButton.setEnabled(enable);
        this.blueButton.setEnabled(enable);
    }

    private void updateGameStateText() {
        Log.d(LOG_TAG, "Current gamestate: " + this.gameState);

        String gameStateText = "";
        switch (this.gameState) {
            case SHOWING_SEQUENCE:
                gameStateText = getString(R.string.showing_sequence_state);
                break;
            case WAITING_FOR_SEQUENCE:
                gameStateText = getString(R.string.waiting_for_sequence_state);
                break;
            case WAITING_FOR_INPUT:
                gameStateText = getString(R.string.waiting_for_input_state);
                break;
            case WAITING_FOR_RESPONSE:
                gameStateText = getString(R.string.waiting_for_response_state);
                break;
        }

        this.gameStateTextView.setText(gameStateText);
    }

    private void addPoint() {
        this.score++;
        this.scoreTextView.setText(getString(R.string.your_score, this.score));

        Log.d(LOG_TAG, "Added one point to the score! The new score is " + this.score);
    }

    private void showEndDialog() {
        this.isEnded = true;
        Log.d(LOG_TAG, "Game ended! Showing end dialog");

        View view = View.inflate(this, R.layout.dialog_game_end, null);

        TextView endScoreTextView = view.findViewById(R.id.end_score);
        endScoreTextView.setText(getString(R.string.your_score, this.score));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
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

        if (id == R.id.action_help) {
            MainActivity.showHelpDialog(this);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putInt("score", this.score);
        savedInstanceState.putString("gameState", this.gameState.toString());
        savedInstanceState.putBoolean("isEnded", this.isEnded);
    }
}