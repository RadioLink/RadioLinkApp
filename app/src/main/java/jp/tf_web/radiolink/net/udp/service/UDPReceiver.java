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

    /** コンストラクタ
     *
     * @param bindIaddress ローカルアドレス
     * @param bindPort ローカルポート
     * @param listener イベントリスナー
     */
    public UDPReceiver(final String bindIaddress, final int bindPort,final UDPReceiverListener listener){
        Log.d(TAG,"UDPReceiver bind "+bindIaddress+":"+bindPort);

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
        //受信用 アドレスとソケット情報
        this.bindSocketAddress = new InetSocketAddress(bindIaddress,bindPort);

        final EventLoopGroup group = new NioEventLoopGroup();
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
        if(udpbs == null) return;
        udpbs.clone();
        udpbs = null;
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
        Log.d(TAG,"         addr:"+ addr.getAddress().getHostAddress()+":"+addr.getPort());
        Log.d(TAG,"         remoteAddress:"+ channel.remoteAddress());

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
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx){
            Log.d(TAG, "channelActive");
            channel = ctx.channel();

            //STUN 処理を実行して public IP,PORTを取得する
            StunProtocolUtil.binding(channel,
                    Config.STUN_SERVER_NAME,
                    Config.STUN_SERVER_PORT,
                    bindSocketAddress.getAddress().getHostAddress(),
                    bindSocketAddress.getPort());
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

            //TODO:毎回
            InetSocketAddress publicSocketAddr = StunProtocolUtil.parsePublicInetSocketAddress(data);
            if(publicSocketAddr != null) {
                //STUN 処理に成功 パブリック IP,ポートの取得に成功
                Log.d(TAG, "public ip:" + publicSocketAddr.getAddress().getHostAddress() + " port:" + publicSocketAddr.getPort());
            }
            else {
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
