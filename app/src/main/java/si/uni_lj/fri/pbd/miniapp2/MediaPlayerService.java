package si.uni_lj.fri.pbd.miniapp2;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;


public class MediaPlayerService extends Service
{
    int currentPosition = 0;
    private int[] playlist;
    String[] list;
    private static final String TAG = MediaPlayerService.class.getSimpleName();
    public static final String ACTION_STOP = "stop_service";
    public static final String ACTION_START = "start_service";
    public static final String ACTION_PAUSE = "pause_service";
    public static final String ACTION_EXIT = "exit_service";
    private static final String channelID = "background_player";
    private static final int NOTIFICATION_ID = 1;
    int pos = 0;
    public boolean isMusicRunning;
    private final IBinder mBinder = new MediaPlayerServiceBinder();
    MediaPlayer mp3;
    int flag=0; //0:stop 1:pause
    AccelerationService accelerationService;
    private boolean serviceBound;

    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service bound");
            AccelerationService.AccelerationServiceBinder binder = (AccelerationService.AccelerationServiceBinder) iBinder;
            accelerationService = binder.getService();
            serviceBound = true;
        }
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnect");
            serviceBound = false;
        }
    };

    public class MediaPlayerServiceBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    private BroadcastReceiver AccelerationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String fromAlertStr = intent.getStringExtra("message");

            if(fromAlertStr.equals("HORIZONTAL"))
            {
                sendMessage("HORIZONTAL");
            }
            else if(fromAlertStr.equals("VERTICAL"))
            {
                sendMessage("VERTICAL");
            }
        }
    };

    public void onCreate()
    {
        isMusicRunning = false;
        settingplaylist();
        super.onCreate();
        createNotificationChannel();

        IntentFilter filter = new IntentFilter();
        filter.addAction("receivefromAccelerationService");
        registerReceiver(AccelerationReceiver, filter);
    }
    public void settingplaylist()
    {
        playlist = new int[3];
        list = new String[3];
        playlist[2] = R.raw.music;
        list[2] = "music.mp3";
        playlist[1] = R.raw.music2;
        list[1] = "music2.mp3";
        playlist[0] = R.raw.music3;
        list[0] = "music3.mp3";
    }
    public void foreground() {
        startForeground(NOTIFICATION_ID, createNotification());
    }
    public void background() {
        stopForeground(true);
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent.getAction()==ACTION_START)
        {
            play();
            foreground();
        }
        else if(intent.getAction()==ACTION_EXIT)
        {
            sendMessage("finish");
        }
        else if(intent.getAction()==ACTION_STOP)
        {
            //System.out.println("action_stop");
            stop();
            foreground();
        }
        else if(intent.getAction()==ACTION_PAUSE)
        {
            //System.out.println("action pause");
            pause();
            foreground();
        }
        return Service.START_STICKY;
    }
    private void sendMessage(String message) {
        Intent intent = new Intent("receivefromService");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }
    public void play()
    {
        foreground();
        if (!isTimerRunning()&&flag==0)
        {
            isMusicRunning = true;
            mp3 = MediaPlayer.create(this, playlist[currentPosition]);
            mp3.setLooping(false);
            mp3.start();
            mp3.setOnCompletionListener(completionListener);
            sendMessage(list[currentPosition]);

        }
        else if(!isTimerRunning()&&flag==1)
        {
            mp3.seekTo(pos);
            mp3.start();
            isMusicRunning = true;
            mp3.setOnCompletionListener(completionListener);
        }
    }

    public void pause()
    {
        if (isTimerRunning()) {
            pos = mp3.getCurrentPosition();
            isMusicRunning = false;
            mp3.pause();
            flag = 1;
        }
    }

    public void stop() {
        if (isTimerRunning()) {
            isMusicRunning = false;
            mp3.stop();
            flag = 0;
        }
        else if(!isTimerRunning()&&flag==1) { //pause
            mp3.stop();
            flag = 0;
        }
    }
    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener()
    {
        public void onCompletion(MediaPlayer mp3)
        {
            mp3.stop();
            //mp3.release();
            currentPosition++;
            if(currentPosition>=playlist.length)
            {
                currentPosition = 0;
            }
            sendMessage("musicend");
        }
    };
    public void buttonOff()
    {
        if(serviceBound) {
            stopService(new Intent(this, AccelerationService.class));
            unbindService(mConnection);
            serviceBound = false;
        }
    }
    public void buttonOn()
    {
        Intent i = new Intent(this, AccelerationService.class);
        startService(i);
        bindService(i, mConnection, 0);
        i.setAction(AccelerationService.ACTION_START);
    }

    public boolean isTimerRunning() {
        return isMusicRunning;
    }

    private Notification createNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setContentTitle(!isTimerRunning()&&flag==0? "":list[currentPosition])
                .setContentText(isTimerRunning()||(!isTimerRunning()&&flag==1)?updateNotification():"")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setChannelId(channelID);

        Intent actionIntent = new Intent(this, MediaPlayerService.class);
        actionIntent.setAction(ACTION_STOP);
        PendingIntent actionPendingIntent = PendingIntent.getService(this, 0,
                actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent actionIntent3 = new Intent(this, MediaPlayerService.class);
        actionIntent3.setAction(isTimerRunning()? ACTION_PAUSE : ACTION_START);
        PendingIntent actionPendingIntent3 = PendingIntent.getService(this, 0,
                actionIntent3, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent actionIntent4 = new Intent(this, MediaPlayerService.class);
        actionIntent4.setAction(ACTION_EXIT);
        PendingIntent actionPendingIntent4 = PendingIntent.getService(this, 0,
                actionIntent4, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent).addAction(android.R.drawable.ic_media_pause, isTimerRunning()? "PAUSE":"START",
                actionPendingIntent3)
                .addAction(android.R.drawable.ic_media_pause, "STOP", actionPendingIntent)
                .addAction(android.R.drawable.ic_media_pause,"EXIT",actionPendingIntent4);
        return builder.build();
    }

    private String updateNotification(){
        long a = elapsedTime();
        int l = (int)a/3600;
        int m=((int) a/60)%60;
        int s=((int) a)%60;
        long a2 = mp3.getDuration();
        int l2 = (int)a2/1000/3600;
        int m2=((int) a2/1000/60)%60;
        int s2=((int)a2/1000)%60;
        String str = String.format("%02d:%02d:%02d / %02d:%02d:%02d", l,m,s,l2,m2,s2);
        return str;
    }

    private void createNotificationChannel() {
        System.out.println("version : " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    public long elapsedTime() {

        return mp3.getCurrentPosition()/1000;
    }


}