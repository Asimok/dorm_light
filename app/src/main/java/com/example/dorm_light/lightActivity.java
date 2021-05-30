package com.example.dorm_light;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import static com.example.dorm_light.light_code.get_light_status;
import static com.example.dorm_light.light_code.openLight0_code;
import static com.example.dorm_light.light_code.openLight1_code;
import static com.example.dorm_light.light_code.openLight2_code;
import static com.example.dorm_light.light_code.openLight3_code;

public class lightActivity extends AppCompatActivity implements IGetMessageCallBack {
    private Button light0, light1, light2, light3;
    private MyServiceConnection serviceConnection;
    private final Handler handler = new Handler();
    private final int get_delay = 10000;
    public boolean isMQTT_connect = false;
    private boolean light0_status_bool = false;
    private boolean light1_status_bool = false;
    private boolean light2_status_bool = false;
    private boolean light3_status_bool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light);
        light0 = findViewById(R.id.light0);
        light1 = findViewById(R.id.light1);
        light2 = findViewById(R.id.light2);
        light3 = findViewById(R.id.light3);

        serviceConnection = new MyServiceConnection();
        serviceConnection.setIGetMessageCallBack(lightActivity.this);

        //用Intent方式创建并启用Service
        Intent intent = new Intent(this, MQTTService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


        light0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MQTTService.publish(openLight0_code);
            }
        });
        light1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(openLight1_code);
            }
        });
        light2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(openLight2_code);
            }
        });
        light3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(openLight3_code);
            }
        });


        Runnable runnable = new Runnable() {
            public void run() {
                Log.d("log", "请求实时开关状态");
                if (isMQTT_connect) {
                    MQTTService.publish(get_light_status);
                } else {
                    light0.setText("与服务器断开连接");
                }
                handler.postDelayed(this, get_delay);
            }
        };
        //        自动请求数据
        handler.postDelayed(runnable, 1000);//开启Timer
    }

    //继承setMessage()接口 解析硬件设备指令
    @Override
    public void setMessage(String message) throws JSONException {

        Log.d("log", message);

        if (message.contains("light")) {
            Log.d("log", "解析开关状态");
//          解析指令
            JSONObject obj = new JSONObject(message);
             light0_status_bool = obj.getBoolean("light0");
             light1_status_bool = obj.getBoolean("light1");
             light2_status_bool = obj.getBoolean("light2");
             light3_status_bool = obj.getBoolean("light3");

            String light0_status = light0_status_bool ? "开关0 - 打开" : "开关0 - 关闭";
            String light1_status = light1_status_bool ? "开关1 - 打开" : "开关1 - 关闭";
            String light2_status = light2_status_bool ? "开关2 - 打开" : "开关2 - 关闭";
            String light3_status = light3_status_bool ? "开关3 - 打开" : "开关3 - 关闭";

            light0.setText(light0_status);
            light1.setText(light1_status);
            light2.setText(light2_status);
            light3.setText(light3_status);

        }
        MQTTService mqttService = serviceConnection.getMqttService();
        mqttService.toCreateNotification(message);

    }

    //实时监测MQTT服务器连接状态
    @Override
    public void setMQTTstatus(boolean MQTTstatus) {
        isMQTT_connect = MQTTstatus;
    }


    //这里最主要还是销毁掉服务，因为Activity销毁掉以后
    //Service并不会自动回收，而是会转入后台运行，这样会
    //影响到下一次的运行，所以这里必须做销毁处理
    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

}