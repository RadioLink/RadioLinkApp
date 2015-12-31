package jp.tf_web.radiolink.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/** MEDIA_BUTTON を押された時のIntent受け取り
 *
 * Created by furukawanobuyuki on 2015/12/28.
 */
public class MediaButtonReceiver extends BroadcastReceiver {
    private static String TAG = "MediaButtonReceiver";

    public static String ACTION_CLICK_MEDIA_BUTTON = "jp.tf_web.radiolink.bluetooth.CLICK_MEDIA_BUTTON";

    //コンテキスト
    private Context context;

    //イベントリスナー
    private MediaButtonReceiverListener listener;

    /** MEDIA_BUTTONが押されたことをブロードキャストを実行するユーテリティ
     *
     */
    public static void onClickMediaButton(Context context){
        Intent intent = new Intent(ACTION_CLICK_MEDIA_BUTTON);
        context.sendBroadcast(intent);
    }

    /** ブロードキャストを受け取る為に登録
     *
     */
    public void registerReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaButtonReceiver.ACTION_CLICK_MEDIA_BUTTON);
        this.context.registerReceiver(this, filter);
    }

    /** ブロードキャストを受け取る為の登録解除
     *
     */
    public void unregisterReceiver(){
        try {
            this.context.unregisterReceiver(this);
        }
        catch (IllegalArgumentException e){

        }
    }

    /** コンストラクタ
     *
     * @param context コンテキスト
     * @param listener イベントリスナー
     */
    public MediaButtonReceiver(Context context,final MediaButtonReceiverListener listener){
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive()");
        String action = intent.getAction();
        if(action.equals(ACTION_CLICK_MEDIA_BUTTON)){
            //MEDIA_BUTTON ボタンが押された事をリスナーに通知する
            listener.onClickMediaButton();
        }
    }
}
