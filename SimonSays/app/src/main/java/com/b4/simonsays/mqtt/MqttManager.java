package com.b4.simonsays.mqtt;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class MqttManager {
    private static final String LOG_TAG = MqttManager.class.getName();

    private MqttAndroidClient client;

    private MessageListener messageListener;

    private boolean isConnected = false;

    private static class MqttManagerHolder {
        private static MqttManager instance = new MqttManager();
    }

    public static MqttManager getInstance() {
        return MqttManagerHolder.instance;
    }

    private MqttManager() {
    }

    public void connect(Context context) {

        if (this.isConnected) return;

        String clientID = MqttClient.generateClientId();

        client = new MqttAndroidClient(context, MqttSettings.getFullServerAddress(), clientID);
        client.setCallback(new MqttCallBackHandler());

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setWill(MqttSettings.getFullAppTopic(), MqttSettings.APP_LEAVE_MESSAGE.getBytes(), MqttSettings.QOS, false);
        connectOptions.setUserName(MqttSettings.USERNAME);
        connectOptions.setPassword(MqttSettings.PASSWORD.toCharArray());

        try {
            Log.d(LOG_TAG, "Connecting to MQTT server...");
            IMqttToken token = client.connect(connectOptions);
            token.setActionCallback(new MqttConnectActionListener());

        } catch (MqttException e) {
            Log.e(LOG_TAG, String.format("Failed connecting to MQTT server due to: %s", e.getMessage()));
        }
    }

    public void disconnect() {
        if (!this.isConnected) return;

        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToTopic(String topic, @Nullable IMqttActionListener iMqttActionListener) {
        try {
            Log.d(LOG_TAG, String.format("Subscribing to topic \"%s\"...", topic));
            IMqttToken token = client.subscribe(topic, MqttSettings.QOS);
            token.setActionCallback(iMqttActionListener);

        } catch (MqttException e) {
            Log.e(LOG_TAG, String.format("Failed subscribing to topic \"%s\" due to: %s", topic, e.getMessage()));
        }
    }

    public void publishToTopic(String topic, String message) {
        MqttMessage newMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));

        try {
            Log.i(LOG_TAG, String.format("Publishing message \"%s\" to topic \"%s\"", message, topic));
            client.publish(topic, newMessage);

        } catch (MqttException e) {
            Log.e(LOG_TAG, String.format("Failed publishing message \"%s\" to topic \"%s\" due to: %s", message, topic, e.getMessage()));
        }
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    class MqttConnectActionListener implements IMqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(LOG_TAG, "Successfully connected to MQTT server!");

            subscribeToTopic(MqttSettings.getFullEspTopic(), new MqttSubscribeActionListener());
            publishToTopic(MqttSettings.getFullAppTopic(), MqttSettings.CONNECTED_MESSAGE);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e(LOG_TAG, "Failed whilst trying to connect to MQTT server due to: " + exception.getMessage());
        }
    }

    static class MqttSubscribeActionListener implements IMqttActionListener {

        private final String LOG_TAG = this.getClass().getName();

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(LOG_TAG, "Successfully subscribed to topic!");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.e(LOG_TAG, "Failed to subscribe to topic!");
        }
    }

    class MqttCallBackHandler implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            Log.d(LOG_TAG, "Connected to MQTT server at " + serverURI);
            isConnected = true;
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(LOG_TAG, String.format("Connection with MQTT server lost due to: %s", cause.getMessage()));
            isConnected = false;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            Log.d(LOG_TAG, String.format("Message (\"%s\") arrived on topic \"%s\"", topic, message.toString()));

            messageListener.onMessageArrived(message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(LOG_TAG, "Delivery of " + token.toString() + " was successful");
        }
    }
}