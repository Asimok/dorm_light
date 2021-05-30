#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <SoftwareSerial.h>
#include "ArduinoJson-v6.15.2.h"
#define light0 D0   //定义开关1为D0号引脚
#define light1 D1
#define light2 D2
#define light3 D3

long lastMsg = 0;
char msg[500];
int value = 0;
//Wi-Fi配置
const char* ssid = "女寝专用";
const char* password = "208208nb";
//MQTT服务器配置
const char* mqtt_server = "39.96.68.13";
const int mqttPort = 1883;
const char* clientId = "xiangbo";
const char* topic = "dorm";

char* code ;
WiFiClient espClient;
PubSubClient client(espClient);

//4路开关实时状态
bool light0Statue = false;
bool light1Statue = false;
bool light2Statue = false;
bool light3Statue = false;

void setup() {

  Serial.begin(9600);
  //连接wifi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("Connecting to WiFi..");
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
  //  继电器引脚设置为输出模式
  pinMode(light0, OUTPUT);
  pinMode(light1, OUTPUT);
  pinMode(light2, OUTPUT);
  pinMode(light3, OUTPUT);

  // 设备上线提醒
  char buff[50];
  memset(buff, 0, sizeof(buff));
  const char *buff2 = " 上线";
  strcpy(buff, clientId);
  strcat(buff, buff2);
  //发送连接成功消息
  client.publish(topic, buff );
  //硬件设备终端订阅主题
  client.subscribe(topic);

  delay(100);
  //初始化开关状态为关
  switchlight("closeLight0");
  delay(100);
  switchlight("closeLight1");
  delay(100);
  switchlight("closeLight2");
  delay(100);
  switchlight("closeLight3");
  delay(100);
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
  //解析指令
  docode(a);

}

  void docode(char json[2000])
{
  //解析指令
  StaticJsonDocument<200> doc;
  deserializeJson(doc, json);
  const char* code = doc["code"];
  Serial.println();
  Serial.println(code);

  String tempcode;
  tempcode = String(code);
  //  get_light_status 表示获取当前开关状态
  if (tempcode == "get_light_status")
    send_light_data();
  else
    //执行开关灯指令
    switchlight(tempcode);
}
void switchlight(String tempcode)
{

  delay(50);
  //解析四路开关开灯指令
  if (tempcode == "openLight0")
  {
    digitalWrite(light0, HIGH);
    light0Statue = true;
    send_light_data();
  }
   if (tempcode == "openLight1")
  {
    digitalWrite(light1, HIGH);
    light1Statue = true;
    send_light_data();
  }
   if (tempcode == "openLight2")
  {
    digitalWrite(light2, HIGH);
    light2Statue = true;
    send_light_data();

  }
   if (tempcode == "openLight3")
  {
    digitalWrite(light3, HIGH);
    light3Statue = true;
    send_light_data();
  }

//解析四路开关关灯指令
  if (tempcode == "closeLight0")
  {
    digitalWrite(light0, HIGH);
    light0Statue = false;
    send_light_data();
  }
   if (tempcode == "closeLight1")
  {
    digitalWrite(light1, HIGH);
    light1Statue = false;
    send_light_data();
  }
   if (tempcode == "closeLight2")
  {
    digitalWrite(light2, HIGH);
    light2Statue = false;
    send_light_data();
  }
   if (tempcode == "closeLight3")
  {
    digitalWrite(light3, HIGH);
    light3Statue = false;
    send_light_data();
  }


}

void send_light_data() {
  delay(10);

  StaticJsonDocument<200> light_data;
  light_data["sensor"] = "light";

  light_data["light0"] = light0Statue;
  light_data["light1"] = light1Statue;
  light_data["light2"] = light2Statue;
  light_data["light3"] = light3Statue;
  serializeJson(light_data, msg);
  Serial.println(msg);
  client.publish(topic, msg );
  delay(100);
}
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