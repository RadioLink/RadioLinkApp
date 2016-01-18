package jp.tf_web.radiolink.ncmb.gcm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 *
 * Created by furukawanobuyuki on 2016/01/19.
 */
public class GcmListenerServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "Gcm ServiceReceiver";

    private Context context;
    private GcmListenerServiceReceiverListener listener;

    /** コンストラクタ
     *
     * @param context
     * @param listener
     */
    public GcmListenerServiceReceiver(final Context context,final GcmListenerServiceReceiverListener listener){
        this.context = context;
        this.listener = listener;
    }

    /** レシーバーを登録する
     *
     */
    public void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GcmListenerService.ACTION_NAME);
        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        //各アクションに対する処理
        String action = intent.getAction();
        Log.d(TAG, "action:"+action);
        if(action.equals(GcmListenerService.ACTION_NAME)){
            String cmd = intent.getStringExtra(GcmListenerService.KEY_NAME_CMD);
            Log.d(TAG, "cmd:"+cmd);
            if(cmd.equals(GcmListenerService.CMD_CHANNEL_UPDATE)) {
                //チャンネル更新コマンドをリスナーに通知
                listener.onUpdateChannel();
            }
        }
    }
}
