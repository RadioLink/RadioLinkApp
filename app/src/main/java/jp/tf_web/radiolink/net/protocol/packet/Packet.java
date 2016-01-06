package jp.tf_web.radiolink.net.protocol.packet;

import android.util.Log;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import jp.tf_web.radiolink.util.ByteArrayUtil;

/** RadioLink パケット
 *
 *  RTPパケット構造を参考にシンプルに実装
 *  参考)
 *  https://tools.ietf.org/html/rfc3550
 *  http://www.infraexpert.com/study/telephony3.html
 *  https://ja.wikipedia.org/wiki/Real-time_Transport_Protocol
 *
 * Created by furukawanobuyuki on 2016/01/04.
 */
public class Packet implements PacketInterface{
    private static String TAG = "Packet";

    //データをバイト配列にした物
    private byte[] src;

    //バイト配列変換用のバイトバッファー
    private ByteBuffer byteBuf = ByteBuffer.allocate(1024);

    /** パケットヘッダー
     *
     */
    private Header header;

    /** パケット ボディ一覧
     *
     */
    private List<Payload> payload;

    /** コンストラクタ
     *
     * @param header
     * @param payload
     */
    public Packet(Header header,List<Payload> payload) {
        this.header = header;
        this.payload = payload;
    }

    /** コンストラクタ
     *
     * @param src
     * @throws ParseException
     */
    public Packet(byte[] src) throws ParseException {
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
        
        //各データを取得する
        this.header = new Header(this.src);
        this.src = this.header.parse();

        this.payload = new ArrayList<Payload>();
        while (this.src.length > 0){
            try {
                Payload p = new Payload(this.src);
                this.src = p.parse();
                this.payload.add(p);
            }catch (ParseException e){
                //ParseExceptionが発生したら処理 終わり。
                break;
            }
        }

        //処理対象じゃなかった残りのデータを返す
        return this.src;
    }

    @Override
    public byte[] toByteArray() {
        if(this.src == null) {
            byteBuf.clear();
            byteBuf.put(this.header.toByteArray());
            for (Payload p : this.payload) {
                byteBuf.put(p.toByteArray());
            }
            byteBuf.flip();
            this.src = new byte[byteBuf.limit()];
            byteBuf.get(this.src);
            Log.d(TAG, "src size:" + this.src.length);
        }
        return this.src;
    }

    /** ヘッダー取得
     *
     * @return
     */
    public Header getHeader(){
        return this.header;
    }

    /** ペイロード取得
     *
     * @return
     */
    public List<Payload> getPayload(){
        return this.payload;
    }
}
