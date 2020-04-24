package si.uni_lj.fri.pbd.miniapp2;
import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button bStart;
    private Button bPause;
    private Button bStop;
    TextView textView1;
    TextView textView2;
    MediaPlayerService mediaplayerservice;
    private boolean serviceBound;
    private final Handler mUpdateTimeHandler = new UIUpdateHandler(this);
    // Message type for the handler
    private final static int MSG_UPDATE_TIME = 0;



    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service bound");
            MediaPlayerService.MediaPlayerServiceBinder binder = (MediaPlayerService.MediaPlayerServiceBinder) iBinder;
            mediaplayerservice = binder.getService();
            serviceBound = true;
            mediaplayerservice.background();
            // Update the UI if the service is already running the timer
            if (mediaplayerservice.isTimerRunning()) {
                int num = mediaplayerservice.currentPosition;
                textView1.setText(mediaplayerservice.list[num]);
                updateUIStartRun();
            }
            else if((!mediaplayerservice.isTimerRunning())&&(mediaplayerservice.flag==1)) {
                int num = mediaplayerservice.currentPosition;
                textView1.setText(mediaplayerservice.list[num]);
                updateUITimer();
            }

        }
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnect");
            serviceBound = false;
        }
    };
    private BroadcastReceiver mAlertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String fromAlertStr = intent.getStringExtra("message");
            //System.out.println("OnReceive " + fromAlertStr);
            if(fromAlertStr.equals("finish"))
            {
                exitPlayer();
            }
            else if(fromAlertStr.equals("musicend"))
            {

                if(serviceBound){
                    mediaplayerservice.stop();
                    mediaplayerservice.play();
                    updateUIStartRun();

                }
            }
            else if(fromAlertStr.endsWith(".mp3"))
            {

                textView1.setText(fromAlertStr);
            }
            else if(fromAlertStr.equals("HORIZONTAL"))
            {
                if(mediaplayerservice.isTimerRunning()&&serviceBound){
                    Toast.makeText(getApplicationContext(),"Pause Player",Toast.LENGTH_LONG).show();
                    mediaplayerservice.pause();
                    updateUIPauseRun();
                    mediaplayerservice.foreground();
                }
            }
            else if(fromAlertStr.equals("VERTICAL"))
            {
                if (!mediaplayerservice.isTimerRunning()&&serviceBound) {
                    Toast.makeText(getApplicationContext(),"Start Player",Toast.LENGTH_LONG).show();
                    mediaplayerservice.play();
                    updateUIStartRun();
                    updateUITimer();
                    mediaplayerservice.foreground(); //notification
                }

            }
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bStart = (Button)findViewById(R.id.buttonPlay);
        bPause = (Button)findViewById(R.id.buttonPause);
        bStop  = (Button)findViewById(R.id.buttonStop);

        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);

        IntentFilter filter = new IntentFilter();
        filter.addAction("receivefromService");
        registerReceiver(mAlertReceiver, filter);

    }
    @Override
    protected void onStart() {
        //System.out.println("On Start");
        super.onStart();
        Intent i = new Intent(this, MediaPlayerService.class);
        startService(i);
        bindService(i, mConnection, 0);
        i.setAction(MediaPlayerService.ACTION_START);
    }
    protected void onDestroy() {
        //System.out.println("On Destroy");
        super.onDestroy();
        unregisterReceiver(mAlertReceiver);
        if(serviceBound) {
            exitPlayer();
        }

    }
    @Override
    protected void onStop() {
        //System.out.println("ON STOP ");
        super.onStop();
        if (serviceBound) {
            if (mediaplayerservice.isTimerRunning()) {
                updateUIStartRun();
                mediaplayerservice.foreground();
            }
        }
    }
    public void onButtonClick(View view) {

        Intent intent = new Intent(MainActivity.this, MediaPlayerService.class);
        switch (view.getId()) {
            case R.id.buttonPlay:
                if (!mediaplayerservice.isTimerRunning()&&serviceBound) {
                    Toast.makeText(getApplicationContext(),"Start Player",Toast.LENGTH_LONG).show();
                    mediaplayerservice.play();
                    updateUIStartRun();
                    mediaplayerservice.foreground(); //notification
                }
                break;
            case R.id.buttonPause:
                if(mediaplayerservice.isTimerRunning()&&serviceBound){
                    Toast.makeText(getApplicationContext(),"Pause Player",Toast.LENGTH_LONG).show();
                    mediaplayerservice.pause();
                    updateUIPauseRun();
                    mediaplayerservice.foreground();
                }
                break;
            case R.id.buttonStop:
                if(serviceBound){
                    Toast.makeText(getApplicationContext(),"Stop Player",Toast.LENGTH_LONG).show();
                    mediaplayerservice.stop();
                    updateUIStopRun();
                    mediaplayerservice.foreground();
                }
                break;
            case R.id.buttonExit:
                exitPlayer();
                break;
            case R.id.buttonG_Off:
                if(serviceBound){
                    Toast.makeText(getApplicationContext(),"Gesture Commands Deactivated",Toast.LENGTH_LONG).show();
                    mediaplayerservice.buttonOff();

                }
                break;
            case R.id.buttonG_On:
                if(serviceBound){
                    Toast.makeText(getApplicationContext(),"Gesture Commands Activated",Toast.LENGTH_LONG).show();
                    mediaplayerservice.buttonOn();
                }
                break;
        }
    }
    private void exitPlayer()
    {
        if(serviceBound){
            mediaplayerservice.stop();
            updateUIStopRun();
            if(mediaplayerservice.mp3!=null)
            {
                mediaplayerservice.mp3.release();
            }
            stopService(new Intent(this, MediaPlayerService.class));
        }
        unbindService(mConnection);
        serviceBound = false;
        moveTaskToBack(true);
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    private void updateUIStartRun() {
        mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
    }
    private void updateUIPauseRun() {
        mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
    }

    private void updateUIStopRun() {
        mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
        textView1.setText("(song info)");
        textView2.setText("(duration)");
    }
    private void updateUITimer() {
        if(serviceBound) {
            long a = mediaplayerservice.elapsedTime();
            int l = (int)a/3600;
            int m=((int) a/60)%60;
            int s=((int) a)%60;
            long a2 = mediaplayerservice.mp3.getDuration();
            int l2 = (int)a2/1000/3600;
            int m2=((int) a2/1000/60)%60;
            int s2=((int)a2/1000)%60;
            String str = String.format("%02d:%02d:%02d / %02d:%02d:%02d", l,m,s,l2,m2,s2);
            textView2.setText(str);

        }
    }
    private void updateNotification()
    {
        if(serviceBound)
        {
            mediaplayerservice.foreground();
        }
    }
    static class UIUpdateHandler extends Handler {

        private final static int UPDATE_RATE_MS = 1000;
        private final WeakReference<MainActivity> activity;
        UIUpdateHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message message) {
            if (MSG_UPDATE_TIME == message.what) {
                Log.d(TAG, "updating time");
                activity.get().updateUITimer();
                activity.get().updateNotification();

                sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_RATE_MS);
            }
        }
    }

}
