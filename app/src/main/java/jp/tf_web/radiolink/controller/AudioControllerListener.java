package jp.tf_web.radiolink.controller;

import java.net.InetSocketAddress;

import jp.tf_web.radiolink.net.protocol.packet.Packet;

/** オーディオ,通信処理をまとめたクラスのイベントリスナー
 *
 * Created by furukawanobuyuki on 2016/01/09.
 */
public interface AudioControllerListener {

    /** 初期化処理が完了して処理可能な状態になった事の通知
     *
     */
    void onActive();

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

    /** ミュート状態に変更があった事の通知
     *
     * @param isMute
     */
    void onMicrophoneMute(boolean isMute);
}
