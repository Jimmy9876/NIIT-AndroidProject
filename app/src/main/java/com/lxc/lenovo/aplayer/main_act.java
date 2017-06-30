package com.lxc.lenovo.aplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Lenovo on 2017/6/29.
 */
public class main_act  extends AppCompatActivity implements View.OnClickListener {

    private EditText etIp;
    private EditText etPort;
    private Button btnStart;
    private TextView tvContent;
    private EditText etUserId;
    private TextView tvConnectServer;
    private Button btnSendMessage;
    private Button btnConfirm;
    private LinearLayout llInit;
    private LinearLayout llMessage;
    private EditText etMessage;
    private Button btnHello;
    private Button btnStartt;
    private Button btnStop;
    private Button btnSendd;
    private Button btnPlay;
    private Button btnFinish;
    private Socket clientSocket;
    private boolean isReceivingMsgReady=false;
    private BufferedReader mReader;
    private BufferedWriter mWriter;
    private boolean isConnected=false;
    private String friendId;
    private LinearLayout llIpport;
    String ip=null;

    private boolean isRecording = true, isPlaying = false; // 标记
    private int frequence = 8000; // 录制频率，单位hz.这里的值注意了，写的不好，可能实例化AudioRecord对象的时候，会出错。我开始写成11025就不行。这取决于硬件设备
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RecordTask recorder;
    private PlayTask player;
    private SendTask sender;
    private File audioFile;

    private StringBuffer sb=new StringBuffer();

