#include <SoftwareSerial.h>
 
int RX=8;
//int TX=6;
int TX=7;
int RED_PIN = 6;
int GREEN_PIN = 4;
//int BLUE_PIN = 4;

SoftwareSerial bluetooth(RX, TX);
 
void setup(){
  Serial.begin(9600);
  bluetooth.begin(9600);
//  pinMode(RED_PIN, OUTPUT);
  pinMode(GREEN_PIN, OUTPUT);
//  pinMode(BLUE_PIN, OUTPUT);
}
 
void loop(){
   digitalWrite(GREEN_PIN, LOW);
  if (bluetooth.available()) {
    char c = bluetooth.read();
//    digitalWrite(GREEN_PIN, LOW);  // 여기서 LOW 실행해도 LED가 한번 켜지면 안꺼짐
    Serial.print(c);
    if(c == 'a') {
      digitalWrite(GREEN_PIN, HIGH);
      delay(2000);
    }
  }
    if (Serial.available()) {
      bluetooth.write(Serial.read());
  }
}