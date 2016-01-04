package jp.tf_web.radiolink.net.udp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.tf_web.radiolink.net.udp.service.*;
import jp.tf_web.radiolink.net.udp.service.UDPService;
import jp.tf_web.radiolink.net.udp.service.UDPServiceListener;

/** UDPServiceからの通知を受け取るブロードキャストレシーバー
 *
 * Created by furukawanobuyuki on 2015/12/08.
 */
public class UDPServiceReceiver extends BroadcastReceiver {
    private static String TAG = "UDPServiceReceiver";

    private ExecutorService executor;

    //受信イベントの通知先リスナー
    private UDPServiceListener listener;

    /** コンストラクタ
     *
     * @param listener
     */
    public UDPServiceReceiver(UDPServiceListener listener){
        this.listener = listener;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /** レシーバーを登録する
     *
     * @param context
     */
    public void registerReceiver(final Context context){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UDPService.ACTION_NAME);
        context.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //インテントで渡されたコマンドの処理を行う
        boolean isCmd = onCmdIntent(intent);
    }

    /** コマンドが届いたら
     *
     * @param intent
     */
    private boolean onCmdIntent(Intent intent) {
        Log.d(TAG, "onCmdIntent");

        if (intent == null) {
            return false;
        }

        final String cmd = (String) intent.getCharSequenceExtra(UDPService.KEY_NAME_CMD);
        Log.d(TAG, "cmd:" + cmd);
        if(cmd == null){
            return false;
        }
        Bundle bundle = intent.getExtras();
        if (cmd.equals(UDPService.CMD_RECEIVE)) {
            Log.d(TAG, "RECEIVE");
            //メッセージが届いた
            final byte[] buf = bundle.getByteArray(UDPService.KEY_NAME_RECEIVE_BUFFER);
            Log.d(TAG, "buf length:"+buf.length);
            //this.listener.onReceive(cmd, buf);
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onReceive(cmd, buf);
                }
            });
        }
        else if (cmd.equals(UDPService.CMD_STUN_BINDING)) {
            Log.d(TAG, "STUN_BINDING");
            final InetSocketAddress publicSocketAddr = (InetSocketAddress) bundle.get(UDPService.KEY_NAME_PUBLIC_SOCKET_ADDR);
            if (publicSocketAddr != null) {
                Log.d(TAG, "public ip:" + publicSocketAddr.getAddress().getHostAddress() + " port:" + publicSocketAddr.getPort());
            }
            //this.listener.onStunBinding(publicSocketAddr);

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onStunBinding(publicSocketAddr);
                }
            });
        }
        else{
            return false;
        }

        return true;
    }
}
