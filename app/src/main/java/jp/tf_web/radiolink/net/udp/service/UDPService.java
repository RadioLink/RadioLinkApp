package jp.tf_web.radiolink.net.udp.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import jp.tf_web.radiolink.util.ByteArrayUtil;

/** UDP サービス
 *
 * Created by furukawanobuyuki on 2015/12/08.
 */
public class UDPService extends Service {
    private static String TAG = "UDPService";

    //ACTION名
    public static String ACTION_NAME = "ACTION_UDP_SERVICE";

    //各コマンド
    public static final String KEY_NAME_CMD = "cmd";

    public static String KEY_NAME_SEND_BUFFER    = "send_buffer";
    public static String KEY_NAME_SEND_HOST      = "send_host";
    public static String KEY_NAME_SEND_PORT      = "send_port";
    public static String KEY_NAME_CONNECT_HOST   = "connect_host";
    public static String KEY_NAME_CONNECT_PORT   = "connect_port";
    public static String KEY_NAME_BIND_ADDRESS   = "bind_address";
    public static String KEY_NAME_BIND_PORT      = "bind_port";
    public static String KEY_NAME_RECEIVE_BUFFER = "receive_buffer";
    public static String KEY_NAME_PUBLIC_SOCKET_ADDR = "public_socket_addr";

    public static String CMD_START      = "start";
    public static String CMD_STOP       = "stop";
    public static String CMD_SEND       = "send";
    public static String CMD_RECEIVE    = "receive";
    public static String CMD_STUN_BINDING = "stun_binding";

    //各処理をする Executor
    private ExecutorService executor;

    //UDPからの受信
    private UDPReceiver udpReceiver;

    /** UDPServiceにコマンドを送信
     *
     * @param cmd コマンド種類
     */
    public static void sendCmd(final Context context,final String cmd){
        sendCmd(context, cmd, null);
    }

    /** UDPServiceにコマンドを送信
     *
     * @param cmd コマンド種類
     * @param params 送信コマンドパラメータ
     */
    public static void sendCmd(final Context context,final String cmd,final Map<String,Object> params){
        Intent intent = new Intent(context,UDPService.class);
        intent.putExtra(KEY_NAME_CMD, cmd);
        if(params != null){
            if(cmd.equals(CMD_START)){
                intent.putExtra(KEY_NAME_BIND_ADDRESS, (String) params.get(KEY_NAME_BIND_ADDRESS));
                intent.putExtra(KEY_NAME_BIND_PORT, (Integer) params.get(KEY_NAME_BIND_PORT));
            }
            else if(cmd.equals(CMD_SEND)){
                Log.d(TAG, "params:" + params);
                intent.putExtra(KEY_NAME_CONNECT_HOST, (String) params.get(KEY_NAME_CONNECT_HOST));
                intent.putExtra(KEY_NAME_CONNECT_PORT, (Integer) params.get(KEY_NAME_CONNECT_PORT));

                intent.putExtra(KEY_NAME_SEND_HOST, (String) params.get(KEY_NAME_SEND_HOST));
                intent.putExtra(KEY_NAME_SEND_PORT, (Integer) params.get(KEY_NAME_SEND_PORT));

                byte[] buf = (byte[])params.get(KEY_NAME_SEND_BUFFER);
                if(buf != null){
                    intent.putExtra(KEY_NAME_SEND_BUFFER, buf);
                }
            }
        }
        context.startService(intent);
    }


