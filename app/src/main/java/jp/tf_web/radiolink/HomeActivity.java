/** ホーム画面
 *
 */
package jp.tf_web.radiolink;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.Switch;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentResult;
import com.nifty.cloud.mb.core.NCMBException;

import java.net.InetSocketAddress;
import java.util.List;

import jp.tf_web.radiolink.billing.InAppBillingUtil;
import jp.tf_web.radiolink.billing.InAppBillingUtilListener;
import jp.tf_web.radiolink.billing.util.IabResult;
import jp.tf_web.radiolink.billing.util.Inventory;
import jp.tf_web.radiolink.billing.util.Purchase;
import jp.tf_web.radiolink.bluetooth.BluetoothAudioDeviceManager;
import jp.tf_web.radiolink.bluetooth.MediaButtonReceiver;
import jp.tf_web.radiolink.bluetooth.MediaButtonReceiverListener;
import jp.tf_web.radiolink.controller.AudioController;
import jp.tf_web.radiolink.controller.AudioControllerListener;
import jp.tf_web.radiolink.ncmb.gcm.GcmSendPushListener;
import jp.tf_web.radiolink.ncmb.gcm.GcmUtil;
import jp.tf_web.radiolink.ncmb.gcm.GcmUtilRegistrationListener;
import jp.tf_web.radiolink.ncmb.db.NCMBUtil;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.ChannelUser;
import jp.tf_web.radiolink.ncmb.db.User;
import jp.tf_web.radiolink.ncmb.db.listener.CreateChannelListener;
import jp.tf_web.radiolink.ncmb.db.listener.DeleteChannelListener;
import jp.tf_web.radiolink.ncmb.db.listener.ExitChannelUserlistener;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelListListener;
import jp.tf_web.radiolink.ncmb.db.listener.GetChannelUserListListener;
import jp.tf_web.radiolink.ncmb.db.listener.JoinChannelUserlistener;
import jp.tf_web.radiolink.ncmb.db.listener.LoginListener;
import jp.tf_web.radiolink.ncmb.db.listener.LogoutListener;
import jp.tf_web.radiolink.ncmb.db.listener.SetChannelIconImageListener;
import jp.tf_web.radiolink.ncmb.db.listener.SigninListener;
import jp.tf_web.radiolink.ncmb.db.listener.UpdateChannelUserListener;
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
import jp.tf_web.radiolink.view.dialog.ChannelCodeDialogFragment;
import jp.tf_web.radiolink.view.dialog.ChannelCodeDialogFragmentListener;


public class HomeActivity extends Activity
{
    private static final String TAG = "HomeActivity";

    //ハンドラー
    private Handler handler;

    //メインのレイアウト
    private FrameLayout mainLayout;

    //Bluetoothヘッドセットへの接続等
    private BluetoothAudioDeviceManager bluetoothAudioDeviceManager;

    //再生,録音,通信処理をするクラス
    private AudioController audioController;

    //MEDIA_BUTTONのクリック受け取り
    private MediaButtonReceiver mediaButtonReceiver;

    //照度センサー
    private LightSensorManager lightSensorManager;

    //課金処理の実装
    private InAppBillingUtil inAppBillingUtil;

    //API処理をするユーテリティ
    private NCMBUtil ncmbUtil;

    //JION中のチャンネル
    private Channel activeChannel;

    //カメラボタン
    private ImageButton btnCamera;

    //チャンネルに登録中のユーザー数
    private TextView lblUserCount;

    //チャンネルコード ラベル
    private TextView lblChannelCode;

    //ハンズフリー切り替えスイッチ
    private Switch btnHandsFreeSwitch;

    //メディアボタン
    private Button btnMedia;

