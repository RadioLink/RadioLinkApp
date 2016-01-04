package jp.tf_web.radiolink.net.udp.service;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/** UDPからの受信等を取得するリスナー
 *
 * Created by furukawanobuyuki on 2015/12/09.
 */
public interface UDPReceiverListener {

    /** STUN Binding 結果を通知
     *
     * @param publicSocketAddr パブリックIP,ポート
     */
    void onStunBinding(final InetSocketAddress publicSocketAddr);

    /** 受信したデータを通知
     *
     * @param ctx
     * @param packet
     * @param data
     */
    void onReceive(final ChannelHandlerContext ctx,final DatagramPacket packet,final byte[] data);
}
