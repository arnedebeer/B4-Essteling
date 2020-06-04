#include <WiFi.h>
#include <WiFiClient.h>
#include <WiFiServer.h>
#include <WiFiUdp.h>


#include <PubSubClient.h>

// Zelf instellen voor je eigen WLAN
const char* WLAN_SSID = "HUISVANMAX";
const char* WLAN_ACCESS_KEY = "roderikkie";

// CLIENT_ID moet uniek zijn, dus zelf aanpassen (willekeurige letters en cijfers)
const char* MQTT_CLIENT_ID = "groupb4";
// Gegevens van de MQTT broker die we in TI-1.4 kunnen gebruiken
const char* MQTT_BROKER_URL = "maxwell.bps-software.nl";
const int   MQTT_PORT = 1883;
const char* MQTT_USERNAME = "androidTI";
const char* MQTT_PASSWORD = "&FN+g$$Qhm7j";

// Definieer de MQTT topics
const char* MQTT_TOPIC_B4_SIMONSAYS = "groupb4/simonsays/esp_to_app";
const char* MQTT_TOPIC_B4_ANDROID = "groupb4/simonsays/app_to_esp";
// Definieer de te gebruiken Quality of Service (QoS)
const int   MQTT_QOS = 0;

WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);
  
  int yellowButton = 19;
  int redButton = 4;
  int blueButton = 18;
  int greenButton = 5;
  int yellow = 27;
  int red = 33;
  int blue = 26;
  int green = 25;
  int sequenceSize = -1;
  int const maxSequenceSize = 2;
  int sequence[maxSequenceSize];

  int lastYellowValue = LOW;
  int lastRedValue = LOW;
  int lastBlueValue = LOW;
  int lastGreenValue = LOW;

  int yellowValue = 0;
  int redValue = 0;
  int blueValue = 0;
  int greenValue = 0;
  long timeCurrent = 0;
  long timePrevious = 0; 


  int androidButtonPressed;
  int currentIndex;
  boolean wonGame = false;
  boolean isConnected = false;
  boolean androidButtonReceived = false;
  boolean isGameRunning = false;


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
  // put your setup code here, to run once:

  //Turning on the WiFi connection
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
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
  
  Serial.println("Code is gestart");
  
  Serial.begin(9600);

  state = IDLE;
  Serial.println("state: IDLE");
  
  pinMode(yellowButton, INPUT);
  pinMode(redButton, INPUT);
  pinMode(blueButton, INPUT);
  pinMode(greenButton, INPUT);

  pinMode(yellow, OUTPUT);
  pinMode(red, OUTPUT);
  pinMode(blue, OUTPUT);
  pinMode(green,OUTPUT);

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
if(isConnected){
  if(state == CONNECTED){
    
    //Establish connection
    state = WAITING_FOR_BUTTON;
          Serial.println("state: WAITFORBUTTON");
      mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS,"WAITING_FOR_SEQUENCE");
    }
  
  if(state == WAITING_FOR_BUTTON){
    timeCurrent = millis();
    yellowValue = digitalRead(yellowButton);
    redValue = digitalRead(redButton);
    blueValue = digitalRead(blueButton);
    greenValue = digitalRead(greenButton);

 
  if(timeCurrent - timePrevious > 2000){
    if(yellowValue == HIGH && lastYellowValue != HIGH){
      Serial.println("Yellow added");
      digitalWrite(yellow, HIGH);
      delay(1000);
      digitalWrite(yellow, LOW);
      delay(500);

      addToSequence(yellow);
      timePrevious = timeCurrent;
      
    }else{
    if(redValue == HIGH && lastYellowValue != HIGH){
      Serial.println("Red added");

      digitalWrite(red, HIGH);
      delay(1000);
      digitalWrite(red, LOW);
      delay(500);

      addToSequence(red);
      timePrevious = timeCurrent;
      
    }else{
    if(blueValue == HIGH && lastBlueValue != HIGH){

      Serial.println("Blue added");
      digitalWrite(blue, HIGH);
      delay(1000);
      digitalWrite(blue, LOW);
      delay(500);

      addToSequence(blue);
      timePrevious = timeCurrent;

    }else{
    if(greenValue == HIGH && lastGreenValue != HIGH){

      Serial.println("Green added");
      digitalWrite(green, HIGH);
      delay(1000);
      digitalWrite(green, LOW);
      delay(500);

      addToSequence(green);
    
      timePrevious = timeCurrent;



  }}}}
}

    lastYellowValue = yellowValue;
    lastRedValue = redValue;
    lastBlueValue = blueValue;
    lastGreenValue = greenValue;
}

  if(state == SHOW_SEQUENCE){
    Lights();
    state = WAITING_FOR_ANDROID;
        Serial.println("state: WAITFORANDROID");
    mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "WAITING_FOR_INPUT");

}

  if(state == WAITING_FOR_ANDROID){

    if(androidButtonReceived){
      androidButtonReceived = false;
    if(androidButtonPressed == sequence[currentIndex]){
      if(currentIndex == sequenceSize){
        if(sequenceSize == maxSequenceSize -1){
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
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS,"WAITING_FOR_SEQUENCE");
        }
      }
      else{
        //The answer is correct and the sequence is not finished
        currentIndex += 1;
        state = WAITING_FOR_ANDROID;
            Serial.println("state: WAITFORANDROID");
        //CORRECT
        sendCorrect();
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "WAITING_FOR_INPUT");
      }
    }
    else{
      //The answer is incorrect and the game ends
      state = ENDGAME;
      //WRONG
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "WRONG");
      }
  }
  }

  if(state == ENDGAME){
      Serial.println("state: ENDGAME");
    if(wonGame){
      //send WON
        mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "WON");
    }
    else{
      //send LOST
    }
    Reset();
  }

}
else{
    Reset();
  }
  
}

