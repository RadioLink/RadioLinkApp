package jp.tf_web.radiolink.net.udp.service;

import java.net.InetSocketAddress;

/** UDPサービスのリスナー
 *
 * Created by furukawanobuyuki on 2015/12/08.
 */
public interface UDPServiceListener {

    /** STUN Binding 結果を通知
     *
     * @param publicSocketAddr パブリックIP,ポート
     */
    void onStunBinding(final InetSocketAddress publicSocketAddr);

    /** UDPServiceから届いいた メッセージを受信した場合
     *
     * @param cmd コマンド文字列
     * @param data 受信した byte[]
     */
    void onReceive(final String cmd, final byte[] data);
}
