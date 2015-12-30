package jp.tf_web.radiolink.net;

import android.util.Log;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/** UDPの受信処理
 *
 * Created by furukawanobuyuki on 2015/12/09.
 */
public class UDPReceiver {
    private final String TAG = "UDPReceiver";

    private Bootstrap udpbs;

    /** コンストラクタ
     *
     * @param port 受信ポート
     * @param listener
     */
    public UDPReceiver(final int port,final UDPReceiverListener listener){
        Log.d(TAG,"UDPReceiver port:"+port);

        final EventLoopGroup group = new NioEventLoopGroup();
        udpbs = new Bootstrap();
        udpbs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new UDPInboundHandlerAdapter(listener))
                .option(ChannelOption.SO_BROADCAST, true)
                .bind(port);
    }

    /** 閉じる
     *
     */
    public void close(){
        if(udpbs == null) return;
        udpbs.clone();
        udpbs = null;
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
            if(this.listener != null){
                this.listener.onReceive(ctx, (DatagramPacket) msg);
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
