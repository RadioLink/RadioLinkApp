package jp.tf_web.radiolink.net.protocol.packet;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.Arrays;

/** RadioLink パケット ペイロード
 *
 * Created by furukawanobuyuki on 2016/01/06.
 */
public class Payload implements PacketInterface {
    private static String TAG = "Payload";

    //バイト配列変換用のバイトバッファー
    private ByteBuffer byteBuf = ByteBuffer.allocate(1024);

    //データをバイト配列にした物
    private byte[] src;

    /** ペイロードデータサイズ
     *  short 2byte
     */
    private short size;

    /** ペイロードデータ
     *
     */
    private byte[] data;

    /** コンストラクタ
     *
     * @param size
     * @param data
     */
    public Payload(short size, byte[] data){
        this.size = size;
        this.data = data;
        //Log.d(TAG,"Payload() size:"+size);
    }

    /** コンストラクタ
     *
     * @param src
     * @throws ParseException
     */
    public Payload(byte[] src) throws ParseException {
        this.src = src.clone();
    }

    /** 構文解析する
     *
     * @throws ParseException
     */
    @Override
    public byte[] parse() throws ParseException {
        if(this.src == null){
            //エラー
            StackTraceElement[] ste = new Throwable().getStackTrace();
            throw new ParseException("src is null",ste[0].getLineNumber());
        }

        //各データを取得する
        byteBuf.clear();
        byteBuf.put(this.src);
        byteBuf.flip();

        //short 2バイト
        byte[] s = new byte[2];
        byteBuf.get(s,0,s.length);
        size = (short)((s[0]<< 8)+s[1]);
        if(size < 0) size = (short)(256+size);
        //Log.d(TAG, "parse() size:" + size+" "+Short.SIZE);

        data = new byte[size];
        byteBuf.get(data, 0, data.length);

        if(byteBuf.position() == 0){
            //残りデータが無かった
            return null;
        }

        byte[] result = new byte[byteBuf.limit() - byteBuf.position()];
        byteBuf.get(result, 0, result.length);

        return result;
    }

    /** バイト配列に変換する
     *
     * @return
     */
    @Override
    public byte[] toByteArray() {
        if(this.src == null) {
            byteBuf.clear();
            byteBuf.putShort(size);
            byteBuf.put(data);
            byteBuf.flip();
            this.src = new byte[byteBuf.limit()];
            byteBuf.get(this.src);
            Log.d(TAG, "src size:" + this.src.length);
        }
        return this.src;
    }

    /** サイズ取得
     *
     * @return
     */
    public short getSize(){
        return this.size;
    }

    /** ペイロードデータ
     *
     * @return
     */
    public byte[] getData(){
        return this.data;
    }
}
