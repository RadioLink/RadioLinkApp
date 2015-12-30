package jp.tf_web.radiolink.net;

import android.util.Log;

import java.net.InetAddress;
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
import jp.tf_web.radiolink.util.ByteArrayUtil;

/** UDPで送信する
 *
 * Created by furukawanobuyuki on 2015/12/10.
 */
public class UDPSender {
    private final String TAG = "UDPSender";

    private Channel channel;

    /** コンストラクタ
     *
     * @param addr 送信元アドレス
     */
    public UDPSender(final InetAddress addr,final int port){
        Log.d(TAG,"UDPSender addr:"+addr.getHostAddress()+" port:"+port);

        final EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap udpbs = new Bootstrap();
        udpbs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new UDPInboundHandlerAdapter())
                .connect(addr,port);
    }

    /** 送信
     *
     * @param data
     */
    public void writeUDP(InetSocketAddress addr,byte[] data) {
        if(channel == null) return;
        Log.d(TAG,"writeUDP data:"+ ByteArrayUtil.toHexString(data));
        Log.d(TAG,"         addr:"+ addr.getAddress().getHostAddress()+":"+addr.getPort());
        Log.d(TAG,"         remoteAddress:"+ channel.remoteAddress());

        //パケット内容
        ByteBuf src = Unpooled.copiedBuffer(data);
        DatagramPacket packet = new DatagramPacket(src,addr);
        channel.writeAndFlush(packet);
    }

    class UDPInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
        UDPInboundHandlerAdapter() {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx){
            Log.d(TAG, "channelActive");
            channel = ctx.channel();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
