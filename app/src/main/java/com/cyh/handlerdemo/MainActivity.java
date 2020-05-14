package com.cyh.handlerdemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "ManiActivity";
    private Button button;
    private Button btnUpdateProcess;
    private Button btn_countdownTime;
    private TextView textView;
    private TextView tv_countdowntime;
    private ProgressBar progressBar;
    Handler handler;
    private String downUrl = "http://qn.yingyonghui.com/apk/6640175/54eb144ebe12642411df84bf5f1633ff?sign=dc093ba510a012dc36f79d912a0ac39a&t=5ebcf061&attname=54eb144ebe12642411df84bf5f1633ff.apk";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        /**
         * 主线程
         */
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //处理消息
                Log.e(TAG,"msg.what = " + msg.what);
                switch (msg.what){
                    case 1001:
                        textView.setText("handler");
                        break;
                    case 1002:
                        Log.e(TAG,"msg.arg1 = " + msg.arg1+ "   msg.arg2 = "+ msg.arg2 + "   msg.obj= " + msg.obj);
                        break;
                    case 1003:
                        progressBar.setProgress((Integer) msg.obj);//主线程更新进度条
                }
            }
        };

//        handler.sendEmptyMessage(1001);
    }

    /**
     * Find the Views in the layout<br />
     * <br />
     * Auto-created on 2020-05-14 09:37:16 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    private void findViews() {
        button = (Button)findViewById( R.id.button );
        btnUpdateProcess = (Button)findViewById( R.id.btnUpdateProcess );
        btn_countdownTime = (Button)findViewById( R.id.btn_countdownTime );
        textView = findViewById( R.id.textView );
        tv_countdowntime = findViewById( R.id.tv_countdowntime );
        progressBar = findViewById(R.id.progressBar);

        button.setOnClickListener( this );
        btnUpdateProcess.setOnClickListener( this );
        btn_countdownTime.setOnClickListener( this );
    }

    /**
     * Handle button click events<br />
     * <br />
     * Auto-created on 2020-05-14 09:37:16 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    @Override
    public void onClick(View v) {
        if ( v == button ) {
            // Handle clicks for button
            //子线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(1001);

                    //1. handler.sendMessage(message);打包发送消息
                    Message message = Message.obtain();//不推荐使用new Message，推荐使用Message.obtain()，因为obtain里边有个消息池
                    message.what = 1002;//标志
                    message.arg1 = 66; //int类型消息1
                    message.arg2 = 99; //int类型消息2
                    message.obj = MainActivity.this; //一切Object对象消息
                    handler.sendMessage(message);

                    //2.计时
//                    handler.sendMessageAtTime(message, SystemClock.uptimeMillis() + 3000);//绝对时间
//                    handler.sendMessageDelayed(message,2000);//相对时间，2S后发送

                    //post做定时任务

                }
            }).start();//不要忘记start
        }else if (v == btnUpdateProcess){
            //异步更新进度条
            //主线程点击、子线程下载、下载完成后通知主线程、主线程更新进度条
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //下载文件
                    downFile(downUrl);

                }
            }).start();

        }else if (v == btn_countdownTime){

            //倒计时
            countdownTime();
        }
    }

    /**
     * 倒计时
     */
    private void countdownTime() {
        CountDownTimeHandler countDownTimeHandler = new CountDownTimeHandler(this);

        Message message = Message.obtain();
        message.what = 1004;
        message.arg1 = 10;
        countDownTimeHandler.sendMessageDelayed(message,1000);
    }

    /**
     * 弱引用Handler
     */
    public static class CountDownTimeHandler extends Handler{
        final WeakReference<MainActivity> mWeakReference;
        public CountDownTimeHandler(MainActivity activity) {//构造方法传入activity类
            mWeakReference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MainActivity activity = mWeakReference.get();//获取到acitivy实例，方便拿到控件
            switch (msg.what){
                case 1004:
                    int vaule = msg.arg1;
                    activity.tv_countdowntime.setText(String.valueOf(vaule--));
                    if (vaule > 0){
//                        sendEmptyMessageDelayed(1004,1000);//报错
                        //需要再次创建Mesage，从消息池obtain中获取value值。再次发送
                        Message message = Message.obtain();
                        message.what = 1004;
                        message.arg1 = vaule;
                        sendMessageDelayed(message,1000);
                    }

            }
        }
    }

    /**
     * 下载文件
     * @param downUrl
     */
    private void downFile(String downUrl) {
        try {
            URL url = new URL(downUrl);
            URLConnection urlConnection = url.openConnection();
            InputStream inputStream = urlConnection.getInputStream();

            //动态获取内存存储权限
            if (Build.VERSION.SDK_INT >= 23) {
                int REQUEST_CODE_CONTACT = 101;
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE}; //验证是否许可权限
                for (String str : permissions) {
                    if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                        //申请权限
                        this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                        return;
                    }
                }
            }

            /**
             * 获取文件的总长度
             */
            int contentLength = urlConnection.getContentLength();
            String downloadFolderName = Environment.getExternalStorageDirectory() + File.separator + "cyh" + File.separator;

            File file = new File(downloadFolderName);
            if (!file.exists()){
                file.mkdir();
            }
            String fileName = downloadFolderName + "cyh.apk";
            File apkFile = new File(fileName);

            if (apkFile.exists()){
                apkFile.delete();
            }

            //下载长度
            int downloadSize = 0;
            byte[] bytes = new byte[1024];
            int length = 0;

            OutputStream outputStream = new FileOutputStream(fileName);
            while ((length = inputStream.read(bytes)) != -1){
                outputStream.write(bytes,0,length);
                downloadSize += length;

                //下载完成后通知主线程
                Message message = Message.obtain();
                message.obj = downloadSize * 100 /contentLength;
                message.what = 1003;
                handler.sendMessage(message);
            }

            inputStream.close();
            outputStream.close();


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
