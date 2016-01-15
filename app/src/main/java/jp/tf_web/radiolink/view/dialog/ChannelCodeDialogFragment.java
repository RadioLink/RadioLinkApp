package jp.tf_web.radiolink.view.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.nifty.cloud.mb.core.NCMBException;

import java.util.List;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.R;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.NCMBUtil;
import jp.tf_web.radiolink.ncmb.db.listener.CreateChannelListener;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelListListener;
import jp.tf_web.radiolink.ncmb.db.listener.SetChannelIconImageListener;
import jp.tf_web.radiolink.util.BitmapUtil;
import jp.tf_web.radiolink.util.EditTextUtil;

/** チャンネルコード設定,検索 ダイアログ
 *
 * Created by furukawanobuyuki on 2016/01/14.
 */
public class ChannelCodeDialogFragment extends DialogFragment {
    private static String TAG = "ChannelCodeDialogFragment";

    public static final String KEY_CHANNEL_CODE = "channel_code";

    //API処理をするユーテリティ
    private NCMBUtil ncmbUtil;

    //イベント通知先
    private ChannelCodeDialogFragmentListener listener;

    //チャンネルコード入力欄
    private EditText txtChannelCode;

    //チャンネルリスト
    private ListView listChannel;

    //チャンネルアダプター
    private ArrayAdapter<String> listChannelAdapter;

    /** リスナーを設定
     *
     * @param listener
     */
    public ChannelCodeDialogFragment setListener(final ChannelCodeDialogFragmentListener listener){
        this.listener = listener;
        return this;
    }

    /** 初期値設定
     *
     * @param savedInstanceState
     * @return
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.channel_code_dialog_fragment);
        dialog.setTitle(R.string.channel_code_dialog_title);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // OK ボタンのリスナ
        dialog.findViewById(R.id.btnOk).setOnClickListener(this.btnOkOnClickListener);

        // Close ボタンのリスナ
        dialog.findViewById(R.id.btnCancel).setOnClickListener(this.btnCancelOnClickListener);

        //チャンネルコード欄
        this.txtChannelCode = (EditText) dialog.findViewById(R.id.txtChannelCode);
        this.txtChannelCode.addTextChangedListener(this.txtChannelCodeChanged);
        EditTextUtil.setInputFilter(this.txtChannelCode, EditTextUtil.INPUT_FILTER_FORMAT_ASCII);

        //チャンネル一覧
        this.listChannel = (ListView) dialog.findViewById(R.id.listChannel);
        this.listChannel.setOnItemClickListener(listChannelOnItemClickListener);
        this.listChannelAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1);
        this.listChannel.setAdapter(this.listChannelAdapter);

        try {
            this.ncmbUtil = NCMBUtil.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dialog;
    }

    // OK ボタンのリスナ
    private View.OnClickListener btnOkOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(listener != null) {
                String channelCode = txtChannelCode.getText().toString();

                //チャンネル作成 & リスナーに通知
                selectChannel(channelCode);
            }
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


    // チャンネルコード 入力監視
    private TextWatcher txtChannelCodeChanged = new TextWatcher(){

        /** 操作前のEtidTextの状態
         *
         * @param s
         * @param start
         * @param count
         * @param after
         */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        /** 操作中のEtidTextの状態
         *
         * @param s
         * @param start
         * @param before
         * @param count
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        /** 操作後のEtidTextの状態
         *
         * @param s
         */
        @Override
        public void afterTextChanged(Editable s) {
            //ここで入力値に応じたチャンネル一覧を検索して表示する
            final String searchChannelCode = s.toString();
            Log.d(TAG,"searchChannelCode:"+searchChannelCode);

            //リストを初期化
            listChannelAdapter.clear();
            listChannelAdapter.notifyDataSetChanged();

            //最新 100件 を取得する
            ncmbUtil.getChannelList(null, 100, new GetChannelListListener(){

                @Override
                public void success(List<Channel> channels) {
                    // チャンネル一覧にアイテムを追加します
                    if(channels.size() == 0){
                        //何もしない
                        return;
                    }
                    Log.d(TAG,"channels size:"+channels.size());
                    //一覧元のデータを更新
                    listChannelAdapter.clear();
                    for(Channel c:channels) {
                        if(c.getChannelCode().indexOf(searchChannelCode) != -1) {
                            //チャンネルコードのカスタムリストを作る
                            listChannelAdapter.add(c.getChannelCode());
                        }
                    }
                    //更新を通知
                    listChannelAdapter.notifyDataSetChanged();
                }

                @Override
                public void error(NCMBException e) {
                    //何もしない
                    Log.d(TAG,"e:"+e.getMessage());
                }
            });

        }
    };

    //チャンネルリスト選択時のイベント
    private AdapterView.OnItemClickListener listChannelOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView listView = (ListView) parent;
            //クリックされたアイテムを取得して チャンネルコードを入力
            String item = (String) listView.getItemAtPosition(position);
            txtChannelCode.setText(item);
            Toast.makeText(getActivity().getApplicationContext(), item, Toast.LENGTH_SHORT).show();
        }
    };


    /** チャンネル作成
     *
     * @param channelCode
     */
    private void selectChannel(final String channelCode){
        //検索
        ncmbUtil.getChannelList(channelCode, new GetChannelListListener() {
            @Override
            public void success(List<Channel> channels) {
                if((channels != null) && (channels.size() > 0)){
                    //既存のチャンネルが見つかった事をリスナーに通知
                    Bundle params = new Bundle();
                    params.putString(KEY_CHANNEL_CODE, channelCode);
                    listener.ok(params);

                    //ダイアログを閉じる
                    dismiss();
                }
                else{
                    //新規作成
                    createChannel(channelCode);
                }
            }

            @Override
            public void error(NCMBException e1) {
                //検索失敗
                Toast.makeText(getActivity().getApplicationContext(),"Channel作成に失敗しました。",Toast.LENGTH_SHORT).show();
            }
        });

    }

    /** チャンネル新規作成
     *
     * @param channelCode
     */
    private void createChannel(final String channelCode){
        try {
            //新規作成
            ncmbUtil.createChannel(channelCode, new CreateChannelListener() {
                @Override
                public void success(Channel channel) {
                    Log.d(TAG, "createChannel success " + channel.getChannelCode());

                    //リスナーに通知
                    Bundle params = new Bundle();
                    params.putString(KEY_CHANNEL_CODE, channelCode);
                    listener.ok(params);

                    //ダイアログを閉じる
                    dismiss();
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "createChannel error " + e);
                    Toast.makeText(getActivity().getApplicationContext(),"Channel作成に失敗しました。",Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e2) {
            e2.printStackTrace();
            Toast.makeText(getActivity().getApplicationContext(),"Channel作成に失敗しました。",Toast.LENGTH_SHORT).show();
        }
    }
}
