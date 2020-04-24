package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class AccelerationService extends Service implements SensorEventListener
{
    private SensorManager mSensorManager = null;
    private int noise_threshold = 5;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;
    private static final int DATA_X =0;
    private static final int DATA_Y = 1;
    private static final int DATA_Z = 2;
    private long lastTime;
    public static final String ACTION_START = "start_service";
    public static final int IDLE = 0;
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;
    private Sensor accelerormeterSensor;

    private final IBinder mBinder = new AccelerationServiceBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    public class AccelerationServiceBinder extends Binder {
        AccelerationService getService() {
            return AccelerationService.this;
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);
            if (gabOfTime > 500) {
                lastTime = currentTime;
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                int command = IDLE;
                if (Math.abs(lastX-x) <= noise_threshold) {
                    // count it as a noise(no change)

                }
                else if(Math.abs(lastX-x)>Math.abs(lastZ-z))//pause
                {
                    command = HORIZONTAL;
                }
                else if(Math.abs(lastZ-z)>Math.abs(lastX-x))//play
                {
                    command = VERTICAL;
                }

                if(command != IDLE) {
                    //update MediaPlayerService
                    if(command == VERTICAL)//play
                    {
                        sendMessage("VERTICAL");
                    }
                    if(command == HORIZONTAL)//pause
                    {
                        sendMessage("HORIZONTAL");
                    }
                }

                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
            }
        }
    }
    private void sendMessage(String message) {
        Intent intent = new Intent("receivefromAccelerationService");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public void onCreate()
    {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerormeterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        super.onCreate();
        System.out.println("AccelerationService start");
    }
    @Override
    public void onDestroy()
    {
        System.out.println("AccelerationService end");
        if(mSensorManager!=null)
        {
            mSensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        if(accelerormeterSensor!=null)
            mSensorManager.registerListener(this,accelerormeterSensor, mSensorManager.SENSOR_DELAY_GAME);
        return Service.START_STICKY;
    }
}