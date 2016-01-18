package jp.tf_web.radiolink.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class BitmapUtil {
    /**
     *
     * bitmapをバイト配列に変換します。
     *
     * @param bitmap ビットマップ
     * @param format 圧縮フォーマット
     * @return
     */
    public static byte[] bmp2byteArray(Bitmap bitmap, Bitmap.CompressFormat format) {
        return bmp2byteArray(bitmap,format,100);
    }

    /**
     * bitmapをバイト配列に変換します。
     *
     * @param bitmap ビットマップ
     * @param format 圧縮フォーマット
     * @param compressVal 圧縮率
     * @return
     */
    public static byte[] bmp2byteArray(Bitmap bitmap, Bitmap.CompressFormat format, int compressVal) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, compressVal, baos);
        return baos.toByteArray();

    }

    /** バイト配列をbitmapに変換します。
     *
     * @param bytes
     * @return
     */
    public static Bitmap byte2bmp(byte[] bytes) {
        Bitmap bmp = null;
        if (bytes != null) {
            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        return bmp;
    }

    /** バイト配列をbitmapに変換します。
     *
     * @param bytes
     * @param sampleSize
     * @return
     */
    public static Bitmap byte2bmp(byte[] bytes,int sampleSize) {
        Bitmap bmp = null;
        if (bytes != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;

            try {
                InputStream stream = new ByteArrayInputStream(bytes, 0, bytes.length);
                bmp = BitmapFactory.decodeStream(stream, null, options);
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bmp;
    }
}
