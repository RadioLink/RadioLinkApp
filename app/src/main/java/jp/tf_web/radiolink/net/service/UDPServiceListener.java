package jp.tf_web.radiolink.net.service;

/** UDPサービスのリスナー
 *
 * Created by furukawanobuyuki on 2015/12/08.
 */
public interface UDPServiceListener {

    /** UDPServiceから届いいた メッセージを受信した場合
     *
     * @param cmd コマンド文字列
     * @param data 受信した byte[]
     */
    public void onReceive(final String cmd, final byte[] data);
}
