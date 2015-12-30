package jp.tf_web.radiolink.audio;

/** 録音結果を取得するリスナー
 *
 * Created by furukawanobuyuki on 2015/12/27.
 */
public interface RecordManagerListener {

    /** 録音結果の通知
     *
     * @param data 録音したPCMデータ
     * @param size 録音データのサイズ
     * @param volume 録音ボリュームの最大値
     */
    void onAudioRecord(final byte[] data,final int size,final short volume);
}
