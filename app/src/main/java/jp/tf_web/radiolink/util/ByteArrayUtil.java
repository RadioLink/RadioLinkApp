package jp.tf_web.radiolink.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by furukawanobuyuki on 2015/12/08.
 */
public class ByteArrayUtil {
    /** short[] -> byte[] 変換
     *
     * @param pcm short配列
     * @param length short配列の大きさ
     * @return
     */
    public static byte[] shortArr2byteArr(final short[] pcm,final int length){
        byte[] dst = new byte[length*2];
        ByteBuffer buf = ByteBuffer.wrap(dst);
        buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm,0,length);
        return dst;
    }

    /** byte[] -> short[] 変換
     *
     * @param pcm バイト配列
     * @param length バイト配列の大きさ
     * @return
     */
    public static short[] byteArr2shortArr(final byte[] pcm,final int length){
        short[] dst = new short[length/2];
        ByteBuffer buf = ByteBuffer.wrap(pcm);
        buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(dst);
        return dst;
    }

    /** 文字列に変換
     *
     * @param src
     * @return
     */
    public static String toHexString(final byte[] src){
        StringBuffer buf = new StringBuffer();
        for(byte b:src){
            String s = String.format("%02x", b & 0xff);
            buf.append(s).append(",");
        }
        return buf.toString();
    }
}
