/**
  The Pozyx ready to range tutorial (c) Pozyx Labs
  Please read the tutorial that accompanies this sketch: https://www.pozyx.io/Documentation/Tutorials/ready_to_range/Arduino
  
  This demo requires two Pozyx devices and one Arduino. It demonstrates the ranging capabilities and the functionality to 
  to remotely control a Pozyx device. Place one of the Pozyx shields on the Arduino and upload this sketch. Move around
  with the other Pozyx device.
  
  This demo measures the range between the two devices. The closer the devices are to each other, the more LEDs will
  light up on both devices.
*/

#include "Pozyx.h"
#include "Pozyx_definitions.h"
#include <Wire.h>

////////////////////////////////////////////////
////////////////// PARAMETERS //////////////////
////////////////////////////////////////////////
// RANGE PARAMETERS

uint16_t anchors[4] = {0x6F3F, 0x6F2B, 0x6F13, 0x6F29};     // the network id of the other pozyx device: fill in the network id of the other device

bool use_remote = false;    //remote mode not working yet. We probably need to update the pozyx firmawre
uint16_t remote_id = anchors[0];
uint8_t data = 0;
////////////////////////////////////////////////
// POSITIONING PARAMETERS

// TODO: Hey fellow lauzhacker, you may want to set anchors_x, anchors_y and heights if you want the areduino to compute the x,y,z position for you
bool use_positioning = true;
uint8_t num_anchors = 4;
int32_t anchors_x[4] = { 6800, 2280,     0,  3970};            // anchor x-coorindates in millimeters
int32_t anchors_y[4] = {-2740,    0, -2980, -9600};            // anchor y-coordinates in millimeters
int32_t heights[4] =   { 1960, 2010,  2480,  2060};            // anchor z-coordinates in millimeters
uint8_t algorithm = POZYX_POS_ALG_UWB_ONLY; // POZYX_POS_ALG_LS;
uint8_t dimension = POZYX_3D;
int32_t height = 1000;
///////////////////////////////////////////////


void getChannel(char* buf, unsigned char channel);
void getRate(char* buf, unsigned char rate);
void getPreLenght(char* buf, uint8_t pre);
void getPulseReap(char* buf, uint8_t pulse);
void getGain(char* buf, unsigned char gain);

void setup(){
  Serial.begin(115200);
    
  if(Pozyx.begin() == POZYX_FAILURE){
    Serial.println("ERROR: Unable to connect to POZYX shield");
    Serial.println("Reset required");
    delay(100);
    abort();
  }
  if(!use_remote){
    remote_id = NULL;
  }
  if (use_positioning){
    // clear all previous devices in the device list
    Pozyx.clearDevices(remote_id);
    // sets the anchor manually
    setAnchorsManual();
  }

  // make sure the pozyx system has no control over the LEDs, we're the boss
  uint8_t led_config = 0x0;
  Pozyx.setLedConfig(led_config);
  // do the same with the remote device
  Pozyx.setLedConfig(led_config, anchors);

  // This block is for synchronization with python script
  if (true){ 
    Serial.println("READY FOR TIME SYNC");
    String line = "";
    while (line!="END SYNC\n"){
      line = serialReadLine();
      Serial.println("0xHEXA INTE INTE SH");
    }
  }  
    
  unsigned long time;
  // millis is ~1582 lower than range.timestamp
  // And the Pozyx have a delay of ~400 ms (need to be measured precisely)
  time = millis() + 1582;// - 400;
  Serial.print("TIME0=");
  Serial.println(time);
  
  Serial.println("START Ranging:");
}

