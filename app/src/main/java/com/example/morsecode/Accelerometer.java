package com.example.morsecode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Accelerometer {

    public interface Listener{
        void onTranslation(float x, float y, float z);
    }

    private Listener listener;
    private Listener listener1;

    public void setListener(Listener l){
        listener = l;
    }
    public void setListener1(Listener ll){
        listener1 = ll;
    }

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;
    boolean on = false;
    private float gravity[] = new float[3];
    private float linear_acceleration[] = new float[3];
    private float alpha = 0.3f;

    Accelerometer(Context context){
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                if(listener1 != null){
                    listener1.onTranslation(event.values[0], event.values[1], event.values[2]);
                }

                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                if (listener != null){
                    listener.onTranslation(linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void register(){
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        on= true;
    }

    public void unregister(){
        sensorManager.unregisterListener(sensorEventListener);
        on = false;
    }



    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}