#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiServer.h>
#include <WiFiUdp.h>

#include <PubSubClient.h>

// WiFi settings
const char* WLAN_SSID = "HUISVANMAX";
const char* WLAN_ACCESS_KEY = "roderikkie";

//client ID
const char* MQTT_CLIENT_ID = "groupb4";
// MQTT broker TI-1.4 data
const char* MQTT_BROKER_URL = "maxwell.bps-software.nl";
const int   MQTT_PORT = 1883;
const char* MQTT_USERNAME = "androidTI";
const char* MQTT_PASSWORD = "&FN+g$$Qhm7j";

// MQTT topics
const char* MQTT_TOPIC_B4_SIMONSAYS = "groupb4/simonsays/esp_to_app";
const char* MQTT_TOPIC_B4_ANDROID = "groupb4/simonsays/app_to_esp";
// Define the Quality of Service
const int   MQTT_QOS = 0;

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

//led and button pins
int yellowButton = 19;
int redButton = 4;
int blueButton = 18;
int greenButton = 5;
int yellowLed = 27;
int redLed = 33;
int blueLed = 26;
int greenLed = 25;

//sequence parameters
int sequenceSize = -1;
int const maxSequenceSize = 5;
int sequence[maxSequenceSize];

//button values
int lastYellowValue = LOW;
int lastRedValue = LOW;
int lastBlueValue = LOW;
int lastGreenValue = LOW;
int yellowValue = 0;
int redValue = 0;
int blueValue = 0;
int greenValue = 0;

//time between button presses
long timeCurrentButtonPressed = 0;
long timePreviousButtonPressed = 0; 
long millisecBetweenButtons = 2000;

//game data
int androidButtonPressed;
int currentIndex;
boolean wonGame = false;
boolean isConnected = false;
boolean androidButtonReceived = false;
boolean isGameRunning = false;

//received messages
const char* CONNECTED_MESSAGE = "CONNECTED";
const char* APP_READY_MESSAGE = "APP_READY";
const char* RED_BUTTON_MESSAGE = "RED_BUTTON_PRESSED";
const char* BLUE_BUTTON_MESSAGE = "BLUE_BUTTON_PRESSED";
const char* GREEN_BUTTON_MESSAGE = "GREEN_BUTTON_PRESSED";
const char* YELLOW_BUTTON_MESSAGE = "YELLOW_BUTTON_PRESSED";
const char* LEAVE_MESSAGE = "LEAVE";

//sent messages
const char* ESP_READY_MESSAGE = "ESP_READY";
const char* WAITING_FOR_INPUT_MESSAGE = "WAITING_FOR_INPUT";
const char* SHOWING_SEQUENCE_MESSAGE = "SHOWING_SEQUENCE";
const char* WAITING_FOR_SEQUENCE_MESSAGE = "WAITING_FOR_SEQUENCE";
const char* CORRECT_MESSAGE = "CORRECT";
const char* WRONG_MESSAGE = "WRONG";
const char* WON_MESSAGE = "WON";

//GameStates
enum States{
IDLE,
CONNECTED,
WAITING_FOR_BUTTON,
WAITING_FOR_ANDROID,
SHOW_SEQUENCE,
ENDGAME
};

States state;

