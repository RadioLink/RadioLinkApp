package jp.tf_web.radiolink.util;

import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;

/**
 * Created by furukawanobuyuki on 2016/01/15.
 */
public class EditTextUtil {

    //英数字半角の場合のフォーマット
    public static final String INPUT_FILTER_FORMAT_ASCII = "^[0-9a-zA-Z]+$";

    private  EditTextUtil(){}

    /** 入力バリデートを設定
     *
     * @param editText
     * @param formatTxt
     */
    public static void setInputFilter(EditText editText,final String formatTxt){
        //フィルターを作成
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.toString().matches(formatTxt)) {
                    return source;
                } else {
                    return "";
                }
            }
        };
        InputFilter[] filters = new InputFilter[] { inputFilter };
        editText.setFilters(filters);
    }
}