void addToSequence(int button){
  state = SHOW_SEQUENCE;
    sequenceSize += 1;
    if(sequenceSize > maxSequenceSize){
      sequenceSize = 0;
    }
    sequence[sequenceSize] = button;
  }

  void Lights(){
    mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "SHOWING_SEQUENCE");
    Serial.println("Showing Sequence");
    delay(2000);
    for(int i = 0; i <= sequenceSize; i++){
      digitalWrite(sequence[i], HIGH);
      delay(1000);
      digitalWrite(sequence[i], LOW);
      delay(100);
    }
  }

  void Reset(){
    clearSequence();
    sequenceSize = -1;
    state = IDLE;
    currentIndex = 0;
    wonGame = false;
    timeCurrent = 0;
    timePrevious = 0;
    androidButtonPressed = -100;
    lastYellowValue = LOW;
    lastRedValue = LOW;
    lastBlueValue = LOW;
    lastGreenValue = LOW;
    Serial.println("state: IDLE");
    isConnected = false;
   }

   void clearSequence(){
    for(int i = 0; i <= maxSequenceSize; i++){
      sequence[i] = -1;
    }
  }

  void sendCorrect(){
    mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "CORRECT");
  }

    void handleMqttMessage(String text){
      Serial.println("Message Recieved");
      if(text.equals("APP_READY")){
        isConnected = true;
      }
      else if(text.equals("LEAVE")){
        isConnected = false;
      }
      else if(text.equals("RED_BUTTON_PRESSED")){
        androidButtonPressed = red;
        androidButtonReceived = true; 
      }
      else if( text.equals("GREEN_BUTTON_PRESSED")){
        androidButtonPressed = green;
        androidButtonReceived = true;         
      }
      else if(text.equals("YELLOW_BUTTON_PRESSED")){
        androidButtonPressed = yellow;
        androidButtonReceived = true;         
      }
      else if(text.equals("BLUE_BUTTON_PRESSED")){
        androidButtonPressed = blue;
        androidButtonReceived = true;         
      }
      else if (text.equals("CONNECTED")){
          mqttClient.publish(MQTT_TOPIC_B4_SIMONSAYS, "ESP_READY");
      }
    
  }

  void mqttCallback(char* topic, byte* payload, unsigned int length) {
  // Logging
  Serial.print("MQTT callback called for topic ");
  Serial.println(topic);
  Serial.print("Payload length ");
  Serial.println(length);
  
  // Kijk welk topic is ontvangen en handel daarnaar
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

  