void setup() {
  Serial.begin(9600);

  //Turning on the WiFi connection
  WiFi.mode(WIFI_STA);
  WiFi.begin(WLAN_SSID, WLAN_ACCESS_KEY);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  // Set up the MQTT Client
  mqttClient.setServer(MQTT_BROKER_URL, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);

  // Maak verbinding met de MQTT broker
  if (!mqttClient.connect(MQTT_CLIENT_ID, MQTT_USERNAME, MQTT_PASSWORD)) {
    Serial.println("Failed to connect to MQTT broker");
  } else {
    Serial.println("Connected to MQTT broker");
  }
  // Subscribe to the app topic
  if (!mqttClient.subscribe(MQTT_TOPIC_B4_ANDROID, MQTT_QOS)) {
    Serial.print("Failed to subscribe to topic ");
    Serial.println(MQTT_TOPIC_B4_ANDROID);
  } else {
    Serial.print("Subscribed to topic ");
    Serial.println(MQTT_TOPIC_B4_ANDROID);
  }

  state = IDLE;
  Serial.println("state: IDLE");
  
  pinMode(yellowButton, INPUT);
  pinMode(redButton, INPUT);
  pinMode(blueButton, INPUT);
  pinMode(greenButton, INPUT);

  pinMode(yellowLed, OUTPUT);
  pinMode(redLed, OUTPUT);
  pinMode(blueLed, OUTPUT);
  pinMode(greenLed,OUTPUT);

}

void loop() {
  // put your main code here, to run repeatedly:
  mqttClient.loop();
  if(state == IDLE){
    //accept connection
    if(isConnected){
      state = CONNECTED;
      Serial.println("Connected!");
    }
  }
  
  //while connected, run the game
  if(isConnected){
    if(state == CONNECTED){
      state = WAITING_FOR_BUTTON;
      Serial.println("state: WAITFORBUTTON");
      mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, WAITING_FOR_SEQUENCE_MESSAGE);
    }

  //waits until a button is added to the sequence
  if(state == WAITING_FOR_BUTTON){
    timeCurrentButtonPressed = millis();
    yellowValue = digitalRead(yellowButton);
    redValue = digitalRead(redButton);
    blueValue = digitalRead(blueButton);
    greenValue = digitalRead(greenButton);

  if(timeCurrentButtonPressed - timePreviousButtonPressed > millisecBetweenButtons){
    if(yellowValue == HIGH && lastYellowValue != HIGH){
      Serial.println("Yellow added");
      showLight(yellowLed);
      addToSequence(yellowLed);
      timePreviousButtonPressed = timeCurrentButtonPressed;
      
    }else{
    if(redValue == HIGH && lastYellowValue != HIGH){
      Serial.println("Red added");
      showLight(redLed);
      addToSequence(redLed);
      timePreviousButtonPressed = timeCurrentButtonPressed;
      
    }else{
    if(blueValue == HIGH && lastBlueValue != HIGH){
      Serial.println("Blue added");
      showLight(blueLed);
      addToSequence(blueLed);
      timePreviousButtonPressed = timeCurrentButtonPressed;
      
    }else {
    if(greenValue == HIGH && lastGreenValue != HIGH){
      Serial.println("Green added");
      showLight(greenLed);
      addToSequence(greenLed);
      timePreviousButtonPressed = timeCurrentButtonPressed;
  }}}}
  }
    lastYellowValue = yellowValue;
    lastRedValue = redValue;
    lastBlueValue = blueValue;
    lastGreenValue = greenValue;
  } 

  //shows the sequence
  if(state == SHOW_SEQUENCE){
    showSequence();
    state = WAITING_FOR_ANDROID;
    Serial.println("state: WAITFORANDROID");
    mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, WAITING_FOR_INPUT_MESSAGE);
  }

  if(state == WAITING_FOR_ANDROID){

    if(androidButtonReceived){
      androidButtonReceived = false;
      
      if(androidButtonPressed == sequence[currentIndex]){
        if(currentIndex == sequenceSize){
          if(sequenceSize == maxSequenceSize - 1){
            //The answer is correct and is the last sequence
            state = ENDGAME;
            wonGame = true;
            //CORRECT
            sendCorrect();
         
          }
           else{
            //The answer is correct and a new sequence will play
            currentIndex = 0;
            state = WAITING_FOR_BUTTON;
            Serial.println("state: WAITFORBUTTON");
            //CORRECT
            sendCorrect();
            mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, WAITING_FOR_SEQUENCE_MESSAGE);
           }
          }
        else{
        //The answer is correct and the sequence is not finished
        currentIndex += 1;
        state = WAITING_FOR_ANDROID;
        Serial.println("state: WAITFORANDROID");
        //CORRECT
        sendCorrect();
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, WAITING_FOR_INPUT_MESSAGE);
      }
    }
    else{
      //The answer is incorrect and the game ends
      state = ENDGAME;
      //WRONG
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, WRONG_MESSAGE);
      }
    }
  }

  if(state == ENDGAME){
      Serial.println("state: ENDGAME");
    if(wonGame){
      //send WON
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, WON_MESSAGE);
    }
    Reset();
  }

}
else{
  //reset when connection is lost
  if(state != IDLE){
    Reset();
    }
  }
  
}

