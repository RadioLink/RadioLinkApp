package jp.tf_web.radiolink.sensor;

import android.hardware.SensorEvent;

/** 照度センサーの値を利用するためのリスナー
 *
 * Created by furukawanobuyuki on 2015/12/22.
 */
public interface LightSensorManagerListener {
    /** 照度の値が変更されたら通知される
     *
     * @param event センサーのイベント
     * @param value 照度センサー値
     */
    void onLightSensorChanged(SensorEvent event, float value);
}
