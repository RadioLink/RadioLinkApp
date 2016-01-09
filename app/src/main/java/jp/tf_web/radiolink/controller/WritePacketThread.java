package jp.tf_web.radiolink.controller;

import android.os.Process;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    //Opusデコード,エンコード
    private OpusManager opusManager;

    //再生処理をするクラス
    private TrackManager trackManager;

    //受信パケットのリスト 同期化オブジェクトでラップ
    private List<Packet> receivePacketList = Collections.synchronizedList(new ArrayList<Packet>());

    //パケット一覧ソート用のコンパレータ
    private PacketComparator comparator = new PacketComparator();

    //再生スレッドの実行状態
    private boolean isRunning;

    public WritePacketThread(final OpusManager opusManager,final TrackManager trackManager){
        this.opusManager = opusManager;
        this.trackManager = trackManager;
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
        receivePacketList.add(packet);
    }

    /** パケットをデコードして再生処理をする
     *
     * @param packet
     */
    private void writePacket(final Packet packet){
        //OPUSデコードする
        List<Payload> payload = packet.getPayload();
        for(Payload p:payload){
            //ペイロードから音を取り出す
            byte[] pcm = opusManager.decode(p.getData(), Config.OPUS_FRAME_SIZE);

            //再生
            trackManager.write(pcm, 0, pcm.length);
        }
    }

    /** スレッド実行を開始
     *
     */
    public void startRunning(){
        if(isRunning == true) return;
        isRunning = true;
        this.start();
    }

    /** スレッド実行を停止
     *
     */
    public void stopRunning(){
        isRunning = false;
    }

    @Override
    public void run(){
        //スレッド優先度を変更
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        isRunning = true;
        while (isRunning){
            //パケット一覧から取得して
            if(receivePacketList.size() == 0) continue;

            // タイムスタンプ,シーケンス番号順にソートする
            Collections.sort(receivePacketList, comparator);

            //リストの最初のパケットを取り出して再生
            Packet packet = receivePacketList.remove( 0 );
            writePacket(packet);
        }
    }
}
