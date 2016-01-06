package jp.tf_web.radiolink.net.protocol.packet;

import android.util.Log;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

/** RadioLink パケットヘッダー
 *
 *  RTPパケット構造を参考にシンプルに実装
 *  参考)
 *  https://tools.ietf.org/html/rfc3550
 *  http://www.infraexpert.com/study/telephony3.html
 *  https://ja.wikipedia.org/wiki/Real-time_Transport_Protocol
 *  http://www.kyastem.co.jp/technical/ExplanationCodec05.html
 *
 * Created by furukawanobuyuki on 2016/01/04.
 */
public class Header implements PacketInterface{
    private static String TAG = "Header";

    //バイト配列変換用のバイトバッファー
    private ByteBuffer byteBuf = ByteBuffer.allocate(1024);

    /** 送信元識別子(Synchronization source)
     *
     *
     */
    private String ssrc;

    /** シーケンス番号
     *
     */
    private long seq;

    /** タイムスタンプ
     *
     */
    private long timeStamp;

    //データをバイト配列にした物
    private byte[] src;

    /** コンストラクタ
     *
     * @param ssrc 送信元識別子
     * @param seq シーケンス番号
     * @param timeStamp タイムスタンプ
     */
    public Header(String ssrc,long seq,long timeStamp){
        this.ssrc = ssrc;
        this.seq = seq;
        this.timeStamp = timeStamp;
    }

    /** コンストラクタ
     *
     * @param src
     * @throws ParseException
     */
    public Header(byte[] src) throws ParseException {
        this.src = src.clone();
    }

    /** 構文解析する
     *
     * @return
     * @throws ParseException
     */
    @Override
    public byte[] parse() throws ParseException {
        if(this.src == null){
            //エラー
            Throwable t = new Throwable();
            StackTraceElement[] ste = t.getStackTrace();
            throw new ParseException("src is null",ste[0].getLineNumber());
        }

        //各ヘッダーを取得する
        byteBuf.clear();
        byteBuf.put(this.src);
        byteBuf.flip();

        //ssrc
        byte[] dst = new byte[32];
        byteBuf.get(dst, 0, dst.length);
        ssrc = new String(dst);
        //seq
        seq = byteBuf.getLong();
        //timestamp
        timeStamp = byteBuf.getLong();

        //処理対象じゃなかった残りのデータを返す
        if(byteBuf.position() == 0){
            //残りデータが無かった
            return null;
        }

        byte[] result = new byte[byteBuf.limit() - byteBuf.position()];
        byteBuf.get(result,0,result.length);

        return result;
    }

    /** バイト配列に変換する
     *
     * @return
     */
    @Override
    public byte[] toByteArray() {
        if(this.src == null){
            byteBuf.clear();
            byteBuf.put(ssrc.getBytes());
            byteBuf.putLong(seq);
            byteBuf.putLong(timeStamp);
            byteBuf.flip();
            this.src = new byte[byteBuf.limit()];
            byteBuf.get(this.src);
        }
        return this.src;
    }

    /** 送信元識別子(Synchronization source)を取得
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

    /** タイムスタンプを取得
     *
     * @return
     */
    public long getTimeStamp(){
        return this.timeStamp;
    }
}