    private Handler handler=new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {

                case 1:
                    String msgContent=(String) msg.obj;
                    sb.append("我："+msgContent+"\n");
                    tvContent.setText(sb.toString());
                    break;

                case 2:

                    String message = (String) msg.obj;
                    if(message.contains("用户名")){
                        isConnected=true;
                        tvConnectServer.setText("连接服务器成功,您的"+message);
                    }else{
                        JSONObject json;
                        try {
                            json = new JSONObject(message);
                            sb.append(json.getString("from")+":" +json.getString("msg")+"   "+getTime(System.currentTimeMillis())+"\n");
                            tvContent.setText(sb.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        };
    };
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_one);
        etIp=(EditText) findViewById(R.id.et_ip);
        etPort=(EditText) findViewById(R.id.et_port);
        btnStart=(Button) findViewById(R.id.btn_start);
        etUserId=(EditText) findViewById(R.id.et_user_id);
        btnSendMessage=(Button) findViewById(R.id.btn_send);
        tvContent=(TextView) findViewById(R.id.tv_content);
        tvConnectServer=(TextView) findViewById(R.id.tv_connect_server);
        btnConfirm=(Button) findViewById(R.id.btn_confirm);
        llInit=(LinearLayout) findViewById(R.id.ll_init);
        llMessage=(LinearLayout) findViewById(R.id.ll_message);
        etMessage=(EditText) findViewById(R.id.et_message);
        tvContent=(TextView) findViewById(R.id.tv_content);
        llIpport=(LinearLayout) findViewById(R.id.ll_ip_port);
        btnHello=(Button) findViewById(R.id.btn_hello);
        btnSendd=(Button) findViewById(R.id.btn_sendd);
        btnStartt=(Button) findViewById(R.id.btn_startt);
        btnStop=(Button) findViewById(R.id.btn_stop);
        btnPlay=(Button) findViewById(R.id.btn_play);
        btnFinish=(Button) findViewById(R.id.btn_finish);
        File fpath = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/data/files/");
        fpath.mkdirs();// 创建文件夹
        try {
            // 创建临时文件,注意这里的格式为.pcm
            audioFile = File.createTempFile("recording", ".pcm", fpath);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        btnStart.setOnClickListener(main_act.this);
        btnSendMessage.setOnClickListener(this);
        btnConfirm.setOnClickListener(this);
        etMessage.setInputType(InputType.TYPE_NULL);
        etUserId.setInputType(InputType.TYPE_NULL);
        btnHello.setOnClickListener(this);
        btnStop.setEnabled(false);
        btnPlay.setEnabled(false);
        btnFinish.setEnabled(false);
        btnSendd.setEnabled(false);
        btnStartt.setVisibility(View.GONE);
        btnSendd.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        btnPlay.setVisibility(View.GONE);
        btnFinish.setVisibility(View.GONE);
//        btnSend.setOnClickListener(this);
//        btnStop.setOnClickListener(this);
//        btnStartt.setOnClickListener(this);
//        btnFinish.setOnClickListener(this);
//        btnPlay.setOnClickListener(this);
    }


    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_start:
                if(!isReceivingMsgReady){
                    initSocket();
                }
                llIpport.setVisibility(View.GONE);
                btnStart.setVisibility(View.GONE);
                break;
            case R.id.btn_send:
                send();
                break;
            case R.id.btn_sendd:
                sender = new SendTask();
                sender.execute();
                break;
            case R.id.btn_confirm:
                if(!isConnected){
                    Toast.makeText(main_act.this,"连接服务器失败", Toast.LENGTH_LONG).show();
                }else{
                    if(!TextUtils.isEmpty(etUserId.getText().toString().trim())){
                        llInit.setVisibility(View.GONE);
                        llMessage.setVisibility(View.VISIBLE);
                        friendId=etUserId.getText().toString().trim();
                    }else{
                        Toast.makeText(main_act.this,"请输入对方的用户名id", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.btn_hello:
                btnConfirm.setVisibility((View.GONE));
                btnStartt.setVisibility(View.VISIBLE);
                btnSendd.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);
                btnPlay.setVisibility(View.VISIBLE);
                btnFinish.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_stop:
                // 停止录制
                tvContent.append("\n"+"-------send-------"+"\n"+getTime(System.currentTimeMillis()));
                this.isRecording = false;
                btnPlay.setEnabled(true);
                btnSendd.setEnabled(true);
                // 更新状态
                // 在录制完成时设置，在RecordTask的onPostExecute中完成
                break;
            case R.id.btn_play:
                player = new PlayTask();
                player.execute();
                break;
            case R.id.btn_finish:
                // 完成播放
                this.isPlaying = false;
                btnStart.setEnabled(true);
                btnPlay.setEnabled(false);
                btnFinish.setEnabled(false);
                break;
            case R.id.btn_startt:
                // 开始录制
                // 这里启动录制任务
                recorder = new RecordTask();
                recorder.execute();
                break;
        }
    }

    private void send() {

        new AsyncTask<String, Integer, String>() {

            @Override
            protected String doInBackground(String... params) {
                sendMsg();
                return null;
            }

        }.execute();

    }

    /**
     * 向服务器发送消息
     */
    protected void sendMsg() {
        try {
            //根据clientSocket.getOutputStream得到BufferedWriter对象，从而从输出流中获取数据
            mWriter=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"utf-8"));
            String msgContent=etMessage.getText().toString();
            //封装成json
            JSONObject json = new JSONObject();
            json.put("to", Integer.parseInt(friendId));
            json.put("msg", msgContent);
            //通过BufferedWriter对象向服务器写数据
            mWriter.write(json.toString()+"\n");
            //一定要调用flush将缓存中的数据写到服务器
            mWriter.flush();
            Message msg = handler.obtainMessage();
            msg.what=1;
            msg.obj=msgContent;
            handler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSocket() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                ip=etIp.getText().toString();
                int port=Integer.parseInt(etPort.getText().toString());

                try {
                    isReceivingMsgReady=true;
                    //在子线程中初始化Socket对象
                    clientSocket=new Socket(ip,port);
                    //根据clientSocket.getInputStream得到BufferedReader对象，从而从输入流中获取数据
                    mReader=new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"utf-8"));
                    //根据clientSocket.getOutputStream得到BufferedWriter对象，从而从输出流中获取数据
                    mWriter=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"utf-8"));
                    while(isReceivingMsgReady){
                        if(mReader.ready()){
                            Message msg = handler.obtainMessage();
                            msg.what=2;
                            msg.obj=mReader.readLine();
                            handler.sendMessage(msg);
                        }
                        Thread.sleep(200);
                    }
                    Socket socket=new Socket();
                    int portt=7700;
                    socket.connect(new InetSocketAddress(ip,portt),1000);
                    FileOutputStream writer=null;
                    DataInputStream in=null;
                    in=new DataInputStream(socket.getInputStream());
                    int bufferSize=20480;
                    byte [] buf=null;
                    buf=new byte[bufferSize];
                    writer=new FileOutputStream(audioFile);
                    int read=0;


                    while((read=in.read(buf,0,buf.length))!=-1){
                        writer.write(buf,0,read);
                    }
                    buf=null;
                    in.close();
                    writer.close();
                    socket.close();
                    mWriter.close();
                    mReader.close();
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
    class RecordTask extends AsyncTask {


        // 当在上面方法中调用publishProgress时，该方法触发,该方法在UI线程中被执行


        @Override
        protected Object doInBackground(Object[] objects) {
            isRecording = true;
            try {
                // 开通输出流到指定的文件
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(audioFile)));
                // 根据定义好的几个配置，来获取合适的缓冲大小
                int bufferSize = AudioRecord.getMinBufferSize(frequence, channelConfig, audioEncoding);
                // 实例化AudioRecord
                AudioRecord record = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequence,
                        channelConfig, audioEncoding, bufferSize);
                // 定义缓冲
                short[] buffer = new short[bufferSize];


                // 开始录制
                record.startRecording();


                int r = 0; // 存储录制进度
                // 定义循环，根据isRecording的值来判断是否继续录制
                while (isRecording) {
                    // 从bufferSize中读取字节，返回读取的short个数
                    // 这里老是出现buffer overflow，不知道是什么原因，试了好几个值，都没用，TODO：待解决
                    int bufferReadResult = record
                            .read(buffer, 0, buffer.length);
                    // 循环将buffer中的音频数据写入到OutputStream中
                    for (int i = 0; i < bufferReadResult; i++) {
                        dos.writeShort(buffer[i]);
                    }
                    publishProgress(new Integer(r)); // 向UI线程报告当前进度
                    r++; // 自增进度值
                }
                // 录制结束
                record.stop();
                Log.v("The DOS available:", "::" + audioFile.length());
                dos.close();
            } catch (Exception e) {
                // TODO: handle exception
            }
            return null;
        }

        protected void onPreExecute() {
            // stateView.setText("正在录制");
            btnStartt.setEnabled(false);
            btnPlay.setEnabled(false);
            btnFinish.setEnabled(false);
            btnStop.setEnabled(true);
        }

        protected void onProgressUpdate(Integer... progress) {
            tvContent.setText(progress[0].toString());
        }


        protected void onPostExecute(Void result) {
            btnStop.setEnabled(false);
            btnStartt.setEnabled(true);
            btnPlay.setEnabled(true);
            btnFinish.setEnabled(false);
        }

    }
    class PlayTask extends AsyncTask {


        @Override
        protected Object doInBackground(Object[] objects) {
            isPlaying = true;
            int bufferSize = AudioTrack.getMinBufferSize(frequence,
                    channelConfig, audioEncoding);
            short[] buffer = new short[bufferSize / 4];
            try {
                // 定义输入流，将音频写入到AudioTrack类中，实现播放
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(audioFile)));
                // 实例AudioTrack
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                        frequence, channelConfig, audioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);
                // 开始播放
                track.play();   // 由于AudioTrack播放的是流，所以，我们需要一边播放一边读取
                while (isPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < buffer.length) {
                        buffer[i] = dis.readShort();
                        i++;
                    }
                    // 然后将数据写入到AudioTrack中
                    track.write(buffer, 0, buffer.length);


                }


                // 播放结束
                track.stop();
                dis.close();
            } catch (Exception e) {
                // TODO: handle exception
            }
            return null;

        }
        protected void onPostExecute(Void result) {
            btnPlay.setEnabled(true);
            btnFinish.setEnabled(false);
            btnStartt.setEnabled(true);
            btnStop.setEnabled(false);
        }


        protected void onPreExecute() {


            // stateView.setText("正在播放");
            btnStartt.setEnabled(false);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(false);
            btnFinish.setEnabled(true);
        }


    }
    class SendTask extends AsyncTask {


        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                Socket socket=new Socket();
                int port=7700;
                socket.connect(new InetSocketAddress(ip,port),1000);
                FileInputStream reader=null;
                DataOutputStream out=null;
                out=new DataOutputStream(socket.getOutputStream());
                int bufferSize=20480;
                byte [] buf=null;
                buf=new byte[bufferSize];
                reader=new FileInputStream(audioFile);
                int read=0;


                while((read=reader.read(buf,0,buf.length))!=-1){
                    out.write(buf,0,read);
                }
                buf=null;
                out.close();
                reader.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;

        }
        protected void onPostExecute(Void result) {
            btnPlay.setEnabled(true);
            btnFinish.setEnabled(false);
            btnStartt.setEnabled(true);
            btnStop.setEnabled(false);
        }


        protected void onPreExecute() {


            // stateView.setText("正在播放");
            btnStartt.setEnabled(false);
            btnStop.setEnabled(false);
            btnPlay.setEnabled(false);
            btnFinish.setEnabled(true);
        }


    }
    /**
     * 得到自己定义的时间格式的样式
     * @param millTime
     * @return
     */
    private String getTime(long millTime) {
        Date d = new Date(millTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf.format(d));
        return sdf.format(d);
    }

}
