package com.example.dorm_light;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flyco.dialog.widget.base.BaseDialog;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import static com.example.dorm_light.code.closeLeft_code;
import static com.example.dorm_light.code.closeRight_code;
import static com.example.dorm_light.code.get_dht11;
import static com.example.dorm_light.code.get_light_status;
import static com.example.dorm_light.code.openLeft_code;
import static com.example.dorm_light.code.openRight_code;
import static com.example.dorm_light.code.restart_code;

public class MainActivity extends AppCompatActivity implements IGetMessageCallBack {


    private Button openLeft_button, openRight_button, closeRight_button, closeLeft_button;
    private TextView temp_tv, humi_tv, setLeft_tv, setRight_tv;
    private LinearLayout get_dht11_lv, restart;
    private MyServiceConnection serviceConnection;
    private MQTTService mqttService;
    private boolean is_get_dht11 = true;

    private Handler handler = new Handler();
    private Runnable runnable;
    private int get_delay = 10000;
    public boolean isMQTT_connect = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_land);


        openLeft_button = findViewById(R.id.openLeft_id);
        openRight_button = findViewById(R.id.openRight_id);
        closeLeft_button = findViewById(R.id.closeLeft_id);
        closeRight_button = findViewById(R.id.closeRight_id);
        humi_tv = findViewById(R.id.humi_id);
        temp_tv = findViewById(R.id.temp_id);
        get_dht11_lv = findViewById(R.id.get_dht11_id);
        setLeft_tv = findViewById(R.id.set_leftstatus_id);
        setRight_tv = findViewById(R.id.set_rightstatus_id);
        restart = findViewById(R.id.restart);

        serviceConnection = new MyServiceConnection();
        serviceConnection.setIGetMessageCallBack(MainActivity.this);
        //用Intent方式创建并启用Service
        Intent intent = new Intent(this, MQTTService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


        //按下这个按钮就发布一条消息
        openLeft_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(openLeft_code);
            }
        });
        openRight_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(openRight_code);
            }
        });
        closeRight_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(closeRight_code);
            }
        });
        closeLeft_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MQTTService.publish(closeLeft_code);
            }
        });
        final AdDialog adDialog = new AdDialog(this);
        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                adDialog.onCreateView();
                adDialog.setUiBeforShow();
                //点击空白区域能不能退出
                adDialog.setCanceledOnTouchOutside(true);
                //按返回键能不能退出
                adDialog.setCancelable(true);
                adDialog.show();


            }
        });
        get_dht11_lv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//
                is_get_dht11 = !is_get_dht11;
                Log.d("aaa", "点击" + is_get_dht11);
                if (is_get_dht11) {
                    temp_tv.setText("正在请求");
                    humi_tv.setText("正在请求");
                    //开启定时查询任务
                    handler.postDelayed(runnable, 1000);//开启Timer
                } else {
                    handler.removeCallbacks(runnable); //停止Timer
                    temp_tv.setText("未请求");
                    humi_tv.setText("未请求");
                }
            }
        });


        runnable = new Runnable() {
            public void run() {
                Log.d("aaa", "请求");
                if (isMQTT_connect) {

                    MQTTService.publish(get_dht11);
                    MQTTService.publish(get_light_status);
                } else {
                    temp_tv.setText("失去连接");
                    humi_tv.setText("失去连接");
                }
                handler.postDelayed(this, get_delay);
                //postDelayed(this,2000)方法安排一个Runnable对象到主线程队列中
            }
        };
        //        自动请求数据
        handler.postDelayed(runnable, 1000);//开启Timer
    }

    //继承了这个接口就直接用这个借口的方法就好啦
    @Override
    public void setMessage(String message) throws JSONException {
        Log.d("aaa", "main message");
        Log.d("aaa", message);
        if (message.contains("DHT11")) {
            Log.d("aaa", "解析指令DHT11");
//          解析指令
            JSONObject obj = new JSONObject(message);
            String temp = obj.getString("temp").trim();
            String humi = obj.getString("humi").trim();
            Log.d("aaa", temp);
            Log.d("aaa", humi);
            temp_tv.setText(temp + "°C");
            humi_tv.setText(humi + " %");

        }
        if (message.contains("servo")) {
            Log.d("aaa", "解析指令servo");
//          解析指令
            JSONObject obj = new JSONObject(message);
            boolean left_status_bool = obj.getBoolean("left");
            boolean right_status_bool = obj.getBoolean("right");
            String left_status = left_status_bool ? "开" : "关";
            String right_status = right_status_bool ? "开" : "关";
            Log.d("aaa", left_status);
            Log.d("aaa", right_status);
            setLeft_tv.setText(left_status);
            setRight_tv.setText(right_status);

        }

        mqttService = serviceConnection.getMqttService();
        mqttService.toCreateNotification(message);


    }

    @Override
    public void setMQTTstatus(boolean MQTTstatus) {

        temp_tv.setText("连接成功");
        humi_tv.setText("连接成功");
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

    public class AdDialog extends BaseDialog<AdDialog> {
        private Context context;
        private ImageView iv_ad;
        private ImageView back;

        public AdDialog(Context context) {
            super( context);
            this.context = (Context) context;
        }


        //该方法用来出来数据初始化代码
        @Override
        public View onCreateView() {
            widthScale(0.85f);
            //填充弹窗布局
            View inflate = View.inflate(context, R.layout.addialog, null);
            //用来放整个图片的控件
            iv_ad = (ImageView) inflate.findViewById(R.id.iv_ad);
            //放在透明部分和错号上的隐形控件，用来点击使弹窗消失
            back = (ImageView) inflate.findViewById(R.id.ad_back);
            //用来加载网络图片，填充iv_ad控件，注意要添加网络权限，和Picasso的依赖和混淆
//            Picasso.with(context)
//                    .load("https://i.loli.net/2020/10/13/PCYi62HeISOavc7.jpg")
//                    .into(iv_ad);
            iv_ad.setImageResource(R.drawable.xb);

            return inflate;
        }

        //该方法用来处理逻辑代码
        @Override
        public void setUiBeforShow() {
            //点击弹窗相应位置，处理相关逻辑。
            iv_ad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //处理完逻辑关闭弹框的代码
                    MQTTService.publish(restart_code);
                    Toast.makeText(context, "正在重启……", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
            //点×关闭弹框的代码
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //关闭弹框的代码
                    dismiss();
                }
            });
        }
    }

}
