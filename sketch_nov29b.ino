#include <SoftwareSerial.h>

// 시리얼 핀 설정
SoftwareSerial BTSerial(3, 4);


// DC 모터 핀
const int motor1 = 10;
const int motor2 = 11;

// 잠김 플래그
boolean flag = false;

// 받는 데이터 저장 변수
char command;

void setup() {
  Serial.begin(9600);
  BTSerial.begin(9600);

  // 출력핀 모드 설정
  pinMode(motor1, OUTPUT);
  pinMode(motor2, OUTPUT);
  
  Serial.print("Strat!!\n");
}

void loop() {
  // 블루투스 데이터 수신
  if(BTSerial.available()){
    command = BTSerial.read();

    // '1'을 받으면 열고
    // '2'를 받으면 잠그고
    switch(command){
      case '1':
        Open();
        break;
      case '2':
        Close();
        break;
    }
  }
}

// open
void Open(){
  if(!flag){
    Serial.print("Open\n");
    digitalWrite(motor1,LOW);
    digitalWrite(motor2,HIGH);
    delay(1000);
    digitalWrite(motor2,LOW);
    flag = true;
  }
}

// close
void Close(){
  if(flag){
    Serial.print("Close\n");
    digitalWrite(motor1,HIGH);
    digitalWrite(motor2,LOW);
    delay(1000);
    digitalWrite(motor1,LOW);
    
    flag = false;
  }
}

