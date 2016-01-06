package jp.tf_web.radiolink.net.protocol;

import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jp.tf_web.radiolink.net.protocol.packet.Header;
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.protocol.packet.Payload;
import jp.tf_web.radiolink.util.ByteArrayUtil;

/** Packet 関係の処理を行うユーテリティ
 * Created by furukawanobuyuki on 2016/01/04.
 */
public class PacketUtil {
    private static String TAG = "PacketUtil";

    private static PacketUtil ourInstance = new PacketUtil();

    public static PacketUtil getInstance() {
        return ourInstance;
    }

    //シーケンス番号
    private long seq = 0;

    //送信元識別子
    private String ssrc;

    /** コンストラクタ
     *
     */
    private PacketUtil() {

    }

    /** テスト
     *
     */
    public void test(){

        //新規作成
        Packet packet = createPacket();
        Log.d(TAG, "ssrc:" + this.ssrc);
        {
            final byte[] data = {
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
            packet.addPayload(data);
        }
        {
            final byte[] data = {0x09,0x08,0x07,0x06,0x05, 0x04,0x03,0x02,0x01,0x00};
            packet.addPayload(data);
        }

        //新規作成したパケットをバイト配列にして比較
        byte[] dst = packet.toByteArray();
        Log.d(TAG, "packet size:"+dst.length+" "+ ByteArrayUtil.toHexString(dst) );

        Packet packet2 = createPacket(dst);
        Header header2 = packet2.getHeader();
        Log.d(TAG, "ssrc:" + header2.getSsrc());

        List<Payload> payload2 = packet2.getPayload();
        Log.d(TAG, "payload2 size:" + payload2.size());
        for(Payload p:payload2) {
            Log.d(TAG, "p size:" + p.getSize() + " " + ByteArrayUtil.toHexString(p.getData()));
        }
    }


    /** 送信元識別子(Synchronization source) 作成
     *
     * @return
     */
    public String createSsrc(){
        return UUID.randomUUID().toString().replaceAll("-","").trim();
    }

    /** 送信元識別子(Synchronization source) 取得
     *
     * @return
     */
    public String getSsrc(){
        return this.ssrc;
    }

    /** シーケンス番号を取得
     *
     * @return
     */
    public long getSeq(){
        return this.seq;
    }

    /** Packet 作成
     *
     * @return 生成したPacket
     */
    public Packet createPacket(){
        //シーケンス番号を進める
        seq++;

        //送信元識別子を初回だけ設定する
        if(ssrc == null){
           ssrc = PacketUtil.getInstance().createSsrc();
        }

        //タイムスタンプ
        long timeStamp = System.currentTimeMillis();

        Header header = new Header(ssrc,seq,timeStamp);
        List<Payload > payload = new ArrayList<>();

        return new Packet(header,payload);
    }


    /** バイト配列からパケットを生成
     *
     * @param src 変換もとバイト配列
     * @return
     */
    public Packet createPacket(byte[] src){
        Packet packet = null;
        try {
            packet = new Packet(src);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return packet;
    }
}
