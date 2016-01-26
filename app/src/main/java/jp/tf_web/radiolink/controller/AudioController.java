package jp.tf_web.radiolink.controller;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.audio.OpusManager;
import jp.tf_web.radiolink.audio.RecordManager;
import jp.tf_web.radiolink.audio.RecordManagerListener;
import jp.tf_web.radiolink.audio.AudioDeviceManager;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.ChannelUser;
import jp.tf_web.radiolink.net.NetWorkUtil;
import jp.tf_web.radiolink.net.protocol.PacketUtil;
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.udp.service.UDPService;
import jp.tf_web.radiolink.net.udp.service.UDPServiceListener;
import jp.tf_web.radiolink.net.udp.service.UDPServiceReceiver;

/** オーディオ,通信処理をまとめたクラス
 *
 * Created by furukawanobuyuki on 2016/01/09.
 */
public class AudioController {
    private static String TAG = "AudioController";

    //Bluetoothヘッドセットへの接続等
    private AudioDeviceManager audioDeviceManager;

    //リスナー
    private AudioControllerListener listener;

    //リスナー通知処理をする
    private ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();

    //コンテキスト
    private Context context;

    //録音 サンプリングレート
    private int sampleRateInHz;

    //録音 バッファサイズ
    private int bufSize;

    //PacketデコードやOPUSデコードする為のスレッド
    private ExecutorService executor = Executors.newCachedThreadPool();

    //Opusデコード,エンコード
    private OpusManager opusManager;

    //録音関連の処理をするクラス
    private RecordManager recordManager;

    //UDPで受信したパケットを受け取るレシーバー
    private UDPServiceReceiver udpServiceReceiver;

    //ローカルIP,ポート
    private InetSocketAddress localAddr;

    //パブリックIP,ポート
    private InetSocketAddress publicAddr;

    //選択中のチャンネル
    private Channel activeChannel;

    //受信パケットのリスト
    private List<Packet> receivePacketList = new ArrayList<Packet>();

    //再生処理をする為のスレッド
    private WritePacketThread writePacketThread;


    /** コンストラクタ
     *
     * @param context コンテキスト
     * @param sampleRateInHz サンプリングレート
     * @param bufSize 録音バッファサイズ
     * @param listener イベント通知リスナー
     */
    public AudioController(final Context context, final int sampleRateInHz, final int bufSize,final AudioControllerListener listener){
        this.context = context;
        this.sampleRateInHz = sampleRateInHz;
        this.bufSize = bufSize;

        this.listener = listener;
    }

    /** 初期化処理
     *
     * @param audioStream オーディオストリーム種類
     */
    public void initialize(final int audioStream) {

        //Bluetoothヘッドセットを利用する
        if(audioDeviceManager == null) {
            audioDeviceManager = new AudioDeviceManager(this.context);
        }

        //OPUSデコード,エンコード
        if(opusManager == null) {
            opusManager = new OpusManager(Config.SAMPLE_RATE_IN_HZ,
                    1,
                    Config.OPUS_FRAME_SIZE,
                    Config.OPUS_OUTPUT_BITRATE_BPS);
        }

        //再生処理の初期化
        if(writePacketThread == null) {
            //再生処理をするスレッドを初期化
            writePacketThread = new WritePacketThread(this.context, opusManager);
        }

        //録音関連処理の初期化
        if(recordManager == null) {
            recordManager = RecordManager.getInstance(this.context, this.sampleRateInHz, this.bufSize, recordManagerListener);
        }

        //UDPServiceからの受信を受け取るレシーバー
        if(udpServiceReceiver == null) {
            udpServiceReceiver = new UDPServiceReceiver(context,udpServiceListener);
            udpServiceReceiver.registerReceiver();
        }
    }

    /** 再生,録音 開始
     *
     * @param audioStream
     */
    public void start(final int audioStream){
        initialize(audioStream);

        //Bluetoothヘッドセットがあればそれを使う
        audioDeviceManager.startVoiceRecognition();

        //ローカルIPアドレスを取得
        NetWorkUtil.getLocalIpv4Address(new NetWorkUtil.GetLocalIpv4AddressListener() {
            @Override
            public void onResult(final String address) {
                Log.d(TAG, "local IpAddress:" + address);

                localAddr = new InetSocketAddress(address, Config.BIND_PORT);

                //UDPサービス START
                Map<String, Object> params = new HashMap<String, Object>() {
                    {
                        put(UDPService.KEY_NAME_BIND_ADDRESS, address);
                        put(UDPService.KEY_NAME_BIND_PORT, Integer.valueOf(Config.BIND_PORT));
                    }
                };
                UDPService.sendCmd(AudioController.this.context, UDPService.CMD_START, params);
            }
        });

        //録音
        recordManager.start();
    }

