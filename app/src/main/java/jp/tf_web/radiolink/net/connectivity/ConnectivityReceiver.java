package jp.tf_web.radiolink.net.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import jp.tf_web.radiolink.net.NetWorkUtil;

/**ネットワーク接続状態の監視 レシーバー
 *
 * Created by furukawanobuyuki on 2016/01/21.
 */
public class ConnectivityReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityReceiver";

    //コンテキスト
    private Context context;

    //接続状態の通知
    private ConnectivityReceiverListener listener;

    /** コンストラクタ
     *
     * @param context
     * @param listener
     */
    public ConnectivityReceiver(final Context context, final ConnectivityReceiverListener listener){
        this.context = context;
        this.listener = listener;
    }

    /** レシーバーを登録する
     *
     */
    public void registerReceiver(){
        context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /** レシーバー登録を解除する
     *
     */
    public void unregisterReceiver(){
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");

        //ネットワーク接続状態を取得する
        int type = NetWorkUtil.isConnectedType(context);
        boolean connected = (type != -1)?true:false;

        //ネットワーク状態 connected が取得できたので リスナーに通知する
        this.listener.onConnectedChanged(type,connected);
    }


}
