package com.b4.simonsays.mqtt;

public class MqttSettings {
    private final String LOG_TAG = this.getClass().getName();

    // General MQTT settings
    private static final String SERVER_ADDRESS = "maxwell.bps-software.nl";
    private static final String PORT = "1883";

    static final String USERNAME = "androidTI";
    static final String PASSWORD = "&FN+g$$Qhm7j";

    static final int QOS = 2;

    // MQTT topic settings
    private static final String BASE_TOPIC = "groupb4/simonsays";
    private static final String APP_TOPIC = "app_update";
    private static final String ESP_TOPIC = "esp_update";

    // Possible messages that are expected to be sent
    public static final String RED_BUTTON_PRESSED_MESSAGE = "RED_BUTTON_PRESSED";
    public static final String YELLOW_BUTTON_PRESSED_MESSAGE = "YELLOW_BUTTON_PRESSED";
    public static final String GREEN_BUTTON_PRESSED_MESSAGE = "GREEN_BUTTON_PRESSED";
    public static final String BLUE_BUTTON_PRESSED_MESSAGE = "BLUE_BUTTON_PRESSED";

    public static final String READY_MESSAGE = "READY";
    public static final String LEAVE_MESSAGE = "LEAVE";

    // Possible messages that are expected to be received
    public static final String SHOWING_SEQUENCE_MESSAGE = "SHOWING_SEQUENCE";
    public static final String CORRECT_MESSAGE = "CORRECT";
    public static final String WRONG_MESSAGE = "WRONG";
    public static final String WON_MESSAGE = "WON";
    public static final String WAITING_FOR_INPUT_MESSAGE = "WAITING_FOR_INPUT";
    public static final String WAITING_FOR_SEQUENCE_MESSAGE = "WAITING_FOR_SEQUENCE";

    static String getFullServerAddress() {
        return String.format("tcp://%s:%s", SERVER_ADDRESS, PORT);
    }

    public static String getFullAppTopic() {
        return String.format("%s/%s", BASE_TOPIC, APP_TOPIC);
    }

    static String getFullEspTopic() {
        return String.format("%s/%s", BASE_TOPIC, ESP_TOPIC);
    }
}