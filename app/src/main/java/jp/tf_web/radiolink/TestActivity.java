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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

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
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.udp.service.UDPService;
import jp.tf_web.radiolink.qrcode.QRCodeUtil;
import jp.tf_web.radiolink.qrcode.ScanQRCodeResultListener;
import jp.tf_web.radiolink.sensor.LightSensorManager;
import jp.tf_web.radiolink.sensor.LightSensorManagerListener;
import jp.tf_web.radiolink.util.BitmapUtil;
import jp.tf_web.radiolink.scheme.ShareActionUtil;


public class TestActivity extends Activity
{
    private static final String TAG = "TestActivity";

    //起動時パラメータのキー
    public static final String KEY_SHARE_ACTION_CHANNEL_CODE = "channel_code";

    //ハンドラー
    private Handler handler;

    //メインのレイアウト
    private LinearLayout mainLayout;

    //ログ表示用テキスト
    private TextView tv;

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

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);

        Log.d(TAG, "onCreate() "+this);

        Log.d(TAG, " DEVICE:"+Build.DEVICE+" MODEL:"+Build.MODEL);

        handler = new Handler();

        //API処理をするユーテリティ
        ncmbUtil = NCMBUtil.getInstance(getApplicationContext(), Config.NCMB_APP_KEY, Config.NCMB_CLIENT_KEY);

        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);

        tv = (TextView)findViewById(R.id.txtView);
        tv.setText(R.string.app_name);

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

        // 購入結果をActivityが受け取るための設定
        if(isResult == false) {
            isResult = inAppBillingUtil.onActivityResult(requestCode, resultCode, data);
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
        audioController.destroy();
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

        // ShareActionProviderにインテントの設定
        ShareActionUtil.getInstance().setShareIntent(getApplicationContext(), "hogehoge");

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

    //各クラスの初期化
    private void initialize(){
        //課金 処理の為の初期化
        if(inAppBillingUtil == null) {
            inAppBillingUtil = new InAppBillingUtil(getApplicationContext(), inAppBillingUtilListener);
            inAppBillingUtil.setup();
        }

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

    //START ボタンクリック時
    private View.OnClickListener btnStartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            start();
        }
    };

    //STOP ボタンクリック時
    private View.OnClickListener btnStopOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stop();
        }
    };


    //Channel Join ボタンクリック時
    private View.OnClickListener btnChannelJoinOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //チャンネル一覧から 一番新しいチャンネルを選択する
            ncmbUtil.getChannelList(new GetChannelListListener() {
                @Override
                public void success(List<Channel> channels) {
                    Log.d(TAG, "getChannelList success size:" + channels.size());
                    if (channels.size() == 0) return;

                    //仮で一番新しいチャンネルを選択
                    final Channel c = channels.get(0);

                    //チャンネルに JOIN する
                    joinChannel(c);
                }

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "getChannelList error " + e);
                }
            });


        }
    };

    /** チャンネルに JOIN する
     *
     * @param channel チャンネル
     */
    private void joinChannel(final Channel channel){
        Log.d(TAG,"joinChannel");
        if(channel == null){
            Log.d(TAG,"channel is null");
            return;
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
                activeChannel = channel;
                audioController.setActiveChannel( channel );
                Toast.makeText(getApplicationContext(), "JOIN!", Toast.LENGTH_SHORT).show();
                //TODO: GCM 通知
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
                activeChannel = null;
                audioController.setActiveChannel( null );
                Toast.makeText(getApplicationContext(), "EXIT!", Toast.LENGTH_SHORT).show();

                //TODO: GCM 通知
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
            inAppBillingUtil.onBuyButtonClicked(TestActivity.this,Config.PRODUCT_ITEM_1_ID);
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

    //Login ボタンクリック時
    private View.OnClickListener btnLoginOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            String name = Build.MODEL;
            String password = "password";

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
            String channelCode = "testChannel";
            ncmbUtil.createChannel(channelCode, new CreateChannelListener() {
                @Override
                public void success(Channel channel) {
                    Log.d(TAG, "createChannel success " + channel.getChannelCode());

                    //Channel アイコン設定
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon_dummy);
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
            String channelCode = "testChannel";
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


            try {
                //QRコード生成
                Bitmap qrImg = QRCodeUtil.createQRCodeByZxing("testChannel", 300);
                //QRコードを画面に表示
                ImageView imgQRcode = (ImageView) findViewById(R.id.imgQRcode);
                imgQRcode.setImageBitmap(qrImg);
            } catch (WriterException e) {
                e.printStackTrace();
            }

            //QRコードを読み込む
            QRCodeUtil.scanQRCode(TestActivity.this);
        }
    };


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
            Purchase purchase = inv.getPurchase(Config.PRODUCT_ITEM_1_ID);
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

        @Override
        public void onActive() {
            Log.d(TAG,"AudioControllerListener onActive");
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
}