void loop(){
  if (!use_positioning){
    if (use_remote){
      measureRange(remote_id);
      measureRemoteRange(remote_id, anchors[1]);
      measureRemoteRange(remote_id, anchors[2]);
      measureRemoteRange(remote_id, anchors[3]);
    }else{
      for(const uint16_t &dest_id : anchors){
        measureRange(dest_id);
      }
    }
  }else{
    coordinates_t position;
    int status;
    if(use_remote){
      status = Pozyx.doRemotePositioning(remote_id, &position, dimension, height, algorithm);
    }else{
      status = Pozyx.doPositioning(&position, dimension, height, algorithm);
    }
    if (status == POZYX_SUCCESS){
      // prints out the result
      for(const uint16_t &dest_id : anchors){
        device_range_t device_range;
        status = Pozyx.getDeviceRangeInfo(dest_id, &device_range);
        printRange(status, dest_id, device_range);
        Serial.print(" ");
        printCoordinates(position);
        Serial.println();
      }
    }else{
      // prints out the error code
      printErrorCode("positioning");
      Serial.println();
    }
  }
}

void measureRange(uint16_t destination_id){
  int status = 1;
  device_range_t range;
  // let's do ranging with the destination
  status &= Pozyx.doRanging(destination_id, &range);
  printRange(status, destination_id, range);
  Serial.println();
}

void measureRemoteRange(uint16_t remote_id, uint16_t destination_id){
  // This function is not working yet
  int status = 1;
  device_range_t range;
  // let's do ranging with the destination
  status = Pozyx.doRemoteRanging(remote_id, destination_id, &range);
  printRange(status, destination_id, range);
  Serial.println();
}

void printRange(int status, uint16_t destination_id, device_range_t range){    
  Serial.print("0x");
  Serial.print(destination_id, HEX);
  Serial.print(" ");
  
  if (status == POZYX_SUCCESS){
    Serial.print(range.timestamp);
    Serial.print(" ");
    Serial.print(range.distance);
    Serial.print(" ");
    Serial.print(range.RSS);
  }
  else if (status == POZYX_TIMEOUT) {
    Serial.print("ERROR: timeout");
    printErrorCode("ranging");
  }
  else if (status == POZYX_FAILURE) {
    Serial.print("ERROR: failure");
    printErrorCode("ranging");
  }else{
    printErrorCode("ranging");
  }
}

// Only used for positioning
void printCoordinates(coordinates_t coor){
  Serial.print(coor.x);
  Serial.print(" ");
  Serial.print(coor.y);
  Serial.print(" ");
  Serial.print(coor.z);
}

// This function is not mandatory
// Error printing function for debugging
// the string "operation" is not important for the sketch. It's just a string which will be printed along with the error
void printErrorCode(String operation){
  uint8_t error_code;
  if (remote_id == NULL){
    Pozyx.getErrorCode(&error_code);
    Serial.print("ERROR ");
    Serial.print(operation);
    Serial.print(", local error code: 0x");
    Serial.print(error_code, HEX);
    return;
  }
  int status = Pozyx.getErrorCode(&error_code, remote_id);
  if(status == POZYX_SUCCESS){
    Serial.print("ERROR ");
    Serial.print(operation);
    Serial.print(" on ID 0x");
    Serial.print(remote_id, HEX);
    Serial.print(", error code: 0x");
    Serial.print(error_code, HEX);
  }else{
    Pozyx.getErrorCode(&error_code);
    Serial.print("ERROR ");
    Serial.print(operation);
    Serial.print(", couldn't retrieve remote error code, local error: 0x");
    Serial.print(error_code, HEX);
  }
}

// Only used for positioning
void setAnchorsManual(){
  for(int i = 0; i < num_anchors; i++){
    device_coordinates_t anchor;
    anchor.network_id = anchors[i];
    anchor.flag = 0x1;
    anchor.pos.x = anchors_x[i];
    anchor.pos.y = anchors_y[i];
    anchor.pos.z = heights[i];
    Pozyx.addDevice(anchor, remote_id);
 }
 if (num_anchors > 4){
  Pozyx.setSelectionOfAnchors(POZYX_ANCHOR_SEL_AUTO, num_anchors);
 }
}


/*
 * Read a charachers from the serial until a '\n' appears
 */
String serialReadLine(){
  String line = "";
  while (true){
    while (Serial.available() == 0){  }
    // something has been received
    while (Serial.available() > 0){
      char c = Serial.read();
      line += c;
      if (c=='\n'){
        return line;
      }
    }
  }
}
