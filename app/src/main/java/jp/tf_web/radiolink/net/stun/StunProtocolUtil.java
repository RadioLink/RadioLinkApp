package jp.tf_web.radiolink.net.stun;

import android.util.Log;

import java.net.InetSocketAddress;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ErrorCode;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.util.UtilityException;
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

/** Binding を行う為のユーテリティクラス
 *
 * Created by furukawanobuyuki on 2015/12/31.
 */
public class StunProtocolUtil {
    private static String TAG = "StunProtocolUtil";

    private InetSocketAddress stunServerSocketAddress;
    private InetSocketAddress bindSocketAddress;

    private Channel channel;

    /** コンストラクタ
     *
     * @param stunServerAddr STUNサーバのホスト名,IpAddress
     * @param stunServerPort STUNサーバのポート
     * @param sourceIaddress レスポンスを受信するIPAddress
     * @param sourcePort レスポンスを受信するポート
     */
    public StunProtocolUtil(final String stunServerAddr,final int stunServerPort,final String sourceIaddress,int sourcePort){

        //接続先 STUNサーバ情報
        this.stunServerSocketAddress = new InetSocketAddress(stunServerAddr,stunServerPort);

        //受信用 アドレスとソケット情報
        this.bindSocketAddress = new InetSocketAddress(sourceIaddress,sourcePort);

        //受信用データグラムソケット作成
        final EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap udpbs = new Bootstrap();
        udpbs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new UDPInboundHandlerAdapter())
                .bind(this.bindSocketAddress);

    }

    class UDPInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
        UDPInboundHandlerAdapter() {

        }

        @Override
        public void channelActive(ChannelHandlerContext ctx){
            Log.d(TAG, "channelActive");
            channel = ctx.channel();
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
            //SUTNサーバからレスポンスを受信？
            DatagramPacket receive = (DatagramPacket) msg;
            byte[] data = receive.content().slice().array();
            MessageHeader receiveMH = new MessageHeader();
            receiveMH = MessageHeader.parseHeader(data);
            receiveMH.parseAttributes(data);

            ErrorCode ec = (ErrorCode) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ErrorCode);
            if(ec != null){
                Log.e(TAG,"error code:"+ec.getResponseCode()+" reason:"+ec.getReason());
                Log.d(TAG, "Message header contains an Errorcode message attribute.");
                return;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }

    /** Binding 処理
     *
     */
    public void binding(){
        try {
            //BindingRequestを生成
            MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
            sendMH.generateTransactionID();
            ChangeRequest changeRequest = new ChangeRequest();
            sendMH.addMessageAttribute(changeRequest);
            byte[] data = sendMH.getBytes();
            Log.d(TAG, "Binding Request sent.");

            ByteBuf src = Unpooled.copiedBuffer(data);
            DatagramPacket packet = new DatagramPacket(src,this.bindSocketAddress);

            channel.connect(this.stunServerSocketAddress);
            channel.writeAndFlush(packet);

        } catch (UtilityException e) {
            e.printStackTrace();
        }
    }
}
