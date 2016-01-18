package jp.tf_web.radiolink.net.udp.service;

import android.os.AsyncTask;
import android.util.Log;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.net.stun.StunProtocolUtil;
import jp.tf_web.radiolink.util.ByteArrayUtil;

/** UDPの受信処理
 *
 * Created by furukawanobuyuki on 2015/12/09.
 */
public class UDPReceiver {
    private final String TAG = "UDPReceiver";

    //送受信に使う ローカルアドレス,ローカルポート
    private InetSocketAddress bindSocketAddress;

    private Bootstrap udpbs;
    private Channel channel;

    //ステータス型
    private enum Status{
        //初期値
        INITIAL,
        //STUNリクエスト送信中
        STUN_SENDING,
        //実行中
        ACTIVE
    };

    //ステータス
    private Status status = Status.INITIAL;

    private EventLoopGroup group;

    /** コンストラクタ
     *
     * @param bindIaddress ローカルアドレス
     * @param bindPort ローカルポート
     * @param listener イベントリスナー
     */
    public UDPReceiver(final String bindIaddress, final int bindPort,final UDPReceiverListener listener){
        Log.d(TAG,"UDPReceiver bind "+bindIaddress+":"+bindPort);
        setStatus(Status.INITIAL);

        AsyncTask task = new AsyncTask<Object, Void, Void>(){
            @Override
            protected Void doInBackground(Object... params) {
                //各種初期化処理
                initialize(bindIaddress,bindPort,listener);
                return null;
            }
        };
        task.execute();
    }

    /** 各種初期化処理
     *
     * @param bindIaddress レスポンスを受信するIPAddress
     * @param bindPort レスポンスを受信するポート
     * @param listener イベントリスナー
     */
    private void initialize(final String bindIaddress,final int bindPort,final UDPReceiverListener listener){
        Log.d(TAG,"initialize");

        //受信用 アドレスとソケット情報
        this.bindSocketAddress = new InetSocketAddress(bindIaddress,bindPort);

        group = new NioEventLoopGroup();
        udpbs = new Bootstrap();
        udpbs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new UDPInboundHandlerAdapter(listener))
                .bind(this.bindSocketAddress);
    }

    /** 閉じる
     *
     */
    public void close(){
        Log.d(TAG,"close()");
        if(udpbs != null) {
            udpbs.clone(group);
            udpbs = null;
        }
        if(channel != null) {
            channel.close();
            channel = null;
        }
    }

    /** ステータスを設定する
     *
     * @param status
     */
    private void setStatus(Status status){
        switch (status){
            case STUN_SENDING:
                //STUN binding リクエストを送る
                sendStunBinding();
                break;
            case ACTIVE:
                //メイン処理をするステータス
                break;
        }
        this.status = status;
    }

    //STUN binding リクエストを送る
    private void sendStunBinding(){
        StunProtocolUtil.binding(channel,
                Config.STUN_SERVER_NAME,
                Config.STUN_SERVER_PORT,
                bindSocketAddress.getAddress().getHostAddress(),
                bindSocketAddress.getPort());
    }

    /** 送信
     *
     * @param connectAddr 接続先アドレス,ポート
     * @param addr 送信元アドレス,ポート
     * @param data 送信データ
     */
    public void writeUDP(final InetSocketAddress connectAddr,InetSocketAddress addr,byte[] data) {
        if(channel == null) return;
        Log.d(TAG, "writeUDP data:" + ByteArrayUtil.toHexString(data));
        Log.d(TAG,"  connectAddr:"+ connectAddr.getAddress().getHostAddress()+":"+connectAddr.getPort());
        Log.d(TAG,"         addr:"+ addr.getAddress().getHostAddress()+":"+addr.getPort());

        //パケット内容
        ByteBuf src = Unpooled.copiedBuffer(data);
        DatagramPacket packet = new DatagramPacket(src,connectAddr,addr);
        channel.writeAndFlush(packet);
    }

    /** UDPチャンネルハンドラーアダプター
     *
     */
    class UDPInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
        //イベントのリスナー
        private UDPReceiverListener listener;

        /** コンストラクタ
         *
         * @param listener イベントのリスナー
         */
        UDPInboundHandlerAdapter(final UDPReceiverListener listener) {
            //リスナーを設定
            this.listener = listener;
            setStatus(Status.INITIAL);
        }

        /** UDPチャンネルが有効になった
         *
         * @param ctx
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx){
            Log.d(TAG, "channelActive");
            channel = ctx.channel();

            //STUN 処理を実行して public IP,PORTを取得する
            setStatus(Status.STUN_SENDING);
        }

        /** UDPでデータが届いた時
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Log.d(TAG, "channelRead");

            DatagramPacket packet = (DatagramPacket) msg;
            int capacity = packet.content().slice().capacity();
            byte[] data = new byte[capacity];
            packet.content().getBytes(0,data);
            Log.d(TAG, "data length:" + data.length + " " + ByteArrayUtil.toHexString(data));

            if(status == Status.STUN_SENDING) {
                //STUN 成功 パブリック IP,ポートを通知
                //TODO: STUN 失敗時の処理を書く必要がある タイムアウト等
                InetSocketAddress publicSocketAddr = StunProtocolUtil.parsePublicInetSocketAddress(data);
                if(publicSocketAddr != null) {
                    listener.onStunBinding(publicSocketAddr);
                    //通常ステータスに移行
                    setStatus(Status.ACTIVE);
                }
            }
            else if(status == Status.ACTIVE) {
                //リスナーに受信データを通知する
                if (this.listener != null) {
                    this.listener.onReceive(ctx, packet, data);
                }
            }
        }

        /** エラー
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
