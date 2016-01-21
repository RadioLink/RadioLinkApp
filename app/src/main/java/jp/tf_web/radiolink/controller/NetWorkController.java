package jp.tf_web.radiolink.controller;

import android.content.Context;
import android.util.Log;

import jp.tf_web.radiolink.net.connectivity.ConnectivityReceiver;
import jp.tf_web.radiolink.net.connectivity.ConnectivityReceiverListener;

/** ネットワーク関係の処理をまとめたクラス
 *
 * Created by furukawanobuyuki on 2016/01/21.
 */
public class NetWorkController {
    private static final String TAG = "NetWorkController";

    //コンテキスト
    private Context context;

    //リスナー
    private NetWorkControllerListener listener;

    //接続状態の監視レシーバー
    private ConnectivityReceiver connectivityActionReceiver;

    /** コンストラクタ
     *
     * @param context
     * @param listener
     */
    public NetWorkController(final Context context,final NetWorkControllerListener listener){
        Log.d(TAG,"NetWorkController");
        this.context = context;
        this.listener = listener;
    }

    /** 各 初期化処理
     *
     */
    private void initialize(){
        //接続状態の監視レシーバー
        if(this.connectivityActionReceiver != null){
            Log.w(TAG,"connectivityActionReceiver is not null");
        }
        else{
            this.connectivityActionReceiver = new ConnectivityReceiver(context,connectivityActionReceiverListener);
        }
    }

    /** 開始
     *
     */
    public void start(){
        Log.d(TAG,"start()");
        //初期化処理
        initialize();
        if(this.connectivityActionReceiver != null) {
            //ネットワーク接続の監視を開始
            this.connectivityActionReceiver.registerReceiver();
        }
    }

    /** 停止
     *
     */
    public void stop(){
        Log.d(TAG,"stop()");
        if(this.connectivityActionReceiver != null){
            //ネットワーク接続の監視を止める
            this.connectivityActionReceiver.unregisterReceiver();
            this.connectivityActionReceiver = null;
        }
        else{
            Log.w(TAG, "connectivityActionReceiver is null");
            return;
        }
    }

    /** 利用停止
     *
     */
    public void destroy(){
        stop();
    }

    //接続状態の監視レシーバー リスナー
    private ConnectivityReceiverListener connectivityActionReceiverListener = new ConnectivityReceiverListener(){

        /** 接続状態が変更されたら通知される
         *
         * @param type
         * @param connected
         */
        @Override
        public void onConnectedChanged(int type,boolean connected) {
            Log.d(TAG, "onConnectedChanged "+connected);
            //リスナーに通知
            listener.onConnectedChanged( type, connected);
        }
    };
}
