package jp.tf_web.radiolink.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

/** 再生 処理をする実装するクラス
 *
 * Created by furukawanobuyuki on 2015/12/28.
 */
public class TrackManager {
    private static String TAG = "TrackManager";

    //コンテキスト
    private Context context;

    //録音
    private AudioTrack audioTrack;

    //再生 サンプリングレート
    private int sampleRateInHz;

    /** コンストラクタ
     *
     * @param context コンテキスト
     * @param sampleRateInHz 再生 サンプリングレート
     */
    public TrackManager(final Context context,final int sampleRateInHz){
        this.context = context;
        this.sampleRateInHz = sampleRateInHz;
    }

    /** 初期化処理
     *
     * @param audioStream オーディオストリーム種類
     */
    void initialize(final int audioStream) {
        if(audioTrack != null){
            audioTrack.stop();
            audioTrack = null;
        }

        // 必要となるバッファサイズを計算
        int bufSize = android.media.AudioTrack.getMinBufferSize(
                sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TAG, "PLAY bufSize:" + bufSize);

        // AudioTrackインスタンス作成
        audioTrack = new AudioTrack(
                audioStream,
                sampleRateInHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
                AudioTrack.MODE_STREAM);

        audioTrack.pause();
    }

    /** 再生 開始
     *
     * @param audioStream
     */
    public void start(final int audioStream){
        //初期化処理
        initialize(audioStream);

        //再生開始
        audioTrack.play();
    }

    /** 再生 停止
     *
     */
    public void stop(){
        if(audioTrack == null) return;
        audioTrack.stop();
    }

    /** オーディオバッファに書き込む
     *
     * @param pcm PCMデータ
     * @param offset バッファオフセット
     * @param size バッファサイズ
     */
    public void write(final byte[] pcm,final int offset,final int size){
        //再生バッファに送る
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            //18〜22 オーディオ書き込みでブロッキング
            audioTrack.write(pcm, offset, size);
        }
        else{
            //23 以上 非同期で書き込みできるのでこちらを使う
            audioTrack.write(pcm, offset, size, AudioTrack.WRITE_NON_BLOCKING);
        }
    }

}
