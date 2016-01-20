package jp.tf_web.radiolink.view.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import jp.tf_web.radiolink.R;
import jp.tf_web.radiolink.ncmb.db.NCMBUtil;
import jp.tf_web.radiolink.util.EditTextUtil;

/**
 * Created by furukawanobuyuki on 2016/01/19.
 */
public class JoinChannelDialogFragment extends DialogFragment {
    private static String TAG = "JoinChannelDialogFragment";

    //イベント通知先
    private DialogFragmentListener listener;

    //チャンネルコード
    private String channelCode;

    /** リスナーを設定
     *
     * @param listener
     */
    public JoinChannelDialogFragment setListener(final DialogFragmentListener listener){
        this.listener = listener;
        return this;
    }

    public JoinChannelDialogFragment setChannelCode(final String channelCode){
        this.channelCode = channelCode;
        return this;
    }

    /** 初期値設定
     *
     * @param savedInstanceState
     * @return
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(new ContextThemeWrapper(getActivity(), R.style.AppDialogTheme));
        dialog.setContentView(R.layout.join_channel_dialog_fragment);
        dialog.setTitle(R.string.join_channel_dialog_title);

        //チャンネルコードを画面に設定
        EditText txtChannelCode = (EditText) dialog.findViewById(R.id.txtChannelCode);
        txtChannelCode.setText(channelCode);

        // OK ボタンのリスナ
        dialog.findViewById(R.id.btnOk).setOnClickListener(this.btnOkOnClickListener);

        // Close ボタンのリスナ
        dialog.findViewById(R.id.btnCancel).setOnClickListener(this.btnCancelOnClickListener);

        return dialog;
    }

    // OK ボタンのリスナ
    private View.OnClickListener btnOkOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(listener != null) {
                //リスナーに通知するパラメータを作る
                Bundle params = new Bundle();
                listener.ok(params);
            }
            dismiss();
        }
    };

    // Close ボタンのリスナ
    private View.OnClickListener btnCancelOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(listener != null) {
                //リスナーに通知するパラメータを作る
                Bundle params = new Bundle();
                listener.cancel(params);
            }

            dismiss();
        }
    };
}
