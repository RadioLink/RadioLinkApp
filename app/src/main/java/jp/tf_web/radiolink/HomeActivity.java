/** ホーム画面
 *
 */
package jp.tf_web.radiolink;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentResult;
import com.nifty.cloud.mb.core.NCMBException;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import jp.tf_web.radiolink.audio.AudioDeviceManager;
import jp.tf_web.radiolink.billing.InAppBillingUtil;
import jp.tf_web.radiolink.billing.InAppBillingUtilListener;
import jp.tf_web.radiolink.billing.util.IabResult;
import jp.tf_web.radiolink.billing.util.Inventory;
import jp.tf_web.radiolink.billing.util.Purchase;
import jp.tf_web.radiolink.bluetooth.MediaButtonReceiver;
import jp.tf_web.radiolink.bluetooth.MediaButtonReceiverListener;
import jp.tf_web.radiolink.controller.AudioController;
import jp.tf_web.radiolink.controller.AudioControllerListener;
import jp.tf_web.radiolink.controller.NetWorkController;
import jp.tf_web.radiolink.controller.NetWorkControllerListener;
import jp.tf_web.radiolink.ncmb.gcm.GcmSendPushListener;
import jp.tf_web.radiolink.ncmb.gcm.GcmUtil;
import jp.tf_web.radiolink.ncmb.gcm.GcmUtilRegistrationListener;
import jp.tf_web.radiolink.ncmb.db.NCMBUtil;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.User;
import jp.tf_web.radiolink.ncmb.db.listener.ExitChannelUserlistener;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelListListener;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelUserListListener;
import jp.tf_web.radiolink.ncmb.db.listener.JoinChannelUserlistener;
import jp.tf_web.radiolink.ncmb.db.listener.LoginListener;
import jp.tf_web.radiolink.ncmb.db.listener.SetChannelIconImageListener;
import jp.tf_web.radiolink.ncmb.db.listener.SigninListener;
import jp.tf_web.radiolink.ncmb.gcm.service.GcmListenerServiceReceiver;
import jp.tf_web.radiolink.ncmb.gcm.service.GcmListenerServiceReceiverListener;
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.udp.service.UDPService;
import jp.tf_web.radiolink.qrcode.QRCodeUtil;
import jp.tf_web.radiolink.qrcode.ScanQRCodeResultListener;
import jp.tf_web.radiolink.sensor.LightSensorManager;
import jp.tf_web.radiolink.sensor.LightSensorManagerListener;
import jp.tf_web.radiolink.util.BitmapUtil;
import jp.tf_web.radiolink.scheme.ShareActionUtil;
import jp.tf_web.radiolink.util.CameraUtil;
import jp.tf_web.radiolink.util.CameraUtilListener;
import jp.tf_web.radiolink.util.PermissionUtil;
import jp.tf_web.radiolink.view.dialog.ChannelCodeDialogFragment;
import jp.tf_web.radiolink.view.dialog.DialogFragmentListener;
import jp.tf_web.radiolink.view.dialog.JoinChannelDialogFragment;


public class HomeActivity extends Activity
{
    private static final String TAG = "HomeActivity";

    //ハンドラー
    private Handler handler;

    //ボリューム
    private ImageView imgVolume;

    //再生,録音,通信処理をするクラス
    private AudioController audioController;

    //ネットワーク関係の処理をするクラス
    private NetWorkController netWorkController;

    //照度センサー
    private LightSensorManager lightSensorManager;

    //課金処理の実装
    private InAppBillingUtil inAppBillingUtil;

    //API処理をするユーテリティ
    private NCMBUtil ncmbUtil;

    //JION中のチャンネル
    private Channel activeChannel;

    //JOIN前の一時的に取得したチャンネル
    private Channel temporaryJoinChannel;

    //カメラボタン
    private ImageButton btnCamera;

    //チャンネルに登録中のユーザー数
    private TextView lblUserCount;

    //チャンネルコード ラベル
    private TextView lblChannelCode;

    //メディアボタン
    private Button btnMedia;

    //マイク ミュートのラベル
    private TextView lblMicMute;