    /** 再生,録音 停止
     *
     */
    public void stop(){
        activeChannel = null;

        //Bluetoothヘッドセットから切断
        if(audioDeviceManager != null) {
            audioDeviceManager.stopVoiceRecognition();
        }

        if(writePacketThread != null) {
            writePacketThread.stopRunning();
            writePacketThread = null;
        }

        if(recordManager != null) {
            recordManager.stop();
            recordManager = null;
        }
    }

    /** 利用停止
     *
     */
    public void destroy(){
        stop();
        if(opusManager != null){
            try{
                opusManager.close();
            }
            catch ( IOException e ){
                Log.e( TAG,"Could not close or flush stream", e );
            }
        }
    }

    /** パブリックIP,ポート
     *
     * @return
     */
    public InetSocketAddress getPublicSocketAddress(){
        return this.publicAddr;
    }

    /** ローカルIP,ポート
     *
     * @return
     */
    public InetSocketAddress getLocalSocketAddress(){
        return this.localAddr;
    }

    /** アクティブチャンネルを設定
     *
     * @param activeChannel
     */
    public void setActiveChannel(Channel activeChannel){
        this.activeChannel = activeChannel;
    }

    /** UDPサービスに byte[]データを送信
     *
     * @param src  byte[]データ
     */
    private void udpServiceSendByteArray(final byte[] src){
        Log.d(TAG, "udpServiceSendByteArray");
        if(activeChannel == null) return;
        if((activeChannel.getChannelUserList() == null)||(activeChannel.getChannelUserList().size() == 0)){
            Log.d(TAG,"activeChannel.getChannelUserList() is null");
            return;
        }

        if(publicAddr == null) {
            Log.d(TAG, "publicAddr is null");
            return;
        }

        //データを送信
        String publicIp = publicAddr.getAddress().getHostAddress();
        int publicPort = publicAddr.getPort();
        for(ChannelUser cu:activeChannel.getChannelUserList()){
            Log.d(TAG,"cu:"+cu.getUser().getUserName());
            //自分以外に送信
            if(cu.publicSocketAddress == null){
                Log.d(TAG,"cu.publicSocketAddress is null");
                continue;
            }
            String ip = cu.publicSocketAddress.getAddress().getHostAddress();
            int port = cu.publicSocketAddress.getPort();
            //Log.d(TAG," ip:"+ip+":"+port+" publicIp:"+publicIp+":"+publicPort);
            if((ip.equals(publicIp)) && (publicPort == port)){
                continue;
            }

            //送信
            udpServiceSendByteArray(publicAddr,cu,src);
        }
    }

    /** UDPサービスに byte[]データを送信
     *
     * @param currentUserPublicAddr 自分のパブリックIP,ポート
     * @param channelUser チャンネルユーザー
     * @param src  byte[]データ
     */
    private void udpServiceSendByteArray(final InetSocketAddress currentUserPublicAddr,final ChannelUser channelUser,final byte[] src){
        //データを送信
        Map<String,Object> params = new HashMap<String,Object>(){
            {
                //送信先ローカルIPポート
                put(UDPService.KEY_NAME_SEND_HOST, channelUser.localSocketAddress.getAddress().getHostAddress());
                put(UDPService.KEY_NAME_SEND_PORT, Integer.valueOf(channelUser.localSocketAddress.getPort()));

                put(UDPService.KEY_NAME_SEND_BUFFER, src);
            }
        };

        //TODO: 自分のパブリックIPと送信先パブリックIPが一致している場合ローカルIPに送信する
        //自分のパブリックIP
        InetAddress currentPublicAddr = currentUserPublicAddr.getAddress();
        //送信先パブリックIP
        InetAddress cuPublicAddr = channelUser.publicSocketAddress.getAddress();
        if(currentPublicAddr.equals(cuPublicAddr) == false){
            //一致してなかった場合
            //送信先が 別ネットワークにいるので パブリックIP,ポート を設定
            params.put(UDPService.KEY_NAME_CONNECT_HOST, cuPublicAddr.getHostAddress());
            params.put(UDPService.KEY_NAME_CONNECT_PORT, Integer.valueOf(channelUser.publicSocketAddress.getPort()));
        }
        else{
            //送信先が同一ネットワークにあるので 送信先ローカルIP,ポートを設定
            params.put(UDPService.KEY_NAME_CONNECT_HOST, channelUser.localSocketAddress.getAddress().getHostAddress());
            params.put(UDPService.KEY_NAME_CONNECT_PORT, Integer.valueOf(channelUser.localSocketAddress.getPort()));
        }

        UDPService.sendCmd(this.context, UDPService.CMD_SEND, params);
    }

