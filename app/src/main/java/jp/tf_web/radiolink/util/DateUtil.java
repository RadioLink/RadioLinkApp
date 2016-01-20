package jp.tf_web.radiolink.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by furukawanobuyuki on 2016/01/21.
 */
public class DateUtil {

    /** 日付をフォーマット指定して文字列で取得
     *
     * @param src
     * @param format
     * @return
     */
    public static String dateFormat(Date src,String format){
        return new SimpleDateFormat(format).format(src);
    }
}
