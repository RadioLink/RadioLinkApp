package jp.tf_web.radiolink.view.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.view.ContextThemeWrapper;
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

import com.google.zxing.integration.android.IntentResult;
import com.nifty.cloud.mb.core.NCMBException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.R;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.NCMBUtil;
import jp.tf_web.radiolink.ncmb.db.listener.CreateChannelListener;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelListListener;
import jp.tf_web.radiolink.ncmb.db.listener.SetChannelIconImageListener;
import jp.tf_web.radiolink.qrcode.QRCodeUtil;
import jp.tf_web.radiolink.qrcode.ScanQRCodeResultListener;
import jp.tf_web.radiolink.scheme.ShareActionUtil;
import jp.tf_web.radiolink.util.BitmapUtil;
import jp.tf_web.radiolink.util.EditTextUtil;

/** チャンネルコード設定,検索 ダイアログ
 *
 * Created by furukawanobuyuki on 2016/01/14.
 */
public class ChannelCodeDialogFragment extends DialogFragment {
    private static String TAG = "ChannelCodeDialogFragment";

    public static final String KEY_CHANNEL_CODE = "channel_code";

    //ハンドラー
    private Handler handler;

    //API処理をするユーテリティ
    private NCMBUtil ncmbUtil;

    //イベント通知先
    private DialogFragmentListener listener;

    //チャンネルコード入力欄
    private EditText txtChannelCode;

    //チャンネルリスト
    private ListView listChannel;

    //チャンネルアダプター
    private ChannelCodeDialogListArrayAdapter listChannelAdapter;

    /** リスナーを設定
     *
     * @param listener
     */
    public ChannelCodeDialogFragment setListener(final DialogFragmentListener listener){
        this.listener = listener;
        this.handler = new Handler();

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
        dialog.setContentView(R.layout.channel_code_dialog_fragment);
        dialog.setTitle(R.string.channel_code_dialog_title);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // OK ボタンのリスナ
        dialog.findViewById(R.id.btnOk).setOnClickListener(this.btnOkOnClickListener);

        // Close ボタンのリスナ
        dialog.findViewById(R.id.btnCancel).setOnClickListener(this.btnCancelOnClickListener);

        //QRコードリーダー起動
        dialog.findViewById(R.id.btnQRCodeReader).setOnClickListener(this.btnQRCodeReaderOnClickListener);

        //チャンネルコード欄
        this.txtChannelCode = (EditText) dialog.findViewById(R.id.txtChannelCode);
        this.txtChannelCode.addTextChangedListener(this.txtChannelCodeChanged);
        EditTextUtil.setInputFilter(this.txtChannelCode, EditTextUtil.INPUT_FILTER_FORMAT_ASCII);

        //チャンネル一覧
        this.listChannel = (ListView) dialog.findViewById(R.id.listChannel);
        this.listChannel.setOnItemClickListener(listChannelOnItemClickListener);

        this.listChannelAdapter = new ChannelCodeDialogListArrayAdapter(getActivity().getApplicationContext());
        this.listChannel.setAdapter(this.listChannelAdapter);

        try {
            this.ncmbUtil = NCMBUtil.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO: 過去にJOINしたことのあるチャンネル一覧を取得して一覧に設定する
        //List<Channel> channels = new ArrayList<>();

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
            final String channelCode = s.toString();
            Log.d(TAG, "searchChannelCode:" + channelCode);

            searchChannelCode(channelCode);
        }
    };

    /** 非同期でチャンネルコードで検索して一覧を更新
     *
     * @param channelCode
     */
    private void searchChannelCode(final String channelCode){
        final AsyncTask<String, Void, Void> searchTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                final String channelCode = params[0];
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //チャンネルコードで検索して一覧を更新
                        getChannelList(channelCode);
                    }
                });
                return null;
            }
        };
        searchTask.execute(channelCode);
    }

    /** チャンネルコードで検索して一覧を更新
     *
     * @param channelCode
     */
    private void getChannelList(final String channelCode){
        //リストを初期化
        listChannelAdapter.clear();

        //最新 100件 を取得する
        //TODO: 過去に作成されたチャンネルが検索対象にならないので何か処理方法を考える
        ncmbUtil.getChannelList(null, 100, new GetChannelListListener() {

            @Override
            public void success(final List<Channel> channels) {
                // チャンネル一覧にアイテムを追加します
                if (channels.size() == 0) {
                    //何もしない
                    return;
                }
                Log.d(TAG, "channels size:" + channels.size());

                //一覧元のデータを更新
                listChannelAdapter.clear();
                for(Channel c:channels) {
                    if(c.getChannelCode().indexOf(channelCode) != -1) {
                        //チャンネルコードのカスタムリストを作る
                        listChannelAdapter.add(c);
                    }
                }
            }

            @Override
            public void error(NCMBException e) {
                //何もしない
                Log.d(TAG, "e:" + e.getMessage());
            }
        });
    }

    //チャンネルリスト選択時のイベント
    private AdapterView.OnItemClickListener listChannelOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView listView = (ListView) parent;

            //クリックされたアイテムを取得して チャンネルコードを入力
            Channel channel = (Channel) listView.getItemAtPosition(position);
            txtChannelCode.setText(channel.getChannelCode());

            //Toast.makeText(getActivity().getApplicationContext(), channel.getChannelCode(), Toast.LENGTH_SHORT).show();
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

                    //チャンネルコードの長さを確認
                    if(Config.CHANNEL_CODE_MINI_LENGTH >= channelCode.length()){
                        String message = getString(R.string.channel_code_mini_length);
                        message = message.replace("{{LENGTH}}",String.valueOf(Config.CHANNEL_CODE_MINI_LENGTH));
                        Toast.makeText(getActivity().getApplicationContext(), message,Toast.LENGTH_LONG).show();
                        return;
                    }

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

    //QRコード読み取りボタン
    private View.OnClickListener btnQRCodeReaderOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            //QRコードを読み込む
            QRCodeUtil.scanQRCode( ChannelCodeDialogFragment.this );
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult");
        //QRコードスキャン結果を受け取る
        boolean isResult = QRCodeUtil.onActivityResult(requestCode, resultCode, intent, new ScanQRCodeResultListener() {
            @Override
            public void success(IntentResult scanResult) {
                String shareUri = scanResult.getContents();
                Log.d(TAG, "shareUri " + shareUri);
                if((shareUri == null)||(shareUri.length() == 0)){
                    Log.d(TAG, "shareURI is null");
                    return;
                }
                if(shareUri.indexOf(Config.SCHEME_REDIRECT_API) == -1){
                    //対象外のQRコード
                    Log.d(TAG, "SCHEME_REDIRECT_API not found.");
                    return;
                }

                try {
                    String shareScheme = URLDecoder.decode(shareUri.replace(Config.SCHEME_REDIRECT_API, ""), "UTF-8");
                    Uri shareSchemeUri = Uri.parse(shareScheme);
                    String channelCode = shareSchemeUri.getQueryParameter(ShareActionUtil.KEY_VALUE_SHARE_CHANNEL_CODE);
                    Log.d(TAG, "channelCode" + channelCode);

                    //チャンネルコードを チャンネルコード欄に設定
                    txtChannelCode.setText(channelCode);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
