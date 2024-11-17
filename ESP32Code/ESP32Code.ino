/**
 * Description: Parses and outputs data to and from the java application to acctuate the electrical components of the drilling rig
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 * =================================================================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>

#define CHARGE_PIN 26
#define BLEED_PIN 27
#define RELEASE_PIN_OUT 14
#define RELEASE_BALL_OUT 12
#define BACKSIDE_PRESSURE 35
#define BOTTLE_PRESSURE 34

#define PIN_IN_STATUS 32
#define PIN_OUT_STATUS 33
#define BALL_HOME_STATUS 22
#define BALL_LAUNCHED_STATUS 23
#define WIPER_STATUS 25

#define BATTERY_LEVEL 13


// WiFi Definitions
const char* ssid = "CMT Core";
const char* password = "Core1957";

// the server that the esp produces
WiFiServer server(80);

// the url that is sent is what carries the information
String header;

// an identifier to know which message is being sent to the arduino
int messageNumber;
int prevIncomingMessageNumber;

int delayTime = 10; // time in milliseconds before the loop section of code runs again
double secondsToWait = 3.0; // time in SECONDS after no new messages have been recieved from the andriod application
                       // before the arduino will return voltages to their default settings


// voltage calibrations
int pointsToReadOver = 4095;
double refVoltage = 2.7;
double voltInputBackside = 0.0;
double voltInputBottle = 0.0;
double voltInputBattery = 0.0;

void setup() {

  messageNumber = 0;
  prevIncomingMessageNumber = 0;

  Serial.begin(115200);

  // set-up for all of the different pins that are to be used //TODO add more pins
  pinMode(CHARGE_PIN, OUTPUT);
  digitalWrite(CHARGE_PIN, LOW);
  pinMode(BLEED_PIN, OUTPUT);
  digitalWrite(BLEED_PIN, LOW);
  pinMode(RELEASE_PIN_OUT, OUTPUT);
  digitalWrite(RELEASE_PIN_OUT, LOW);
  pinMode(RELEASE_BALL_OUT, OUTPUT);
  digitalWrite(RELEASE_BALL_OUT, LOW);
  pinMode(BACKSIDE_PRESSURE, INPUT);
  pinMode(BOTTLE_PRESSURE, INPUT);

  
  pinMode(PIN_IN_STATUS, INPUT_PULLUP);
  pinMode(PIN_OUT_STATUS, INPUT_PULLUP);
  pinMode(BALL_HOME_STATUS, INPUT_PULLUP);
  pinMode(BALL_LAUNCHED_STATUS, INPUT_PULLUP);
  pinMode(WIPER_STATUS, INPUT_PULLUP);

  pinMode(BATTERY_LEVEL, INPUT);

  // Create my own wifi signal
  Serial.print("Setting AP (Access Point)…");
  WiFi.softAP(ssid, password);
  IPAddress IP = WiFi.softAPIP();
  // the IP Address will always be a default 192.168.4.1
  Serial.print("AP IP address: ");
  Serial.println(IP);

  server.begin();
}

int getIncomingMessageNumber(){
  char *headerCopy = new char[16];

  strcpy(headerCopy, header.substring(0,15).c_str());
  
  int number, garbage;
  sscanf(headerCopy, "GET /%d'%s", &number, &garbage);

  delete[] headerCopy;
  return number;
}

boolean lineIs(String text){
  return header.indexOf(text) >= 0;
}



// this function returns new in the case there is a new message to be processed, false otherwise
bool newMessageCheck(){
  messageNumber = getIncomingMessageNumber();
  // this is the first message being sent by the andriod application, reset the previousMessageNumber back to 0
  if(messageNumber == 1){
      prevIncomingMessageNumber = 0;
  }

  // this is any other *NEW* message being sent by the android application, return true and update the previous message number
  if (prevIncomingMessageNumber != messageNumber){
    prevIncomingMessageNumber = messageNumber;
    return true;
  }
  // this is when the message being sent by the andriod application is not new
  return false;
}

void voltagesToDefault(){
  digitalWrite(CHARGE_PIN, LOW);
  digitalWrite(BLEED_PIN, LOW);
}

void processMessageRecieved(WiFiClient client) {
  // if the message recieved by the client has not changed over the last message, do not process it, set the charge and bleed pins to their default setting (off)
  if (!newMessageCheck()){
    voltagesToDefault();
    return;
  }

  // recieve messages given by the andriod application
  if (lineIs("builtin-true")) {
    digitalWrite(BUILTIN_LED, HIGH);

  }if (lineIs("builtin-false")) {
    digitalWrite(BUILTIN_LED, LOW);

  }if(lineIs("charge-true")) {
    digitalWrite(CHARGE_PIN, HIGH);

  }if(lineIs("charge-false")){
    digitalWrite(CHARGE_PIN, LOW);

  }if(lineIs("bleed-true")) {
    digitalWrite(BLEED_PIN, HIGH);

  }if(lineIs("bleed-false")){
    digitalWrite(BLEED_PIN, LOW);

  }if(lineIs("sendPin-true")) {
    digitalWrite(RELEASE_PIN_OUT, HIGH);

  }if(lineIs("sendPin-false")){
    digitalWrite(RELEASE_PIN_OUT, LOW);

  }if(lineIs("sendBall-true")) {
    digitalWrite(RELEASE_BALL_OUT, HIGH);

  }if(lineIs("sendBall-false")){
    digitalWrite(RELEASE_BALL_OUT, LOW);
  }

  sendAllInformation(client);
}

void sendAllInformation(WiFiClient client){
  messageNumber++;
  // make sure that this number will never overflow
  if (messageNumber >= 100000){
    messageNumber = 1;
  }

  client.print("Info Tag");

  sendClientInfo("MessageNumber", String(messageNumber), client);

  sendClientInfo("Connected", "T", client);

  sendClientInfo("BackSide", String(voltInputBackside), client);
  sendClientInfo("BottlePressure", String(voltInputBottle), client);
  sendClientInfo("BatteryLevel", String(voltInputBattery), client);

  sendClientInfo("PinInStatus", String(digitalRead(PIN_IN_STATUS)), client);
  sendClientInfo("PinOutStatus", String(digitalRead(PIN_OUT_STATUS)), client);
  sendClientInfo("BallHomeStatus", String(digitalRead(BALL_HOME_STATUS)), client);
  sendClientInfo("BallLaunchedStatus", String(digitalRead(BALL_LAUNCHED_STATUS)), client);
  sendClientInfo("WiperStatus", String(digitalRead(WIPER_STATUS)), client);

  client.print("Info Tag");
}

void sendClientInfo(String description, String info, WiFiClient client){
  client.print(description + "`" + info);
  client.print("~");
}

void getPressuresAndBattery(){
 
  double rawInputBackside = analogRead(BACKSIDE_PRESSURE);
  voltInputBackside = refVoltage * rawInputBackside / pointsToReadOver;

  double rawInputBottle = analogRead(BOTTLE_PRESSURE);
  voltInputBottle = refVoltage * rawInputBottle / pointsToReadOver;

  double rawInputBattery = analogRead(BATTERY_LEVEL);
  voltInputBattery = refVoltage * rawInputBattery / pointsToReadOver;

}

int clientDisconnectedInARow = 0;
void loop() {
  getPressuresAndBattery(); // get the pressure values from the pressure transducers, and the battery level

  // process any connections made over wifi
  WiFiClient client = server.available();  // Listen for incoming clients
  if (client) {                            // If a new client connects,
    String currentLine = "";               // make a String to hold incoming data from the client
    while (client.connected()) {           // loop while the client's connected
      if (client.available()) {            // if there's bytes to read from the client,
         char c = client.read();            // read a byte, then
        header += c;
        if (c == '\n') {  // if the byte is a newline character
          // if the current line is blank that means that this is the end of the message
          if (currentLine.length() == 0) {
            // tell the client that we received its message so it doesnt throw any errors
            client.print("Recieved");
            processMessageRecieved(client);
            // Break out of the while loop (we have read all of the information the client wants to tell us so there is no need to continue trying)
            break;
          } else {  // if you got a newline, then clear currentLine so that the message recieved is handled properly
            currentLine = "";
          }
        } else if (c != '\r') {  // if you got anything else but a carriage return character,
          currentLine += c;      // add it to the end of the currentLine
        }
      }
    }
    header = "";
    // reset the counter since the arduino got a message from the android app recently
    clientDisconnectedInARow = 1;
  }else{
    // increase the counter since the arduino has gotten no signal recently
    clientDisconnectedInARow++;
    //Serial.println(String(clientDisconnectedInARow));
    // if enough time has passed without a signal, reset the voltages to a default
    if (clientDisconnectedInARow > (secondsToWait*1000/delayTime)){
      voltagesToDefault();
    }
  }
  delay(delayTime);
}