    /** UDPで何か届いた場合ブロードキャストレシーバーに通知
     *
     * @param context
     * @param cmd
     * @param params
     */
    public static void sendOnReceive(final Context context,final String cmd,final Map<String,Object> params){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_NAME);
        broadcastIntent.putExtra(KEY_NAME_CMD, cmd);
        if(params != null){
            if(cmd.equals(CMD_RECEIVE)){
                byte[] buf = (byte[])params.get(KEY_NAME_RECEIVE_BUFFER);
                if(buf != null) {
                    broadcastIntent.putExtra(KEY_NAME_RECEIVE_BUFFER, buf);
                }
            }
            else if(cmd.equals(CMD_STUN_BINDING)){
                InetSocketAddress publicSocketAddr = (InetSocketAddress) params.get(KEY_NAME_PUBLIC_SOCKET_ADDR);
                broadcastIntent.putExtra(KEY_NAME_PUBLIC_SOCKET_ADDR, publicSocketAddr);
            }
        }
        context.sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "Service has been started.", Toast.LENGTH_SHORT).show();

        //インテントで渡されたコマンドの処理を行う
        boolean isCmd = onCmdIntent(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "Service has been terminated.", Toast.LENGTH_SHORT).show();

        if(executor != null){
            executor.shutdownNow();
            executor = null;
        }
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

        final String cmd = (String) intent.getCharSequenceExtra(KEY_NAME_CMD);
        Log.d(TAG, "cmd:" + cmd);
        if(cmd == null){
            return false;
        }

        final Bundle bundle = intent.getExtras();
        if (cmd.equals(CMD_START)) {
            Log.d(TAG, "START");
            if(executor == null) executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    //初期化処理
                    final String addr = bundle.getString(UDPService.KEY_NAME_BIND_ADDRESS);
                    final int port = bundle.getInt(UDPService.KEY_NAME_BIND_PORT);
                    start(addr,port);
                }
            });
        }
        else if (cmd.equals(CMD_STOP)) {
            Log.d(TAG, "STOP");
            if(executor == null) executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    //停止処理
                    stop();
                }
            });
        }
        else if (cmd.equals(CMD_SEND)) {
            Log.d(TAG, "SEND");
            //UDPで送信処理をする
            if(executor == null) executor = Executors.newSingleThreadExecutor();

            if(udpReceiver != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        //接続先
                        final String cHost = bundle.getString(UDPService.KEY_NAME_CONNECT_HOST);
                        final int cPort = bundle.getInt(UDPService.KEY_NAME_CONNECT_PORT);
                        final InetSocketAddress connectAddr = new InetSocketAddress(cHost, cPort);

                        //送信先
                        final String host = bundle.getString(UDPService.KEY_NAME_SEND_HOST);
                        final int port = bundle.getInt(UDPService.KEY_NAME_SEND_PORT);
                        final InetSocketAddress addr = new InetSocketAddress(host, port);

                        final byte[] buf = bundle.getByteArray(UDPService.KEY_NAME_SEND_BUFFER);
                        udpReceiver.writeUDP(connectAddr, addr, buf);
                    }
                });
            }
        }
        else if (cmd.equals(CMD_RECEIVE)) {
            Log.d(TAG, "RECEIVE");
            //パケットを受信
            if(executor == null) executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    //ブロードキャストレシーバーに受信バッファーを送信
                    final byte[] buf = bundle.getByteArray(UDPService.KEY_NAME_RECEIVE_BUFFER);
                    udpReceive(cmd,buf);
                }
            });
        }
        else{
            //コマンド以外
            return false;
        }
        return true;
    }

    /** レシーバーにデータを送信
     *
     */
    private void udpReceive(final String cmd,final byte[] buf){
        Map<String,Object> params = new HashMap<String,Object>(){
            {
                put(UDPService.KEY_NAME_RECEIVE_BUFFER, buf);
            }
        };
        sendOnReceive(getApplicationContext(),cmd,params);
    }

    /** 初期化処理など
     *
     */
    private void start(final String bindAddr,final int bindPort){
        Log.d(TAG, "start()");

        //受信待ち
        if(udpReceiver == null) {
            udpReceiver = new UDPReceiver(bindAddr,bindPort,this.udpReceiverListener);
        }
    }

    /** 停止処理など
     *
     */
    private void stop(){
        Log.d(TAG, "stop()");

        //受信 待ち 終了
        if(udpReceiver != null) {
            udpReceiver.close();
            udpReceiver = null;
        }

        //サービス停止
        stopSelf();
    }

    /** UDPからの受信イベント
     *
     */
    private UDPReceiverListener udpReceiverListener = new UDPReceiverListener(){

        /** STUN Binding 結果を通知
         *
         * @param publicSocketAddr パブリックIP,ポート
         */
        @Override
        public void onStunBinding(final InetSocketAddress publicSocketAddr){
            Log.d(TAG, "onStunBinding");
            if (publicSocketAddr != null) {
                Log.d(TAG, "public ip:" + publicSocketAddr.getAddress().getHostAddress() + " port:" + publicSocketAddr.getPort());
            }

            //ブロードキャストレシーバーに通知
            Map<String,Object> params = new HashMap<String,Object>(){
                {
                    put(UDPService.KEY_NAME_PUBLIC_SOCKET_ADDR, publicSocketAddr);
                }
            };
            sendOnReceive(getApplicationContext(),CMD_STUN_BINDING,params);
        }

        /** パケットを受信した時
         *
         * @param ctx
         * @param packet
         */
        @Override
        public void onReceive(ChannelHandlerContext ctx, DatagramPacket packet, byte[] data) {
            InetSocketAddress addr = packet.sender();
            Log.d(TAG, "sender addr:" + addr);

            if(data != null){
                //受信データをログに表示
                Log.d(TAG, "data length:" + data.length + " " + ByteArrayUtil.toHexString(data));

                //ブロードキャストレシーバーに受信データを送信
                Map<String,Object> params = new HashMap<String,Object>();
                params.put(UDPService.KEY_NAME_RECEIVE_BUFFER, data);
                UDPService.sendOnReceive(getApplicationContext(), UDPService.CMD_RECEIVE, params);
            }
        }
    };
}
