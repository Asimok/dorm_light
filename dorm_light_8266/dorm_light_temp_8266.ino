#include<Servo.h>
#include "dht11.h"
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <SoftwareSerial.h>
#include "ArduinoJson-v6.15.2.h"
#define dht11Pin D2   //定义温湿度针脚号为D2号引脚

dht11 dht;    //实例化一个对象
Servo left_servo, right_servo;
long lastMsg = 0;
char msg[500];
int value = 0;

const char* ssid = "女寝专用";
const char* password = "208208nb";
const char* mqtt_server = "39.96.68.13";
const int mqttPort = 1883;
const char* clientId = "4B208";
const char* topic = "dorm";
char* code = "closeLeft";
WiFiClient espClient;
PubSubClient client(espClient);

bool leftStatue = false;
bool rightStatue = false;

void setup() {


  Serial.begin(9600);
  //连接wifi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    // Serial.println("Connecting to WiFi..");
  }
  Serial.println("Connected to the WiFi network");
  //连接MQTT服务器
  client.setServer(mqtt_server, mqttPort);
  client.setCallback(callback);
  while (!client.connected()) {
    Serial.println("Connecting to MQTT...");
    if (client.connect(clientId)) {
      Serial.println("connected");
    } else {
      Serial.print("failed with state ");
      Serial.print(client.state());
      delay(2000);
    }
  }
  pinMode(dht11Pin, OUTPUT);
  char buff[50];
  memset(buff, 0, sizeof(buff));
  const char *buff2 = " 上线";
  strcpy(buff, clientId);
  strcat(buff, buff2);
  //发送连接成功消息
  client.publish(topic, buff );
  //订阅主题
  client.subscribe(topic);
  delay(100);
  //  初始化开关状态
  switchlight("closeLeft");
  delay(1000);
  switchlight("closeRight");
}
void get_dht11()     //loop函数，重复循环执行
{
  int tol = dht.read(dht11Pin);    //将读取到的值赋给tol
  int temp = (float)dht.temperature; //将温度值赋值给temp
  int humi = (float)dht.humidity; //将湿度值赋给humi
  //  Serial.print("Temperature:");     //在串口打印出Tempeature:
  //  Serial.print(temp);       //在串口打印温度结果
  //  Serial.println("℃");    //在串口打印出℃
  //  Serial.print("Humidity:");     //在串口打印出Humidity:
  //  Serial.print(humi);     //在串口打印出湿度结果
  //  Serial.println("%");     //在串口打印出%
  delay(10);      //延时1秒


  StaticJsonDocument<200> temperature_data;

  temperature_data["sensor"] = "DHT11";
  temperature_data["temp"] = temp;
  temperature_data["humi"] = humi;
  serializeJson(temperature_data, msg);
  //      Serial.println(msg);
  client.publish(topic, msg );


}
/**
   断开重连
*/
void reconnect_mqtt() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.println("reConnecting to MQTT...");

    if (client.connect(clientId)) {
      Serial.println("connected");
    } else {
      Serial.print("failed with state ");
      Serial.print(client.state());
      delay(2000);
    }
  }
}
void reconnect_wifi()
{
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("Connecting to WiFi..");
  }
  Serial.println("reConnected to the WiFi network");
}

void callback(char* topic, byte* payload, unsigned int length) {
  //收到消息
  Serial.print("Message arrived in topic: ");
  Serial.println(topic);
  Serial.print("Message:");
  char a[2000] = "";
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
    a[i] = (char)payload[i];
  }

  docode(a);

}
void docode(char json[2000])
{
  //  "解析指令"

  StaticJsonDocument<200> doc;
  deserializeJson(doc, json);
  const char* code = doc["code"];
  Serial.println();
  Serial.println(code);
  String tempcode;
  tempcode = String(code);
  if (tempcode == "get_dht11")
    get_dht11();
    else if (tempcode == "get_light_status")
     send_light_data();
  else
    switchlight(tempcode);


}
void switchlight(String tempcode)
{

  delay(50);
  if (tempcode == "openLeft")
  {
    left_servo.attach(D0);
    left_servo.write(82);
    delay(300);
    left_servo.write(45);
    delay(100);
    left_servo.detach();
    leftStatue = true;
send_light_data();
  }
  else if (tempcode == "openRight")
  {
    right_servo.attach(D1);
    right_servo.write(8);
    delay(300);
    right_servo.write(65);
    delay(100);
    right_servo.detach();
    rightStatue = true;
    send_light_data();
  }
  else if (tempcode == "closeLeft")
  {
    left_servo.attach(D0);
    left_servo.write(2);
    delay(300);
    left_servo.write(45);
    delay(100);
    left_servo.detach();
    leftStatue = false;
    send_light_data();

  }
  else if (tempcode == "closeRight")
  {
    right_servo.attach(D1);
    right_servo.write(130);
    delay(300);
    right_servo.write(65);
    delay(100);
    right_servo.detach();
    rightStatue = false;
    send_light_data();
  }

  


}

void send_light_data(){
  delay(10);

  StaticJsonDocument<200> light_data;
  light_data["sensor"] = "servo";
  light_data["left"] = leftStatue;
  light_data["right"] = rightStatue;
  serializeJson(light_data, msg);
  //      Serial.println(msg);
  client.publish(topic, msg );
  delay(100);}
void loop() {

  //重连机制
  if (!client.connected()) {
    reconnect_mqtt();
  }
  if (WiFi.status() != WL_CONNECTED)
  {
    reconnect_wifi();
  }
  client.loop();

}
