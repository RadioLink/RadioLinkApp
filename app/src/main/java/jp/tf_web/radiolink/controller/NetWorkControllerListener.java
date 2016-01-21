package jp.tf_web.radiolink.controller;

/**
 * Created by furukawanobuyuki on 2016/01/21.
 */
public interface NetWorkControllerListener {

    /** 接続状態が変更されたら通知される
     *
     * @param type
     * @param connected
     */
    void onConnectedChanged(int type, boolean connected);
}