    //購入済みアイテム
    private Purchase purchase = null;

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
            inAppBillingUtil = new InAppBillingUtil(getApplicationContext(), inAppBillingUtilListener);
            inAppBillingUtil.setup();
        }

        //GCMの通知を受信するレシーバーを有効にする
        gcmListenerServiceReceiver = new GcmListenerServiceReceiver(getApplicationContext(),gcmListenerServiceReceiverListener);
        gcmListenerServiceReceiver.registerReceiver();

        //メイン レイアウト
        mainLayout = (FrameLayout) findViewById(R.id.mainLayout);

        //チャンネルユーザー数
        lblUserCount = (TextView)findViewById(R.id.lblUserCount);

        //チャンネルコード
        lblChannelCode = (TextView)findViewById(R.id.lblChannelCode);
        lblChannelCode.setOnClickListener(this.lblChannelCodeOnClickListener);

        //カメラボタン
        btnCamera = (ImageButton) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(this.btnCameraOnClickListener);

        //ハンズフリー切り替えスイッチ
        btnHandsFreeSwitch = (Switch) findViewById(R.id.btnHandsFreeSwitch);
        btnHandsFreeSwitch.setOnCheckedChangeListener(btnHandsFreeSwitchOnCheckedChangeListener);

        //メディア切り替えボタン
        btnMedia = (Button)findViewById(R.id.btnMedia);
        btnMedia.setOnClickListener(btnMediaOnClickListener);

