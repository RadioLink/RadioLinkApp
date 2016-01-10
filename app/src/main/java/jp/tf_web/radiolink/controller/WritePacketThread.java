package jp.tf_web.radiolink.controller;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.audio.OpusManager;
import jp.tf_web.radiolink.audio.TrackManager;
import jp.tf_web.radiolink.net.protocol.PacketComparator;
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.protocol.packet.Payload;

/**
 * Created by furukawanobuyuki on 2016/01/09.
 */
public class WritePacketThread extends Thread {
    private static String TAG = "WritePacketThread";

    //コンテキスト
    private Context context;

    //Opusデコード,エンコード
    private OpusManager opusManager;

    //再生処理をするクラス
    private Map<String,TrackManager> trackManagerMap = Collections.synchronizedMap(new HashMap<String, TrackManager>());

    //受信パケットのリスト 同期化オブジェクトでラップ
    private List<Packet> receivePacketList = Collections.synchronizedList(new ArrayList<Packet>());

    //パケット一覧ソート用のコンパレータ
    private PacketComparator comparator = new PacketComparator();

    //再生スレッドの実行状態
    private boolean isRunning;

    //オーディオストリームのタイプ
    private int audioStream;

    //パケットを一覧に追加するスレッドプール
    private ExecutorService addPacketExecutor = Executors.newCachedThreadPool();

    /** コンストラクタ
     *
     * @param context
     * @param opusManager
     */
    public WritePacketThread(final Context context,final OpusManager opusManager){
        this.context = context;
        this.opusManager = opusManager;
    }

    /** 再生状態
     *
     * @return
     */
    public boolean isRunning(){
        return this.isRunning;
    }

    /** パケットを一覧に追加
     *
     * @param packet
     */
    public void addPacket(final Packet packet){
        //送信元識別子別にオーディオトラックを生成
        String ssrc = packet.getHeader().getSsrc();
        if(trackManagerMap.containsKey(ssrc) == false){
            //未登録の場合 trackManager を生成する
            TrackManager trackManager = new TrackManager(context, opusManager.getSamplingRate());
            trackManager.start(audioStream);
            trackManagerMap.put(ssrc, trackManager);
        }

        //リストに追加する
        addPacketExecutor.execute(new Runnable() {
            @Override
            public void run() {
                receivePacketList.add(packet);
            }
        });
    }

    /** パケットをデコードして再生処理をする
     *
     * @param packet
     */
    private void writePacket(final Packet packet){
        //送信元に対応したオーディオトラックを取得
        String ssrc = packet.getHeader().getSsrc();
        if(trackManagerMap.get(ssrc) == null){
            Log.d(TAG,"trackManager is null");
            return;
        }

        for(Payload p:packet.getPayload()){
            //ペイロードから音データを取り出しOPUSデコードする
            byte[] pcm = opusManager.decode(p.getData(), Config.OPUS_FRAME_SIZE);

            //再生
            trackManagerMap.get(ssrc).write(pcm, 0, pcm.length);
        }
    }

    /** スレッド実行を開始
     *
     */
    public void startRunning(final int audioStream){
        if(isRunning == true) return;
        isRunning = true;
        this.audioStream = audioStream;

        if(this.getState() == State.NEW) {
            this.start();
        }
    }

    /** スレッド実行を停止
     *
     */
    public void stopRunning(){
        isRunning = false;
    }

    @Override
    public void run(){
        Log.i(TAG, "WritePacketThread start");
        //スレッド優先度を変更
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        isRunning = true;
        while (isRunning){
            if(trackManagerMap.size() == 0) continue;
            if (receivePacketList.size() == 0) continue;
            try {
                // タイムスタンプ,シーケンス番号順にソートする
                Collections.sort(receivePacketList, comparator);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try{
                //リストの最初のパケットを取り出して再生
                Packet packet = receivePacketList.remove(0);
                writePacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //全てのオーディオトラックを停止
        for(Map.Entry<String,TrackManager> t:trackManagerMap.entrySet()) {
            t.getValue().stop();
        }
        trackManagerMap.clear();
        receivePacketList.clear();

        Log.i(TAG, "WritePacketThread stop");
    }
}