//blinks a led
void showLight(int led){
  digitalWrite(led, HIGH);
  delay(1000);
  digitalWrite(led,LOW);
  delay(500);
}

//adds a led to the sequence
void addToSequence(int button){
  state = SHOW_SEQUENCE;
  sequenceSize += 1;
  
  if(sequenceSize > maxSequenceSize){
    sequenceSize = 0;
  }

  sequence[sequenceSize] = button;
}

//shows the led sequence
void showSequence(){
    mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "SHOWING_SEQUENCE");
    Serial.println("Showing Sequence");
    delay(2000);
    
    for(int i = 0; i <= sequenceSize; i++){
      showLight(sequence[i]);
    }
}

//resets the game data
void Reset(){
  clearSequence();
  sequenceSize = -1;
  state = IDLE;
  currentIndex = 0;
  wonGame = false;
  timeCurrentButtonPressed = 0;
  timePreviousButtonPressed = 0;
  androidButtonPressed = -100;
  lastYellowValue = LOW;
  lastRedValue = LOW;
  lastBlueValue = LOW;
  lastGreenValue = LOW;
  Serial.println("state: IDLE");
  isConnected = false;
}

//clears the sequence
void clearSequence(){
  for(int i = 0; i <= maxSequenceSize; i++){
    sequence[i] = -1;
  }
}

//sends a 'correct' message
void sendCorrect(){
  mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, CORRECT_MESSAGE);
}

void handleMqttMessage(String text){
  Serial.println("Message Recieved");
  if(text.equals(APP_READY_MESSAGE)){
    isConnected = true;
  }
  else if(text.equals(LEAVE_MESSAGE)){
    isConnected = false;
  }
  else if(text.equals(RED_BUTTON_MESSAGE)){
    androidButtonPressed = redLed;
    androidButtonReceived = true; 
  }
  else if( text.equals(GREEN_BUTTON_MESSAGE)){
    androidButtonPressed = greenLed;
    androidButtonReceived = true;         
  }
  else if(text.equals(YELLOW_BUTTON_MESSAGE)){
    androidButtonPressed = yellowLed;
    androidButtonReceived = true;         
  }
  else if(text.equals(BLUE_BUTTON_MESSAGE)){
    androidButtonPressed = blueLed;
    androidButtonReceived = true;         
  }
  else if (text.equals(CONNECTED_MESSAGE)){
      mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, ESP_READY_MESSAGE);
  }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  // Logging
  Serial.print("MQTT callback called for topic ");
  Serial.println(topic);
  Serial.print("Payload length ");
  Serial.println(length);
  
  // If received from the right topic
  if (strcmp(topic, MQTT_TOPIC_B4_ANDROID) == 0) {
    // Let op, geen null-terminated string, dus voeg zelf de \0 toe
    char txt[22];
    for (int i = 0; i < 22; i++) { txt[i] = '\0'; }
    strncpy(txt, (const char*) payload, length > 22 ? 22 : length);
    // Laat de tekst zien in log
    Serial.print("Text: ");
    Serial.println(txt);
    String mqttMessage = "";
    for(int x = 0; x < length; x++){
      mqttMessage = mqttMessage + txt[x];
    }
    Serial.println(mqttMessage);
    handleMqttMessage(mqttMessage);
  }
}  
