package jp.tf_web.radiolink.controller;

import jp.tf_web.radiolink.net.protocol.packet.Packet;

/** オーディオ,通信処理をまとめたクラスのイベントリスナー
 *
 * Created by furukawanobuyuki on 2016/01/09.
 */
public interface AudioControllerListener {

    /** 録音結果の通知
     *
     * @param packet 録音データをパケットに変換したデータ
     * @param volume 録音ボリュームの最大値
     */
    void onAudioRecord(final Packet packet,final short volume);


    /** 受信データの通知
     *
     * @param packet 受信したパケット
     */
    void onReceive(final Packet packet);
}
