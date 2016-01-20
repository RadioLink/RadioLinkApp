package jp.tf_web.radiolink.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import jp.tf_web.radiolink.util.ByteArrayUtil;

/** 録音 処理を実装するクラス
 *
 * Created by furukawanobuyuki on 2015/12/27.
 */
public class RecordManager {
    private static String TAG = "RecordManager";

    //コンテキスト
    private Context context;

    //録音 サンプリングレート
    private int sampleRateInHz;

    //録音 バッファサイズ
    private int bufSize;

    //通知さきリスナー
    private RecordManagerListener listener;

    //PCM録音する為のクラス
    private AudioRecord audioRecord = null;

    //録音中かのステータス
    private boolean isRecording = false;

    //オーディオ録音データ取得用のスレッド
    private Thread audioRecordThread;

    //シングルトン
    private static RecordManager ourInstance;

    /** インスタンス取得
     *
     * @param context
     * @param sampleRateInHz
     * @param bufSize
     * @param listener
     * @return
     */
    public static RecordManager getInstance(final Context context,final int sampleRateInHz,final int bufSize,final RecordManagerListener listener) {
        ourInstance = new RecordManager(context, sampleRateInHz, bufSize, listener);
        return ourInstance;
    }

    public static RecordManager getInstance() throws Exception {
        if(ourInstance == null){
            throw new Exception("ourInstance is null");
        }
        return ourInstance;
    }

    /** コンストラクタ
     *
     * @param context  コンテキスト
     * @param sampleRateInHz  録音サンプリングレート
     * @param bufSize  録音バッファーサイズ
     * @param listener 録音結果受け取りリスナー
     */
    private RecordManager(final Context context,final int sampleRateInHz,final int bufSize,final RecordManagerListener listener){
        this.context = context;
        this.sampleRateInHz = sampleRateInHz;
        this.bufSize = bufSize;
        this.listener = listener;
    }

    /** 各種初期化
     *
     */
    private void initialize(){
        /* 機種依存なので 固定値にする
        final int bufSize = AudioRecord.getMinBufferSize(
                Config.SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        */

        Log.d(TAG, "REC bufSize:" + this.bufSize);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                this.sampleRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                this.bufSize);

        audioRecord.startRecording();

        audioRecordThread = new Thread(new Runnable(){
            @Override
            public void run() {

                isRecording = true;
                while (isRecording) {

                    //マイクからのデータを取得する
                    byte[] src = new byte[bufSize];
                    int rec_size = audioRecord.read(src, 0, src.length);

                    final byte[] dst = new byte[rec_size];
                    System.arraycopy(src, 0, dst, 0, rec_size);

                    //最大ボリュームを取得
                    final short volume = getMaxVolume(dst,rec_size);

                    //ここで読み込んだ分だけコールバックする
                    listener.onAudioRecord(dst, rec_size, volume);
                }
                audioRecord.stop();
                //audioRecord.release();
                audioRecord = null;
            }
        });
    }

    /** 録音 開始
     *
     */
    public void start(){
        Log.d(TAG, "start()");
        if(audioRecord != null){
            //開始済みなので無視
            return;
        }

        //停止済みなので初期化する
        initialize();
        if(audioRecordThread != null) {
            audioRecordThread.start();
        }
    }

    /** 録音 停止
     *
     */
    public void stop(){
        Log.d(TAG, "stop()");
        if(audioRecordThread != null) {
            try {
                isRecording = false;
                //スレッドの終了 待ち
                audioRecordThread.join();
                audioRecordThread = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** 最大ボリュームを取得してコールバックする
     *
     * @param src PCM録音したデータ
     * @param size 録音データのサイズ
     */
    private short getMaxVolume(byte[] src,final int size){
        short[] data = ByteArrayUtil.byteArr2shortArr(src,size);
        short max = 0;
        for (int i=0; i<data.length; i++) {
            // 最大音量を計算
            max = (short)Math.max(max, data[i]);
        }
        return max;
    }
}
