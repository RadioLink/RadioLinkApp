package jp.tf_web.radiolink.net.stun;

import android.os.AsyncTask;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.ErrorCode;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderParsingException;
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
import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.util.ByteArrayUtil;

/** STUN Binding を行う為のユーテリティクラス
 *
 * Created by furukawanobuyuki on 2015/12/31.
 */
public class StunProtocolUtil {
    private static String TAG = "StunProtocolUtil";

//    private InetSocketAddress bindSocketAddress;
//
//    private Channel channel;
//
//    /** コンストラクタ
//     *
//     * @param sourceIaddress レスポンスを受信するIPAddress
//     * @param sourcePort レスポンスを受信するポート
//     */
//    public StunProtocolUtil(final String sourceIaddress,final int sourcePort) {
//        AsyncTask task = new AsyncTask<Object, Void, Void>(){
//            @Override
//            protected Void doInBackground(Object... params) {
//                //各種初期化処理
//                initialize(sourceIaddress,sourcePort);
//                return null;
//            }
//        };
//        task.execute();
//    }
//
//    /** 各種初期化処理
//     *
//     * @param sourceIaddress レスポンスを受信するIPAddress
//     * @param sourcePort レスポンスを受信するポート
//     */
//    private void initialize(final String sourceIaddress,final int sourcePort){
//
//        //受信用 アドレスとソケット情報
//        this.bindSocketAddress = new InetSocketAddress(sourceIaddress,sourcePort);
//
//        //受信用データグラムソケット作成
//        final EventLoopGroup group = new NioEventLoopGroup();
//        Bootstrap udpbs = new Bootstrap();
//        udpbs.group(group)
//                .channel(NioDatagramChannel.class)
//                .handler(new UDPInboundHandlerAdapter())
//                .bind(this.bindSocketAddress);
//
//    }
//
//    class UDPInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
//        UDPInboundHandlerAdapter() {
//
//        }
//
//        @Override
//        public void channelActive(ChannelHandlerContext ctx){
//            Log.d(TAG, "channelActive");
//            channel = ctx.channel();
//
//            //STUN Bindingを実行
//            binding(channel,Config.STUN_SERVER_NAME, Config.STUN_SERVER_PORT, bindSocketAddress.getAddress().getHostAddress(), Config.STUN_BIND_PORT);
//        }
//
//        /** UDPでデータが届いた時
//         *
//         * @param ctx
//         * @param msg
//         * @throws Exception
//         */
//        @Override
//        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//            Log.d(TAG, "channelRead");
//            //SUTNサーバからレスポンスを受信？
//            DatagramPacket receive = (DatagramPacket) msg;
//
//            int capacity = receive.content().slice().capacity();
//            byte[] data = new byte[capacity];
//            receive.content().getBytes(0,data);
//            Log.d(TAG, "data length:" + data.length + " " + ByteArrayUtil.toHexString(data));
//
//            InetSocketAddress publicSocketAddr = parsePublicInetSocketAddress(data);
//            Log.d(TAG,"public ip:"+publicSocketAddr.getAddress().getHostAddress()+" port:"+publicSocketAddr.getPort());
//
//            //TODO: STUN処理に成功したのでリスナーに通知等をする
//        }
//
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//            cause.printStackTrace();
//            ctx.close();
//        }
//    }

    /** STUNのレスポンスで取得したIPアドレス,ポートを取得する
     *
     * @param data UDPで取得したデータ
     * @return IPアドレス,ポート
     *
     * @throws MessageHeaderParsingException
     * @throws UtilityException
     * @throws UnknownHostException
     * @throws MessageAttributeParsingException
     */
    public static InetSocketAddress parsePublicInetSocketAddress(byte[] data){
        InetSocketAddress result = null;
        try {
            MessageHeader receiveMH = MessageHeader.parseHeader(data);
            receiveMH.parseAttributes(data);

            ErrorCode ec = (ErrorCode) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ErrorCode);
            if (ec != null) {
                Log.e(TAG, "error code:" + ec.getResponseCode() + " reason:" + ec.getReason());
                Log.d(TAG, "Message header contains an Errorcode message attribute.");
                return null;
            }
            MappedAddress ma = (MappedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
            if (ma == null) {
                //STUN処理出来なかった場合？
                Log.d(TAG, "Response does not contain a Mapped Address");
                return null;
            }
            /*
            ChangedAddress ca = (ChangedAddress) receiveMH.getMessageAttribute(MessageAttribute.MessageAttributeType.ChangedAddress);
            if(ca == null) {
                //STUN処理出来なかった場合？
                Log.d(TAG,"Response does not contain a Changed Address message attribute.");
                return;
            }
            */
            //STUN処理に成功
            InetAddress publicIp = ma.getAddress().getInetAddress();
            int publicPort = ma.getPort();

            result = new InetSocketAddress(publicIp, publicPort);
        }
        catch (MessageHeaderParsingException e){
            //e.printStackTrace();
        }
        catch (UtilityException e){
            e.printStackTrace();
        }
        catch (UnknownHostException e){
            e.printStackTrace();
        }
        catch (MessageAttributeParsingException e){
            e.printStackTrace();
        }

        return result;
    }

    /** BindingRequest を生成
     *
     * @return BindingRequestのbyte配列
     */
    private static byte[] createBindingRequest(){
        byte[] data = null;
        try{
            //BindingRequestを生成
            MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
            sendMH.generateTransactionID();
            ChangeRequest changeRequest = new ChangeRequest();
            sendMH.addMessageAttribute(changeRequest);
            data = sendMH.getBytes();
        } catch (UtilityException e) {
            e.printStackTrace();
        }
        return data;
    }

    /** Binding 処理
     *
     * @param channel
     * @param stunServerAddr STUNサーバのホスト名,IpAddress
     * @param stunServerPort STUNサーバのポート
     * @param bindIaddress BIND アドレス
     * @param bindPort バインドポート
     */
    public static void binding(final Channel channel,final String stunServerAddr,final int stunServerPort,final String bindIaddress,final int bindPort){
        Log.d(TAG, "binding()");

        AsyncTask task = new AsyncTask<Object, Void, Void>(){
            @Override
            protected Void doInBackground(Object... params) {

                //接続先 STUNサーバ情報
                InetSocketAddress stunServerSocketAddress = new InetSocketAddress(stunServerAddr,stunServerPort);

                InetSocketAddress bindSocketAddress = new InetSocketAddress(bindIaddress,bindPort);
                //BindingRequestを生成
                byte[] data = createBindingRequest();

                ByteBuf src = Unpooled.copiedBuffer(data);
                DatagramPacket packet = new DatagramPacket(src,stunServerSocketAddress,bindSocketAddress);
                channel.writeAndFlush(packet);

                Log.d(TAG, "Binding Request sent.");

                return null;
            }
        };
        task.execute();
    }
}