    /** 録音結果を受け取るリスナー
     *
     */
    private RecordManagerListener recordManagerListener = new RecordManagerListener(){

        /** 録音結果の通知
         *
         * @param data 録音したPCMデータ
         * @param size 録音データのサイズ
         * @param volume 録音ボリュームの最大値
         */
        @Override
        public void onAudioRecord(final byte[] data,final int size,final short volume) {
            if(activeChannel == null) return;

            //OPUSエンコード
            byte[] opus = opusManager.encode(data);
            if((opus != null) && (opus.length > 1)){
                //パケットにまとめてから UDPでユーザーに送信する
                final Packet packet = PacketUtil.getInstance().createPacket();
                packet.addPayload(opus);

                //リスナーにパケットとボリュームを通知
                listenerExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.onAudioRecord(packet, volume);
                    }
                });

                //volume閾値での簡易VAD
                if(volume > Config.VOLUME_THRESHOLD) {
                    udpServiceSendByteArray(packet.toByteArray());
                }
            }
        }
    };

    //ブロードキャストレシーバーから受信したイベントを受け取る
    private UDPServiceListener udpServiceListener = new UDPServiceListener(){

        /** STUN Binding 結果を通知
         *
         * @param publicSocketAddr パブリックIP,ポート
         */
        @Override
        public void onStunBinding(final InetSocketAddress publicSocketAddr){
            Log.d(TAG, "onStunBinding");
            //パブリックIP,ポートを保存
            publicAddr = publicSocketAddr;

            //リスナーに通知
            listener.onActive();
        }

        /** UDPServiceから届いいた メッセージを受信した場合
         *
         * @param cmd コマンド文字列
         * @param data 受信した byte[]
         */
        @Override
        public void onReceive(final String cmd, final byte[] data) {
            Log.d(TAG, "onReceive cmd:" + cmd);
            if(activeChannel == null) return;

            if(cmd == null){
                return;
            }

            if (cmd.equals(UDPService.CMD_RECEIVE)) {
                if(data == null){
                    Log.d(TAG, "buf is null");
                    return;
                }
                //受信データのサイズ
                Log.i(TAG, "receive data size:"+data.length);

                //再生を開始
                writePacketThread.startRunning(AudioManager.STREAM_MUSIC);

                //onReceiveを成る可く早く終わらせたいので 別スレッドでエンコード等を実行

                //遅いと遅延が発生する
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        //バイト配列からパケットを生成
                        final Packet packet = PacketUtil.getInstance().createPacket(data);

                        //タイムスタンプが古い場合は破棄
                        //送信元の時計とのズレがあるとあるとパケットが破棄される数が増える
                        long ct = System.currentTimeMillis();
                        long t = ct - packet.getHeader().getTimeStamp();
                        Log.d(TAG, "t:" + t + " ct:" + ct + " ht:" + packet.getHeader().getTimeStamp());
                        if( t > Config.PACKET_TIMESTAMP_EXPIRE_MSEC){
                            Log.w(TAG,"timestamp expire:"+t);
                            return;
                        }

                        //リスナーに通知
                        listenerExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                listener.onReceive(packet);
                            }
                        });

                        //パケットリストに追加
                        writePacketThread.addPacket(packet);
                    }
                });

            }
        }
    };

    /** オーディオデバイスの設定
     *
     * @param mode
     */
    public void setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE mode){
        audioDeviceManager.setAudioDevice(mode);
    }

    /** 現在のオーディオデバイスモードを取得する
     *
     * @return
     */
    public AudioDeviceManager.AUDIO_DEVICE_MODE getAudioDeviceMode(){
        return audioDeviceManager.getAudioDeviceMode();
    }
}
