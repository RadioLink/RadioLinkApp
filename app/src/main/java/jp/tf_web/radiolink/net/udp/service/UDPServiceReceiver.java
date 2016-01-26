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

    private Context context;

    //受信イベントの通知先リスナー
    private UDPServiceListener listener;

    //ステータス型
    private enum Status{
        //初期値
        INITIAL,
        //受信可能状態
        ACTIVE,
        RECEIVE,
    };

    //ステータス
    private Status status = Status.INITIAL;

    /** コンストラクタ
     *
     * @param context
     * @param listener
     */
    public UDPServiceReceiver(final Context context,UDPServiceListener listener){
        this.context = context;
        this.listener = listener;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /** レシーバーを登録する
     *
     */
    public void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UDPService.ACTION_NAME);
        context.registerReceiver(this, intentFilter);
    }

    /** レシーバー登録を解除する
     *
     */
    public void unregisterReceiver(){
        context.unregisterReceiver(this);
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

        if (cmd.equals(UDPService.CMD_STUN_BINDING)) {
            Log.d(TAG, "STUN_BINDING");
            setStatus(intent,Status.ACTIVE);
        }
        else if (cmd.equals(UDPService.CMD_RECEIVE)) {
            Log.d(TAG, "RECEIVE");
            //メッセージが届いた
            setStatus(intent, Status.RECEIVE);
        }
        else{
            return false;
        }

        return true;
    }

    /** STUN でのアドレスポート解決結果を受信した時
     *
     * @param intent
     */
    private void onStunBinding(final Intent intent){
        Bundle bundle = intent.getExtras();
        final InetSocketAddress publicSocketAddr = (InetSocketAddress) bundle.get(UDPService.KEY_NAME_PUBLIC_SOCKET_ADDR);
        if (publicSocketAddr != null) {
            Log.d(TAG, "public ip:" + publicSocketAddr.getAddress().getHostAddress() + " port:" + publicSocketAddr.getPort());
        }

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onStunBinding(publicSocketAddr);
            }
        });
    }

    /** メッセージが届いた
     *
     * @param intent
     */
    private void onReceive(final Intent intent){
        Log.d(TAG, "onReceive");
        Bundle bundle = intent.getExtras();
        final String cmd = (String) intent.getCharSequenceExtra(UDPService.KEY_NAME_CMD);
        //メッセージが届いた
        final byte[] buf = bundle.getByteArray(UDPService.KEY_NAME_RECEIVE_BUFFER);
        Log.d(TAG, "buf length:" + buf.length);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onReceive(cmd, buf);
            }
        });
    }

    /** ステータスを設定
     *
     */
    private void setStatus(final Intent intent,final Status status){
        switch (status){
            case ACTIVE:
                onStunBinding(intent);
                break;
            case RECEIVE:
                if(status != Status.INITIAL){
                    //STUN処理が済んで ACTIVE になるまではリスナーに受信パケットを通知しない
                    onReceive(intent);
                }
                break;
        }
        this.status = status;
    }
}
