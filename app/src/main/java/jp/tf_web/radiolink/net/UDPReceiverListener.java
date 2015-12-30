package jp.tf_web.radiolink.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/** UDPからの受信等を取得するリスナー
 *
 * Created by furukawanobuyuki on 2015/12/09.
 */
public interface UDPReceiverListener {

    /** 受信したデータを通知
     *
     * @param ctx
     * @param packet
     */
    public void onReceive(ChannelHandlerContext ctx, DatagramPacket packet);
}
