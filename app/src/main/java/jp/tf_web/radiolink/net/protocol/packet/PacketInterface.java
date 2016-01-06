package jp.tf_web.radiolink.net.protocol.packet;

import java.text.ParseException;

/** パケット共通のインターフェース定義
 *
 * Created by furukawanobuyuki on 2016/01/06.
 */
public interface PacketInterface {

    /** 構文解析する
     *
     * @return
     * @throws ParseException
     */
    byte[] parse() throws ParseException;

    /** バイト配列に変換する
     *
     * @return
     */
    byte[] toByteArray();
}
