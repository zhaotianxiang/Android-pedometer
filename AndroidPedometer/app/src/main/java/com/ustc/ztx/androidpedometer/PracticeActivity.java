package com.ustc.ztx.androidpedometer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

public class PracticeActivity extends Activity implements PermissionInterface, SensorEventListener,
        OnClickListener {
    /** Called when the activity is first created. */
    private final String TAG = "PracticeActivity";
    //Create a LOG label
    private Button mWriteButton, mStopButton;
    private boolean doWrite = false;
    private SensorManager sm;
    private float lowX = 0, lowY = 0, lowZ = 0;
    private final float FILTERING_VALAUE = 0.1f;
    private TextView AT,ACT,Calc;
    private final float alpha = 0.8f;
    int count=0;
    // 加速计的三个维度数值
    public static float[] gravity = new float[3];
    public static float[] linear_acceleration = new float[3];
    //用三个维度算出的平均值
    public static float average = 0;
    private Timer timer;
    // 倒计时8秒，8秒内不会显示计步，用于屏蔽细微波动
    private long duration = 8000;
    private TimeCount time;
    private long perCalTime = 0;
    private final float minValue = 8.8f;
    private final float maxValue = 10.5f;
    private final float verminValue = 9.5f;
    private final float vermaxValue = 10.0f;
    private final float minTime = 150;
    private final float maxTime = 2000;
    /**
     * 0-准备计时   1-计时中  2-准备为正常计步计时  3-正常计步中
     */
    private int CountTimeState = 0;
    public static int CURRENT_SETP = 0;
    public static int TEMP_STEP = 0;
    private int lastStep = -1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionHelper permissionHelper = new PermissionHelper(this, this);
        permissionHelper.requestPermissions();
    }

    private void initView() {
        //初始化视图
        AT = (TextView)findViewById(R.id.AT);
        ACT = (TextView)findViewById(R.id.onAccuracyChanged);
        Calc =(TextView)findViewById(R.id.Button_Calculate);
        //Create a SensorManager to get the system’s sensor service
        sm =
                (SensorManager)getSystemService(Context.SENSOR_SERVICE);
             /*
             *Using the most common method to register an event
             * Parameter1 ：SensorEventListener detectophone
             * Parameter2 ：Sensor one service could have several Sensor
            realizations.Here,We use getDefaultSensor to get the defaulted Sensor
             * Parameter3 ：Mode We can choose the refresh frequency of the
            data change
             * */
        // Register the acceleration sensor
        sm.registerListener(this,
                sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);//High sampling rate；.SENSOR_DELAY_NORMAL means a lower sampling rate
        try {
            FileOutputStream fout = openFileOutput("acc.txt",
                    Context.MODE_PRIVATE);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mWriteButton = (Button) findViewById(R.id.Button_Write);
        mWriteButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.Button_Stop);
        mStopButton.setOnClickListener(this);

    }

    public void onPause(){
        super.onPause();
    }

    public void onClick(View v) {
        if (v.getId() == R.id.Button_Write) {
            doWrite = true;
        }
        if (v.getId() == R.id.Button_Stop) {
            doWrite = false;
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        ACT.setText("onAccuracyChanged is detonated");
    }
    public void onSensorChanged(SensorEvent event) {
        String message = new String();
        String bsmessage = new String();
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            average = (float) Math.sqrt(Math.pow(gravity[0], 2)
                    + Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));

            if (average <= verminValue) {
                if (average <= minValue) {
                    perCalTime = System.currentTimeMillis();
                }
            }else if (average >= vermaxValue) {
                if (average >= maxValue) {
                    //时间差
                    float betweentime = System.currentTimeMillis()
                            - perCalTime;
                    //波峰波谷时间差
                    if (betweentime >= minTime && betweentime < maxTime) {
                        perCalTime = 0;
                        if (CountTimeState == 0) {
                            // 开启计时器
                            time = new TimeCount(duration, 1000);
                            time.start();
                            CountTimeState = 1;
                            Log.v(TAG, "开启计时器");
                        } else if (CountTimeState == 1) {
                            TEMP_STEP++;
                            Log.v(TAG, "计步中 TEMP_STEP:" + TEMP_STEP);
                        } else if (CountTimeState == 2) {
                            timer = new Timer(true);
                            TimerTask task = new TimerTask() {
                                public void run() {
                                    if (lastStep == CURRENT_SETP) {
                                        timer.cancel();
                                        CountTimeState = 0;
                                        lastStep = -1;
                                        TEMP_STEP = 0;
                                        Log.v(TAG, "停止计步：" + CURRENT_SETP);
                                    } else {
                                        lastStep = CURRENT_SETP;
                                    }
                                }
                            };
                            timer.schedule(task, 0, 2000);
                            CountTimeState = 3;
                        } else if (CountTimeState == 3) {
                            CURRENT_SETP++;
                        }
                    }
                }
            }

            DecimalFormat df = new DecimalFormat("#,##0.000");
            bsmessage = df.format(TEMP_STEP);
            ACT.setText(bsmessage);
            AT.setText(message + "\n");
            if (doWrite) {
                write2file(message);
            }
        }
    }

    private void startTimeCount() {
        time = new TimeCount(duration, 1000);
        time.start();
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // 如果计时器正常结束，则开始计步
            time.cancel();
            // save();
            startTimeCount();
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    }
    private void write2file(String a){

        try {

            File file = new File("/sdcard/acc.txt");//write the result into/sdcard/acc.txt
            if (!file.exists()){
                file.createNewFile();}

            // Open a random access file stream for reading and writing
            RandomAccessFile randomFile = new
                    RandomAccessFile("/sdcard/acc.txt", "rw");
            // The length of the file (the number of bytes)
            long fileLength = randomFile.length();
            // Move the file pointer to the end of the file
            randomFile.seek(fileLength);
            randomFile.writeBytes(a);
            randomFile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Override
    public int getPermissionsRequestCode() {
        return 10000;
    }

    @Override
    public String[] getPermissions() {
        return new String[0];
    }

    @Override
    public void requestPermissionsSuccess() {
        initView();
    }


    @Override
    public void requestPermissionsFail() {
        //权限请求不被用户允许。可以提示并退出或者提示权限的用途并重新发起权限申请。
        finish();
    }
}
