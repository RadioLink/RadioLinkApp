package jp.tf_web.radiolink.sensor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

/** 照度センサーの値を取得
 *
 * Created by furukawanobuyuki on 2015/12/22.
 */
public class LightSensorManager  {
    private final String TAG = "LightSensorManager";

    //コンテキスト
    private Context context;

    //センサーマネージャ
    private SensorManager manager;

    //イベント通知先
    private LightSensorManagerListener listener;

    public LightSensorManager(final Context context,final LightSensorManagerListener listener){
        this.context = context;
        this.manager = (SensorManager)context.getSystemService(Activity.SENSOR_SERVICE);
        this.listener = listener;
    }

    /** 照度センサーのイベント取得を開始
     *
     */
    public void start(){
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_LIGHT);
        if(sensors.size() > 0) {
            this.manager.registerListener(sensorEventListener, sensors.get(0), SensorManager.SENSOR_DELAY_UI);
        }
    }

    /** 照度センサーのイベント取得を停止
     *
     */
    public void stop(){
        manager.unregisterListener(sensorEventListener);
    }

    private SensorEventListener sensorEventListener = new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
                //Log.d(TAG, "照度センサ値:" + event.values[0]);
                LightSensorManager.this.listener.onLightSensorChanged(event, event.values[0]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

}