    //マイク ミュートボタン
    private Button btnMicMute;

    //マイク ミュート時のオーバーレイ画像
    private ImageView imgMicMute;

    //購入済みアイテム
    private Purchase purchase = null;

    //メニュー
    private MenuItem actionItem;

    //GCM 通知 受信
    private GcmListenerServiceReceiver gcmListenerServiceReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.d(TAG, "onCreate() "+this);
        Log.d(TAG, " DEVICE:" + Build.DEVICE + " MODEL:" + Build.MODEL);

        handler = new Handler();

        //API処理をするユーテリティ
        ncmbUtil = NCMBUtil.getInstance(getApplicationContext(), Config.NCMB_APP_KEY, Config.NCMB_CLIENT_KEY);

        //課金 処理の為の初期化
        if(inAppBillingUtil == null) {
            try {
                inAppBillingUtil = new InAppBillingUtil(getApplicationContext(), inAppBillingUtilListener);
                inAppBillingUtil.setup();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //GCMの通知を受信するレシーバーを有効にする
        gcmListenerServiceReceiver = new GcmListenerServiceReceiver(getApplicationContext(),gcmListenerServiceReceiverListener);
        gcmListenerServiceReceiver.registerReceiver();

        //録音 音量 表示欄
        imgVolume = (ImageView) findViewById(R.id.imgVolume);

        //チャンネルユーザー数
        lblUserCount = (TextView)findViewById(R.id.lblUserCount);

        //チャンネルコード
        lblChannelCode = (TextView)findViewById(R.id.lblChannelCode);
        lblChannelCode.setOnClickListener(this.lblChannelCodeOnClickListener);

        //カメラボタン
        btnCamera = (ImageButton) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(this.btnCameraOnClickListener);

        //メディア切り替えボタン
        btnMedia = (Button)findViewById(R.id.btnMedia);
        btnMedia.setOnClickListener(btnMediaOnClickListener);

        //マイクのミュート
        lblMicMute = (TextView) findViewById(R.id.lblMicMute);
        imgMicMute = (ImageView) findViewById(R.id.imgMicMute);
        btnMicMute = (Button) findViewById(R.id.btnMicMute);
        btnMicMute.setOnClickListener(btnMicMuteOnClickListener);

        //M以上の場合パーミッションの確認をする
        checkPermissions();

        //起動時のサインイン,ログイン処理
        login();

        //起動時パラメータを取得
        onAction();
    }

    /** 起動時パラメータを取得
     *
     * @return
     */
    private boolean onAction(){
        boolean result = false;

        String action = getIntent().getAction();
        Log.d(TAG, "action:" + action);

        //SCHEME起動された場合 チャンネルコードを取得する
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                Log.d(TAG, "uri " + uri.toString());
                final String channelCode = uri.getQueryParameter(ShareActionUtil.KEY_VALUE_SHARE_CHANNEL_CODE);
                Log.d(TAG, "channelCode " + channelCode);
                //Toast.makeText(getApplicationContext(),"channelCode:"+channelCode,Toast.LENGTH_SHORT).show();

                result = true;

                //JOIN 確認ダイアログを表示する
                showJoinChannelConfirm(channelCode);
            }
        }
        return result;
    }

    /** 起動済みでスキーム起動された場合
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");
        //起動時のインテントを設定する
        setIntent(intent);
        //起動時パラメータを取得
        onAction();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult()");
        boolean isResult = false;

        //カメラからデータを取得した時
        if(isResult == false) {
            isResult = CameraUtil.onActivityResult(HomeActivity.this,requestCode,resultCode,data, cameraUtilListener);
        }

        // 購入結果をActivityが受け取るための設定
        if(isResult == false) {
            if(inAppBillingUtil != null) {
                isResult = inAppBillingUtil.onActivityResult(requestCode, resultCode, data);
            }
        }

        if(isResult == false) {
            //通常の onActivityResult を実行
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //許可ダイアログに対してユーザーが選択した結果の受け取り
        PermissionUtil.getInstance().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /** 終了処理
     *
     */
    private void destroy() {
        //停止
        stop();

        //課金処理の破棄
        if(inAppBillingUtil != null) {
            inAppBillingUtil.dispose();
        }

        //audioManagerを破棄
        if(audioController != null) {
            audioController.destroy();
        }

        //ネットワーク監視等の利用停止
        if(netWorkController != null){
            netWorkController.destroy();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        //アクティブなチャンネルが設定済みの場合は Exitしてから 終了処理をする
        if(activeChannel != null){
            exitChannel(activeChannel, new ExitChannelUserlistener() {
                @Override
                public void success() {
                    //終了処置
                    destroy();
                }

                @Override
                public void error(NCMBException e) {
                    destroy();
                }
            });
        }
        else{
            //終了処置
            destroy();
        }

        super.onDestroy();
    }

    /** メニューを画面に追加
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // ShareActionProvider の設定
        actionItem = menu.findItem(R.id.actionShareAlways);
        ShareActionProvider actionProvider = (ShareActionProvider) actionItem.getActionProvider();

        ShareActionUtil.getInstance().setActionProvider(actionProvider);

        return true;
    }

    /** メニュー選択時の処理
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /** チャンネルを設定
     *
     * @param channel
     */
    private void setActiveChannel(Channel channel){

        this.activeChannel = channel;
        if(this.audioController != null) {
            this.audioController.setActiveChannel(channel);
        }

        if(channel == null) {
            //this.lblChannelCode.setText("");
            this.lblUserCount.setText("0");
            ShareActionUtil.getInstance().removeShareIntent();
            this.btnCamera.setImageResource(R.drawable.camera01);
            return;
        }

        //チャンネルコードを画面に表示
        this.lblChannelCode.setText(channel.getChannelCode());

        // ShareActionProviderのチャンネルコードを更新 インテントの設定
        ShareActionUtil.getInstance().setShareIntent(getApplicationContext(), channel.getChannelCode());

        //チャンネルの画像をボタンに設定
        byte[] icon = channel.getIcon();
        if(icon != null) {
            this.btnCamera.setImageBitmap(BitmapUtil.byte2bmp(icon));
        }

        //チャンネルのユーザー数を取得
        ncmbUtil.getChannelUserList(channel, new GetChannelUserListListener() {
            @Override
            public void success(Channel channel) {

                final int cnt = channel.getChannelUserList().size();
                lblUserCount.setText( String.valueOf(cnt) );
            }

            @Override
            public void error(NCMBException e) {

            }
        });
    }

    /** サインイン,ログイン処理
     *
     */
    private void login(){
        User currentUser = ncmbUtil.getCurrentUser();
        Log.d(TAG, "currentUser " + currentUser);
        if(currentUser == null) {
            ncmbUtil.login(new User(Build.MODEL, "password"), new LoginListener() {
                @Override
                public void success(User user) {
                    Log.d(TAG, "login success " + user);
                    //User currentUser = ncmbUtil.getCurrentUser();
                    //Log.d(TAG, "login currentUser " + currentUser + " nickName:" + currentUser.getNickName());

                    //ユーザ登録に成功していたら GCM デバイス登録をする
                    GcmUtil.getInstance().registration(user,HomeActivity.this.gcmUtilRegistrationListener);
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "login error " + e);
                    //ログイン失敗の場合
                    //TODO: ユーザー新規作成をしてみる
                    User user = new User(Build.MODEL, "password");
                    user.setNickName(Build.DEVICE);

                    //ユーザー登録
                    ncmbUtil.signin(user, new SigninListener() {
                        @Override
                        public void success(User user) {
                            Log.d(TAG, "signin success " + user);

                            //ログイン
                            ncmbUtil.login(new User(Build.MODEL, "password"), new LoginListener() {
                                @Override
                                public void success(User user) {
                                    //ユーザ登録に成功していたら GCM デバイス登録をする
                                    GcmUtil.getInstance().registration(user, HomeActivity.this.gcmUtilRegistrationListener);
                                }

                                @Override
                                public void error(NCMBException e) {

                                }
                            });

                        }

                        @Override
                        public void error(NCMBException e) {
                            Log.e(TAG, "signin error " + e);
                        }
                    });
                }
            });
        }
        else{
            //ユーザ登録に成功していたら GCM デバイス登録をする
            GcmUtil.getInstance().registration(currentUser,HomeActivity.this.gcmUtilRegistrationListener);
        }
    }
    //各クラスの初期化
    private void initialize(){

        //再生,録音,通信 処理クラスを初期化
        if(audioController == null) {
            audioController = new AudioController(getApplicationContext(), Config.SAMPLE_RATE_IN_HZ, Config.OPUS_FRAME_SIZE * 2, audioControllerListener);
        }

        //ネットワーク接続状態の監視
        if(netWorkController == null){
            netWorkController = new NetWorkController(getApplicationContext(),netWorkControllerListener);
        }

        //照度センサーを初期化
        if(lightSensorManager == null) {
            lightSensorManager = new LightSensorManager(getApplicationContext(), lightSensorManagerListener);
        }
    }

    //各クラスの開始
    private void start(){

        //各クラスの初期化
        initialize();

        //再生 開始
        audioController.start(AudioManager.STREAM_MUSIC);

        //ネットワーク監視を開始
        netWorkController.start();

        //照度センサーを利用 開始
        lightSensorManager.start();
    }

    //各クラスの終了
    private void stop(){

        //ネットワーク監視を停止
        if(netWorkController != null){
            netWorkController.stop();
            netWorkController = null;
        }

        //照度センサーを利用 停止
        if(lightSensorManager != null) {
            lightSensorManager.stop();
            lightSensorManager = null;
        }

        //録音 停止
        if(audioController != null) {
            audioController.stop();
            audioController = null;
        }

    }

    /** チャンネル未設定の場合
     *
     */
    private void isChannelCodeIsNull(){
        //チャンネル未設定の場合
        String msg = getString(R.string.channel_code_is_null);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

        //ここでChannelCode設定ダイアログを表示してみる
        showChannelCodeDialog();
    }

    //カメラボタン
    private View.OnClickListener btnCameraOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(HomeActivity.this.activeChannel != null) {

                //他人が作ったチャンネルはアイコン登録できない
                String activeChannelUserObjID = HomeActivity.this.activeChannel.getUser().getObjectId();
                String channelUserObjID = ncmbUtil.getCurrentUser().getObjectId();

                if((activeChannelUserObjID.equals(channelUserObjID)) == false){
                    //オーナーじゃない場合は画像の設定はできない
                    String msg = getString(R.string.channel_is_not_owner);
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                //カメラ,画像 選択 起動
                CameraUtil.requestGetImage(HomeActivity.this);
            }
            else{
                //チャンネル未設定の場合
                isChannelCodeIsNull();
            }

        }
    };

    //チャンネルコード入力ダイアログを表示する
    private void showChannelCodeDialog(){
        ChannelCodeDialogFragment dialogFragment = new ChannelCodeDialogFragment().setListener(channelCodeDialogFragmentListener);
        dialogFragment.show(getFragmentManager(), ChannelCodeDialogFragment.class.getSimpleName());
    }

    //チャンネルコードをタップ時
    private View.OnClickListener lblChannelCodeOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            //チャンネルコード入力ダイアログを表示する
            showChannelCodeDialog();
        }
    };

    /** チャンネルに JOIN する
     *
     * @param channelCode チャンネルコード
     */
    private boolean joinChannel(final String channelCode){
        Log.d(TAG,"joinChannel(channelCode)");
        if(channelCode == null){
            Log.d(TAG, "channelCode is null");
            return false;
        }

        //検索して見つかったチャンネルを選択
        ncmbUtil.getChannelList(channelCode, new GetChannelListListener() {
            @Override
            public void success(List<Channel> channels) {
                Log.d(TAG, "getChannelList success size:" + channels.size());
                if (channels.size() == 0) return;
                //検索して見つかったチャンネルを選択
                final Channel c = channels.get(0);

                //チャンネルに JOIN する
                joinChannel(c);
            }

            @Override
            public void error(NCMBException e) {
                Log.e(TAG, "getChannelList error " + e);
            }
        });

        return true;
    }

    /** チャンネルに JOIN する
     *
     * @param channel チャンネル
     */
    private boolean joinChannel(final Channel channel){
        Log.d(TAG, "joinChannel(channel)");
        if(channel == null){
            Log.d(TAG,"channel is null");
            return false;
        }

        //start処理完了後に join 処理をする join対象のチャンネルを保持しておく
        temporaryJoinChannel = channel;

        //チャンネルコードを仮設定　
        this.lblChannelCode.setText( channel.getChannelCode() );

        //アクティブなチャンネルが設定済みの場合は Exitしてから join する
        if(activeChannel != null){
            exitChannel(activeChannel, new ExitChannelUserlistener() {
                @Override
                public void success() {
                    //停止
                    stop();

                    //開始
                    start();
                }

                @Override
                public void error(NCMBException e) {

                }
            });
        }
        else {
            //初回だったので開始
            //停止
            stop();

            //開始
            start();
        }

        return true;
    }

    /** 指定チャンネルからEXITする
     *
     * @param channel
     */
    private void exitChannel(final Channel channel,final ExitChannelUserlistener listener){
        //チャンネルから Exit する
        ncmbUtil.exitChannelUser(channel, audioController.getPublicSocketAddress(), audioController.getLocalSocketAddress(), new ExitChannelUserlistener() {
            /** 成功
             *
             */
            @Override
            public void success() {
                //EXIT に成功
                //Toast.makeText(getApplicationContext(), "EXIT!", Toast.LENGTH_SHORT).show();

                //GCM 通知
                GcmUtil.getInstance().channelUpdateSendPush(getApplicationContext(), activeChannel, new GcmSendPushListener() {
                    @Override
                    public void success() {
                        Log.d(TAG,"GCM sendPush success");
                    }

                    @Override
                    public void error(NCMBException e) {
                        Log.d(TAG,"GCM sendPush error");
                    }
                });

                //GCM通知してから null を設定
                setActiveChannel(null);

                listener.success();
            }

            /** 失敗
             *
             * @param e
             */
            @Override
            public void error(NCMBException e) {
                Log.e(TAG, "error:" + e);
                listener.error(e);
            }
        });

    }

    /** 画面の背景色を設定
     *
     * @param color
     */
    private void setVolumeBackgroundColor(final int color){
        handler.post(new Runnable() {
            @Override
            public void run() {
                imgVolume.setBackgroundColor(color);
            }
        });
    }

    /** UIの有効,無効切り替え
     *
     * @param enabled
     */
    private void setUiEnabled(boolean enabled){
        btnCamera.setEnabled(enabled);
        lblChannelCode.setEnabled(enabled);
        actionItem.setEnabled(enabled);
    }

    /** 照度センサーのステータス受信
     *
     */
    private LightSensorManagerListener lightSensorManagerListener = new LightSensorManagerListener(){

        @Override
        public void onLightSensorChanged(SensorEvent event, float value) {
            //Log.d(TAG, "onLightSensorChanged value:" + value);

            //照度センサーの閾値 判定
            if(value < Config.LIGHT_SENSOR_THRESHOLD){
                //画面の操作を停止する
                setUiEnabled(false);
            }
            else{
                setUiEnabled(true);
            }
        }
    };

    /** 課金の結果リスナー
     *
     */
    private InAppBillingUtilListener inAppBillingUtilListener = new InAppBillingUtilListener(){

        /** セットアップ結果の通知
         *
         * @param result
         */
        @Override
        public void onIabSetupFinished(IabResult result) {
            if (!result.isSuccess()) {
                Log.d(TAG,"IAB セットアップ 失敗");
                return;
            }
            Log.d(TAG, "IAB セットアップ 成功");
        }

        /** 購入済みアイテムの取得結果の通知
         *
         * @param result
         * @param inv
         */
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            if (result.isFailure()) {
                Log.d(TAG, "IAB 購入済みアイテムの取得 失敗");
                return;
            }
            Log.d(TAG, "IAB 購入済みアイテムの取得 成功");

            // 購入済みアイテムの確認
            purchase = inv.getPurchase(Config.PRODUCT_ITEM_1_ID);
            if (purchase != null) {
                Log.d(TAG, "IAB 商品を購入済み");
                //購入済みなので 制限された機能の開放等を行う
                return;
            }
            Log.d(TAG, "IAB 商品を購入が可能");
        }

        /** 購入完了 結果の通知
         *
         * @param result
         * @param purchase
         */
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Log.d(TAG, "IAB 購入 キャンセル");
                String msg = (String) getString(R.string.purchase_cancel);
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();

                //NORMAL 受話器アイコン
                setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.NORMAL);

                return;
            }
            Log.d(TAG,"IAB 購入 成功");

            if (purchase.getSku().equals(Config.PRODUCT_ITEM_1_ID)) {
                Log.d(TAG, "IAB あなたの商品：" + Config.PRODUCT_ITEM_1_ID + "を購入しました。\n"
                        + "orderIdは：" + purchase.getOrderId() + "\n"
                        + "INAPP_PURCHASE_DATAのJSONは：" + purchase.getOriginalJson());

                //購入商品に一致する機能解除等を行う
                HomeActivity.this.purchase = purchase;
            }
        }
    };

    /** 録音,再生,送信,受信 処理のリスナー
     *
     */
    private AudioControllerListener audioControllerListener = new AudioControllerListener(){

        /** 初期化処理が完了して処理可能な状態になった事の通知
         *
         */
        @Override
        public void onActive(){
            if(temporaryJoinChannel == null){
                return;
            }

            //ここで初めて join 処理が可能
            ncmbUtil.joinChannelUser(temporaryJoinChannel, audioController.getPublicSocketAddress(), audioController.getLocalSocketAddress(), new JoinChannelUserlistener() {
                /** 成功
                 *
                 * @param channel
                 */
                @Override
                public void success(final Channel channel) {

                    //JOINに成功
                    setActiveChannel(channel);

                    Toast.makeText(getApplicationContext(), "JOIN!", Toast.LENGTH_SHORT).show();

                    //GCM 通知
                    GcmUtil.getInstance().channelUpdateSendPush(getApplicationContext(), activeChannel, new GcmSendPushListener() {
                        @Override
                        public void success() {
                            Log.d(TAG, "GCM sendPush success");
                        }

                        @Override
                        public void error(NCMBException e) {
                            Log.d(TAG, "GCM sendPush error");
                        }
                    });
                }

                /** 失敗
                 *
                 * @param e
                 */
                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "error:" + e);
                }
            });
        }

        /** 録音データが通知される
         *
         * @param packet 録音したデータのパケット
         * @param volume 録音ボリュームの最大値
         */
        @Override
        public void onAudioRecord(final Packet packet, short volume) {
            //volume閾値で画面を更新
            if(volume < Config.VOLUME_THRESHOLD){
                //背景色を設定
                setVolumeBackgroundColor(Color.TRANSPARENT);
                return;
            }
            setVolumeBackgroundColor(Color.RED);
        }

        /** データを受信した時通知される
         *
         * @param packet 受信したパケット
         */
        @Override
        public void onReceive(Packet packet) {

        }

        /** ミュート状態に変更があった事の通知
         *
         * @param isMute
         */
        @Override
        public void onMicrophoneMute(boolean isMute){
            String msg = ((isMute)?getString(R.string.mic_mute_message):getString(R.string.mic_mute_normal));
            Log.d(TAG,msg);
            Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();

            //画面にミュート状態を反映させる
            setMicMute(isMute);
        }
    };

    //カメラ,画像 取得時のリスナー
    private CameraUtilListener cameraUtilListener = new CameraUtilListener(){
        @Override
        public void onImage(final Bitmap bitmap) {
            if(bitmap == null) return;
            Log.d(TAG, "onImage bitmap w:"+bitmap.getWidth()+" h:"+bitmap.getHeight());

            //ChannelCodeのイメージとして保存する
            byte[] jpg = BitmapUtil.bmp2byteArray(bitmap, Bitmap.CompressFormat.JPEG);
            ncmbUtil.saveChannelIcon(activeChannel, jpg, new SetChannelIconImageListener() {

                @Override
                public void success(Channel channel) {
                    Log.d(TAG, "success");
                    //アイコンが設定されたので更新
                    setActiveChannel(channel);
                }

                @Override
                public void error(NCMBException e) {
                    e.printStackTrace();
                    Log.e(TAG, "e:" + e.getMessage());
                }
            });

            //GCMで通知
            GcmUtil.getInstance().channelUpdateSendPush(getApplicationContext(), activeChannel, new GcmSendPushListener() {
                @Override
                public void success() {
                    Log.d(TAG,"GCM sendPush success");
                }

                @Override
                public void error(NCMBException e) {
                    Log.d(TAG,"GCM sendPush error");
                }
            });
        }
    };

    /** GCM デバイス登録処理のリスナー
     *
     */
    private GcmUtilRegistrationListener gcmUtilRegistrationListener = new GcmUtilRegistrationListener(){

        @Override
        public void success(final User user) {
            Log.d(TAG,"GCM Registration success.");
            //Toast.makeText(getApplicationContext(),"GCM Registration success.",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void error(NCMBException saveErr) {
            Log.d(TAG, "GCM Registration error.");
            //Toast.makeText(getApplicationContext(),"GCM Registration error.",Toast.LENGTH_SHORT).show();
        }
    };


    /** チャンネルコード入力 ダイアログのリスナー
     *
     */
    private DialogFragmentListener channelCodeDialogFragmentListener = new DialogFragmentListener(){

        @Override
        public void ok(Bundle params) {
            //チャンネルコードが入力され OK を押された
            String channelCode = params.getString(ChannelCodeDialogFragment.KEY_CHANNEL_CODE);
            if((channelCode != null) && (channelCode.length() > 0)) {
                // join
                joinChannel(channelCode);
            }
        }

        @Override
        public void cancel(Bundle params) {
            //キャンセルされた

        }
    };

    //メディア切り替えボタン
    private View.OnClickListener btnMediaOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {

            Log.d(TAG,"btnMediaOnClickListener onClick");

            if(audioController == null){
                //チャンネル未設定の場合
                isChannelCodeIsNull();
                return;
            }

            //NORMAL → スピーカー → ヘッドセット
            Log.d(TAG,"AudioDeviceMode:"+audioController.getAudioDeviceMode());
            if(audioController.getAudioDeviceMode() == AudioDeviceManager.AUDIO_DEVICE_MODE.NORMAL){
                //スピーカー
                setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.SPEAKER);
            }
            else if(audioController.getAudioDeviceMode() == AudioDeviceManager.AUDIO_DEVICE_MODE.SPEAKER){
                    if(purchase == null) {
                        //ヘッドセット利用は有料
                        if (inAppBillingUtil == null) return;
                        inAppBillingUtil.onBuyButtonClicked(HomeActivity.this, Config.PRODUCT_ITEM_1_ID);
                        //購入結果は inAppBillingUtil リスナーに通知される
                    }
                    else{
                        //ヘッドセット
                        setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.HEADSET);
                    }
            }
            else {
                //NORMAL 受話器アイコン
                setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.NORMAL);
            }
        }

    };

    /** オーディオデバイスモードを切り替える
     *
     * @param mode
     */
    private void setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE mode){
        TextView lblMediaType = (TextView) findViewById(R.id.lblMediaType);
        if(mode == AudioDeviceManager.AUDIO_DEVICE_MODE.HEADSET){
            //ヘッドセット
            lblMediaType.setText(getText(R.string.media_type_headset));
            btnMedia.setBackgroundResource(R.drawable.headset16);
            audioController.setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.HEADSET);
        }
        else if(mode == AudioDeviceManager.AUDIO_DEVICE_MODE.SPEAKER){
            //スピーカー
            lblMediaType.setText(getText(R.string.media_type_speaker));
            btnMedia.setBackgroundResource(R.drawable.audio45);
            audioController.setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.SPEAKER);
        }
        else{
            //NORMAL 受話器アイコン
            lblMediaType.setText(getText(R.string.media_type_normal));
            btnMedia.setBackgroundResource(R.drawable.telephone46);
            audioController.setAudioDevice(AudioDeviceManager.AUDIO_DEVICE_MODE.NORMAL);
        }
    }
    /** GCMの通知を受信するリスナー
     *
     */
    private GcmListenerServiceReceiverListener gcmListenerServiceReceiverListener = new GcmListenerServiceReceiverListener(){

        @Override
        public void onUpdateChannel() {
            //GCM通知により チャンネル情報を更新する
            //TODO: 通知されたチャンネルと activeChannel が一致したら更新
            Log.d(TAG, "onUpdateChannel");
            if(activeChannel == null){
                return;
            }

            final String channelCode = activeChannel.getChannelCode();
            ncmbUtil.getChannelList(channelCode, new GetChannelListListener() {
                @Override
                public void success(List<Channel> channels) {
                    if (channels.size() == 0) {
                        return;
                    }
                    //チャンネルを更新
                    setActiveChannel(channels.get(0));
                }

                @Override
                public void error(NCMBException e) {

                }
            });
        }
    };

    /** JOIN 確認ダイアログを表示
     *
     * @param channelCode
     */
    private void showJoinChannelConfirm(final String channelCode){

        //チャンネルコードで チャンネルを検索して取得
        ncmbUtil.getChannelList(channelCode, new GetChannelListListener() {
            @Override
            public void success(List<Channel> channels) {
                Log.d(TAG, "channels size:" + channels.size());
                if (channels.size() == 0) {
                    //チャンネルが見つからなかった場合
                    String message = getString(R.string.channel_not_found);
                    Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT);
                    return;
                }

                final Channel channel = channels.get(0);

                //JOIN 確認ダイアログ
                JoinChannelDialogFragment dialogFragment = new JoinChannelDialogFragment();
                dialogFragment.setChannelCode( channel.getChannelCode());
                dialogFragment.setListener(new DialogFragmentListener() {
                    @Override
                    public void ok(Bundle params) {
                        //JOIN 処理をする
                        joinChannel(channel);
                    }

                    @Override
                    public void cancel(Bundle params) {

                    }
                });
                dialogFragment.show(getFragmentManager(), JoinChannelDialogFragment.class.getSimpleName());
            }

            @Override
            public void error(NCMBException e) {
                //チャンネルが見つからなかった場合
                String message = getString(R.string.channel_not_found);
                Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT);
            }
        });

    }

    //ネットワーク接続状態の変更等の通知
    private NetWorkControllerListener netWorkControllerListener = new NetWorkControllerListener(){

        /** ネットワーク接続状態に変更があった
         *
         * @param type
         * @param connected
         */
        @Override
        public void onConnectedChanged(int type, boolean connected) {

            if(connected) {
                //Toast.makeText(getApplicationContext(), "connect type:" + type, Toast.LENGTH_SHORT).show();
                Log.d(TAG,"connect activeChannel "+activeChannel);
                //JOIN済みの場合 再JOINして STUN アドレス解決等を行う
                if(activeChannel != null){
                    joinChannel( activeChannel );
                    activeChannel = null;
                }
            }
        }
    };

    /** パーミッションの確認
     *
     */
    private void checkPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            List<String> permissions = new ArrayList<String>(){
                {
                    add(Manifest.permission.CAMERA);
                    add(Manifest.permission.RECORD_AUDIO);
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            };
            PermissionUtil.getInstance().checkPermissions(this,permissions);
        }
    }

    //ミュートボタン クリックリスナー
    private View.OnClickListener btnMicMuteOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            if(audioController == null){
                //Channel未設定の場合
                isChannelCodeIsNull();
                return;
            }
            //マイク ミュート設定をトグル
            audioController.setMicrophoneMute(!audioController.isMicrophoneMute());
        }
    };

    /** マイクアイコンををミュートにする
     *
     * @param mute
     */
    private void setMicMute(final boolean mute){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(mute == true){
                    //ミュート
                    String txt = (String) getString(R.string.mic_mute);
                    lblMicMute.setText(txt);
                    imgMicMute.setVisibility(View.VISIBLE);
                }
                else{
                    //通常
                    String txt = (String) getString(R.string.mic_normal);
                    lblMicMute.setText(txt);
                    imgMicMute.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
}