/*
        //開始
        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this.btnStartOnClickListener);

        //停止
        Button btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this.btnStopOnClickListener);

        //Channelに入る
        Button btnChannelJoin = (Button) findViewById(R.id.btnChannelJoin);
        btnChannelJoin.setOnClickListener(this.btnChannelJoinOnClickListener);

        //Channelから出る
        Button btnChannelExit = (Button) findViewById(R.id.btnChannelExit);
        btnChannelExit.setOnClickListener(this.btnChannelExitOnClickListener);

        //アイテム購入ボタン
        Button btnInAppBilling = (Button)findViewById(R.id.btnInAppBilling);
        btnInAppBilling.setOnClickListener(this.btnInAppBillingOnClickListener);

        //ユーザー作成
        Button btnSignin = (Button)findViewById(R.id.btnSignin);
        btnSignin.setOnClickListener(this.btnSigninOnClickListener);

        //ユーザー作成
        Button btnLogin = (Button)findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this.btnLoginOnClickListener);

        //ユーザー作成
        Button btnLogout = (Button)findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(this.btnLogoutOnClickListener);

        //チャンネル作成
        Button btnNewChannel = (Button)findViewById(R.id.btnNewChannel);
        btnNewChannel.setOnClickListener(this.btnNewChannelOnClickListener);

        //チャンネルアイコン設定
        Button btnGetChannelList = (Button)findViewById(R.id.btnGetChannelList);
        btnGetChannelList.setOnClickListener(this.btnGetChannelListOnClickListener);

        //チャンネル削除
        Button btnDeleteChannel = (Button)findViewById(R.id.btnDeleteChannel);
        btnDeleteChannel.setOnClickListener(this.btnDeleteChannelOnClickListener);

        //QRコードスキャン ボタン
        Button btnQRCodeScan = (Button)findViewById(R.id.btnQRCodeScan);
        btnQRCodeScan.setOnClickListener(this.btnQRCodeScanOnClickListener);
*/

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
        //SCHEME起動された場合 チャンネルコードを取得する
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                Log.d(TAG, "uri " + uri.toString());
                final String channelCode = uri.getQueryParameter(ShareActionUtil.KEY_VALUE_SHARE_CHANNEL_CODE);
                Log.d(TAG, "channelCode " + channelCode);
                Toast.makeText(getApplicationContext(),"channelCode:"+channelCode,Toast.LENGTH_SHORT).show();

                result = true;

                //チャンネルコードで チャンネルを検索して取得
                ncmbUtil.getChannelList(channelCode, new GetChannelListListener() {
                    @Override
                    public void success(List<Channel> channels) {
                        if(channels.size() != 0){
                            //TODO: ここでアクティブなチャンネルを直接設定せずに ユーザーに JOIN するか確認するダイアログ等を表示する
                            joinChannel(channels.get(0));
                        }
                    }

                    @Override
                    public void error(NCMBException e) {

                    }
                });

            }
        }
        return result;
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

        //QRコードスキャン結果を受け取る
        if(isResult == false) {
            isResult = QRCodeUtil.onActivityResult(requestCode, resultCode, data, new ScanQRCodeResultListener() {
                @Override
                public void success(IntentResult scanResult) {
                    Log.d(TAG, "QRCode "+scanResult.getContents());
                }
            });
        }

        if(isResult == false) {
            //通常の onActivityResult を実行
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        //各種 停止
        stop();

        //課金処理の破棄
        if(inAppBillingUtil != null) {
            inAppBillingUtil.dispose();
        }

        //audioManagerを破棄
        if(audioController != null) {
            audioController.destroy();
        }
    }

    /** メニューを画面に追加
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // ShareActionProvider の設定
        MenuItem actionItem = menu.findItem(R.id.actionShareAlways);
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
        this.audioController.setActiveChannel(channel);

        //チャンネルコードを画面に表示
        this.lblChannelCode.setText("");
        if(channel != null) {
            this.lblChannelCode.setText(channel.getChannelCode());
        }

        //チャンネルの画像をボタンに設定
        byte[] icon = this.activeChannel.getIcon();
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

        //Bluetoothヘッドセットを利用する
        if(bluetoothAudioDeviceManager == null) {
            bluetoothAudioDeviceManager = new BluetoothAudioDeviceManager(getApplicationContext());
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

        //Bluetoothヘッドセットがあればそれを使う
        bluetoothAudioDeviceManager.startVoiceRecognition();

        //MEDIA_BUTTON イベントを受信する
        mediaButtonReceiver = new MediaButtonReceiver(getApplicationContext(), mediaButtonReceiverListener);
        mediaButtonReceiver.registerReceiver();

        //再生 開始
        audioController.start(AudioManager.STREAM_MUSIC);

        //照度センサーを利用 開始
        lightSensorManager.start();
    }

    //各クラスの終了
    private void stop(){
        //録音 停止
        if(audioController != null) {
            audioController.stop();
        }

        //MEDIA_BUTTON イベントを受信する事を止める
        if(mediaButtonReceiver != null) {
            mediaButtonReceiver.unregisterReceiver();
        }

        //Bluetoothヘッドセットから切断
        if(bluetoothAudioDeviceManager != null) {
            bluetoothAudioDeviceManager.stopVoiceRecognition();
        }

        //照度センサーを利用 停止
        if(lightSensorManager != null) {
            lightSensorManager.stop();
        }

        //UDPサービス STOP
        UDPService.sendCmd(getApplicationContext(), UDPService.CMD_STOP);
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
                String msg = getString(R.string.channel_code_is_null);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }

        }
    };

    //チャンネルコードをタップ時
    private View.OnClickListener lblChannelCodeOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            // 一度 停止
            stop();

            // 開始
            start();

            //チャンネルコード入力ダイアログを表示する
            ChannelCodeDialogFragment exampleDialogFragment = new ChannelCodeDialogFragment().setListener(channelCodeDialogFragmentListener);
            exampleDialogFragment.show(getFragmentManager(), ChannelCodeDialogFragment.class.getSimpleName());
        }
    };

    /** チャンネルに JOIN する
     *
     * @param channelCode チャンネルコード
     */
    private boolean joinChannel(final String channelCode){
        Log.d(TAG,"joinChannel");
        if(channelCode == null){
            Log.d(TAG,"channelCode is null");
            return false;
        }
        if(audioController == null){
            Log.d(TAG,"audioController is null");
            return false;
        }

        //検索して見つかったチャンネルを選択
        ncmbUtil.getChannelList(channelCode,new GetChannelListListener() {
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
        Log.d(TAG,"joinChannel");
        if(channel == null){
            Log.d(TAG,"channel is null");
            return false;
        }
        if(audioController == null){
            Log.d(TAG,"audioController is null");
            return false;
        }

        // ShareActionProviderのチャンネルコードを更新 インテントの設定
        ShareActionUtil.getInstance().setShareIntent(getApplicationContext(), channel.getChannelCode());

        //チャンネルに JOIN する
        ncmbUtil.joinChannelUser(channel, audioController.getPublicSocketAddress(), audioController.getLocalSocketAddress(), new JoinChannelUserlistener() {
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
                        Log.d(TAG,"GCM sendPush success");
                    }

                    @Override
                    public void error(NCMBException e) {
                        Log.d(TAG,"GCM sendPush error");
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

        return true;
    }

    /** 指定チャンネルからEXITする
     *
     * @param channel
     */
    private void exitChannel(final Channel channel){
        //チャンネルから Exit する
        ncmbUtil.exitChannelUser(channel, audioController.getPublicSocketAddress(), audioController.getLocalSocketAddress(), new ExitChannelUserlistener() {
            /** 成功
             *
             */
            @Override
            public void success() {
                //EXIT に成功
                setActiveChannel( null );
                Toast.makeText(getApplicationContext(), "EXIT!", Toast.LENGTH_SHORT).show();

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

    //Channel Exit ボタンクリック時
    private View.OnClickListener btnChannelExitOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //ログイン中のチャンネルからEXIT
            exitChannel(activeChannel);
        }
    };

    //購入ボタン クリック時
    private View.OnClickListener btnInAppBillingOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //購入処理
            if(inAppBillingUtil == null) return;
            inAppBillingUtil.onBuyButtonClicked(HomeActivity.this,Config.PRODUCT_ITEM_1_ID);
        }
    };

    //Signin ボタンクリック時
    private View.OnClickListener btnSigninOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            String name = Build.MODEL;
            String password = "password";
            String nickname = Build.DEVICE;

            User user = new User(name, password);
            user.setNickName(nickname);

            //ユーザー登録
            ncmbUtil.signin(user, new SigninListener() {
                @Override
                public void success(User user) {
                    Log.d(TAG, "signin success " + user);
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "signin error " + e);
                }
            });
        }
    };

    /** ログイン
     *
     * @param name
     * @param password
     */
    private void login(final String name,final String password){
        ncmbUtil.login(new User(name, password), new LoginListener() {
            @Override
            public void success(User user) {
                Log.d(TAG, "login success " + user);
                //TODO: ログイン中のユーザーを取得してみる
                User currentUser = ncmbUtil.getCurrentUser();
                Log.d(TAG, "login currentUser " + currentUser + " nickName:" + currentUser.getNickName());
            }

            @Override
            public void error(NCMBException e) {
                Log.e(TAG, "login error " + e);
            }
        });
    }

    //Login ボタンクリック時
    private View.OnClickListener btnLoginOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            ncmbUtil.login(new User(Build.MODEL, "password"), new LoginListener() {
                @Override
                public void success(User user) {
                    Log.d(TAG, "login success " + user);
                    //TODO: ログイン中のユーザーを取得してみる
                    User currentUser = ncmbUtil.getCurrentUser();
                    Log.d(TAG, "login currentUser " + currentUser + " nickName:" + currentUser.getNickName());
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "login error " + e);
                }
            });
        }
    };

    //Logout ボタンクリック時
    private View.OnClickListener btnLogoutOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            ncmbUtil.logout(new LogoutListener() {
                @Override
                public void success() {
                    Log.d(TAG, "logout success");

                    User currentUser = ncmbUtil.getCurrentUser();
                    Log.d(TAG, "logout currentUser " + currentUser);
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "logout error " + e);
                }
            });
        }
    };


    //チャンネル作成
    private View.OnClickListener btnNewChannelOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            String channelCode = Config.DEFAULT_CHANNEL_CODE;
            ncmbUtil.createChannel(channelCode, new CreateChannelListener() {
                @Override
                public void success(Channel channel) {
                    Log.d(TAG, "createChannel success " + channel.getChannelCode());

                    //Channel アイコン設定
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
                    byte[] data = BitmapUtil.bmp2byteArray(bmp, Bitmap.CompressFormat.JPEG);
                    String extension = Bitmap.CompressFormat.JPEG.name();
                    Log.d(TAG, "data size:" + data.length + " extension:" + extension);

                    ncmbUtil.saveChannelIcon(channel, data, new SetChannelIconImageListener() {
                        @Override
                        public void success(Channel channel) {
                            Log.d(TAG, "saveChannelIcon success size:" + channel.getIcon().length);
                        }

                        @Override
                        public void error(NCMBException e) {
                            Log.e(TAG, "saveChannelIcon error " + e);
                        }
                    });
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "createChannel error " + e);
                }
            });
        }
    };

    //チャンネル一覧取得
    private View.OnClickListener btnGetChannelListOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            ncmbUtil.getChannelList(new GetChannelListListener() {
                @Override
                public void success(List<Channel> channels) {
                    Log.d(TAG, "getChannelList success size:" + channels.size());
                    if (channels.size() == 0) return;

                    //チャンネルユーザーを追加
                    final Channel c = channels.get(0);

                    //ログイン中のユーザーを取得
                    final User currentUser = ncmbUtil.getCurrentUser();
                    Log.d(TAG, "currentUser:" + currentUser);

                    //チャンネルユーザー一覧を取得
                    ncmbUtil.getChannelUserList(c, new GetChannelUserListListener() {
                        @Override
                        public void success(final Channel channel) {
                            Log.d(TAG, "getChannelUserList success size:" + channel.getChannelUserList().size());

                            if (channel.getUser().getUserName() == null) {
                                Log.e(TAG, "channel.getUser().getUserName() is null");
                                return;
                            }
                            Log.d(TAG, "channel user:" + channel.getUser().getUserName());

                            {
                                InetSocketAddress publicAddr = new InetSocketAddress("192.168.0.1", 9999);
                                InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 8080);
                                ChannelUser cu1 = new ChannelUser(channel, currentUser, publicAddr, localAddr);
                                Log.d(TAG, "cu1 user:" + cu1.channel.getUser());
                                channel.addChannelUser(cu1);
                            }
                            {
                                InetSocketAddress publicAddr = new InetSocketAddress("192.168.0.2", 9999);
                                InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 8080);
                                ChannelUser cu2 = new ChannelUser(channel, currentUser, publicAddr, localAddr);
                                Log.d(TAG, "cu2 user:" + cu2.channel.getUser());
                                channel.addChannelUser(cu2);
                            }
                            //チャンネルユーザー一覧を更新
                            ncmbUtil.updateChannelUserList(channel, new UpdateChannelUserListener() {
                                @Override
                                public void success(Channel channel) {
                                    Log.d(TAG, "updateChannelUserList success");

                                    //チャンネルユーザー一覧を取得
                                    ncmbUtil.getChannelUserList(channel, new GetChannelUserListListener() {
                                        @Override
                                        public void success(final Channel channel) {
                                            Log.d(TAG, "getChannelUserList success size:" + channel.getChannelUserList().size());
                                        }

                                        @Override
                                        public void error(NCMBException e) {
                                            Log.e(TAG, "getChannelUserList error " + e);
                                        }
                                    });
                                }

                                @Override
                                public void error(NCMBException e) {
                                    Log.e(TAG, "updateChannelUserList error " + e);
                                }
                            });
                        }

                        @Override
                        public void error(NCMBException e) {
                            Log.e(TAG, "getChannelUserList error " + e);
                        }
                    });
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "getChannelList error " + e);
                }
            });
        }
    };

    //チャンネル削除
    private View.OnClickListener btnDeleteChannelOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            String channelCode = Config.DEFAULT_CHANNEL_CODE;
            ncmbUtil.deleteChannel(channelCode, new DeleteChannelListener() {
                @Override
                public void success() {
                    Log.d(TAG, "deleteChannel success");
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "deleteChannel error " + e);
                }
            });
        }
    };

    //QRコード ボタン
    private View.OnClickListener btnQRCodeScanOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(activeChannel == null) {
                Log.d(TAG,"activeChannel is null");
                return;
            }

            try {
                //QRコード生成
                Bitmap qrImg = QRCodeUtil.createQRCodeByZxing(activeChannel.getChannelCode(), 300);
                //QRコードを画面に表示
                ImageView imgQRcode = (ImageView) findViewById(R.id.imgQRcode);
                imgQRcode.setImageBitmap(qrImg);
            } catch (WriterException e) {
                e.printStackTrace();
            }

            //QRコードを読み込む
            QRCodeUtil.scanQRCode(HomeActivity.this);
        }
    };


    /** 画面の背景色を設定
     *
     * @param color
     */
    private void setBackgroundColor(final int color){
        handler.post(new Runnable() {
            @Override
            public void run() {
                mainLayout.setBackgroundColor(color);
            }
        });
    }

    /** MediaButtonクリックイベントを受け取るリスナー
     *
     */
    private MediaButtonReceiverListener mediaButtonReceiverListener = new MediaButtonReceiverListener(){
        /** MEDIA_BUTTON ボタンが押された事を通知
         *
         */
        @Override
        public void onClickMediaButton(){
            Log.d(TAG, "BUTTON PRESSED!");
            Toast.makeText(getApplicationContext(), "BUTTON PRESSED!", Toast.LENGTH_SHORT).show();
        }
    };


    /** 照度センサーのステータス受信
     *
     */
    private LightSensorManagerListener lightSensorManagerListener = new LightSensorManagerListener(){

        @Override
        public void onLightSensorChanged(SensorEvent event, float value) {
            //Log.d(TAG, "onLightSensorChanged value:" + value);
            if(value < Config.LIGHT_SENSOR_THRESHOLD){
                //照度センサーの閾値 判定
                //TODO: 画面の操作を停止する等を行う
                //仮で非表示にする
                mainLayout.setVisibility(View.INVISIBLE);
            }
            else{
                mainLayout.setVisibility(View.VISIBLE);
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
            Log.d(TAG,"IAB セットアップ 成功");
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
                Log.d(TAG,"IAB 商品を購入済み");
                //TODO: 購入済みなので 制限された機能の開放等を行う
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
                Log.d(TAG,"IAB 購入 失敗");
                return;
            }
            Log.d(TAG,"IAB 購入 成功");

            if (purchase.getSku().equals(Config.PRODUCT_ITEM_1_ID)) {
                Log.d(TAG, "IAB あなたの商品：" + Config.PRODUCT_ITEM_1_ID + "を購入しました。\n"
                        + "orderIdは：" + purchase.getOrderId() + "\n"
                        + "INAPP_PURCHASE_DATAのJSONは：" + purchase.getOriginalJson());

                //TODO: 購入商品に一致する機能解除等を行う
            }
        }
    };

    /** 録音,再生,送信,受信 処理のリスナー
     *
     */
    private AudioControllerListener audioControllerListener = new AudioControllerListener(){

        /** 録音データが通知される
         *
         * @param packet 録音したデータのパケット
         * @param volume 録音ボリュームの最大値
         */
        @Override
        public void onAudioRecord(final Packet packet, short volume) {
            //volume閾値で画面を更新
            if(volume < Config.VOLUME_THRESHOLD){
                //仮で背景色を設定
                setBackgroundColor(Color.RED);
                return;
            }
            setBackgroundColor(Color.GREEN);
        }

        /** データを受信した時通知される
         *
         * @param packet 受信したパケット
         */
        @Override
        public void onReceive(Packet packet) {

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
                    Log.d(TAG,"success");
                    //アイコンが設定されたので更新
                    setActiveChannel( channel );
                }

                @Override
                public void error(NCMBException e) {
                    e.printStackTrace();
                    Log.e(TAG,"e:"+e.getMessage());
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
    private ChannelCodeDialogFragmentListener channelCodeDialogFragmentListener = new ChannelCodeDialogFragmentListener(){

        @Override
        public void ok(Bundle params) {
            //チャンネルコードが入力され OK を押された
            String channelCode = params.getString(ChannelCodeDialogFragment.KEY_CHANNEL_CODE);
            if((channelCode != null) && (channelCode.length() > 0)) {
                HomeActivity.this.lblChannelCode.setText(channelCode);

                // join
                joinChannel(channelCode);
            }
        }

        @Override
        public void cancel(Bundle params) {
            //キャンセルされた

        }
    };

    //ハンズフリー切り替えスイッチ
    private CompoundButton.OnCheckedChangeListener btnHandsFreeSwitchOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            String c = (isChecked)?"YES":"NO";
            Log.d(TAG,"onCheckedChanged isChecked:"+c);
            //TODO: VADのON,OFF
        }
    };

    //メディア切り替えボタン
    private View.OnClickListener btnMediaOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            //購入済みアイテムの確認
            if(purchase == null) {
                //ヘッドセット利用は有料
                if (inAppBillingUtil == null) return;
                inAppBillingUtil.onBuyButtonClicked(HomeActivity.this, Config.PRODUCT_ITEM_1_ID);
                //購入結果は inAppBillingUtil リスナーに通知される
            }
            else {
                //アイテム購入済みの場合 切り替え可能
                TextView lblMediaType = (TextView) findViewById(R.id.lblMediaType);
                boolean isChecked = getText(R.string.media_type_true).equals(lblMediaType.getText());
                if(isChecked == false){
                    //ヘッドセット
                    lblMediaType.setText(getText(R.string.media_type_true));
                    btnMedia.setBackgroundResource(R.drawable.headset16);
                }
                else{
                    //スピーカー
                    lblMediaType.setText(getText(R.string.media_type_false));
                    btnMedia.setBackgroundResource(R.drawable.audio45);
                }

            }
        }
    };

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
}
