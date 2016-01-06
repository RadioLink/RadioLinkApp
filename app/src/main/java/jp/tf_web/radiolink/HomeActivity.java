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
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentResult;
import com.nifty.cloud.mb.core.NCMBException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.tf_web.radiolink.audio.OpusManager;
import jp.tf_web.radiolink.audio.RecordManager;
import jp.tf_web.radiolink.audio.RecordManagerListener;
import jp.tf_web.radiolink.audio.TrackManager;
import jp.tf_web.radiolink.billing.InAppBillingUtil;
import jp.tf_web.radiolink.billing.InAppBillingUtilListener;
import jp.tf_web.radiolink.billing.util.IabResult;
import jp.tf_web.radiolink.billing.util.Inventory;
import jp.tf_web.radiolink.billing.util.Purchase;
import jp.tf_web.radiolink.bluetooth.BluetoothAudioDeviceManager;
import jp.tf_web.radiolink.bluetooth.MediaButtonReceiver;
import jp.tf_web.radiolink.bluetooth.MediaButtonReceiverListener;
import jp.tf_web.radiolink.ncmb.NCMBUtil;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.ChannelUser;
import jp.tf_web.radiolink.ncmb.db.User;
import jp.tf_web.radiolink.ncmb.listener.CreateChannelListener;
import jp.tf_web.radiolink.ncmb.listener.DeleteChannelListener;
import jp.tf_web.radiolink.ncmb.listener.ExitChannelUserlistener;
import jp.tf_web.radiolink.ncmb.listener.GetChannelListListener;
import jp.tf_web.radiolink.ncmb.listener.GetChannelUserListListener;
import jp.tf_web.radiolink.ncmb.listener.JoinChannelUserlistener;
import jp.tf_web.radiolink.ncmb.listener.LoginListener;
import jp.tf_web.radiolink.ncmb.listener.LogoutListener;
import jp.tf_web.radiolink.ncmb.listener.SetChannelIconImageListener;
import jp.tf_web.radiolink.ncmb.listener.SigninListener;
import jp.tf_web.radiolink.ncmb.listener.UpdateChannelUserListener;
import jp.tf_web.radiolink.net.NetWorkUtil;
import jp.tf_web.radiolink.net.protocol.PacketUtil;
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.protocol.packet.Payload;
import jp.tf_web.radiolink.net.udp.service.UDPService;
import jp.tf_web.radiolink.net.udp.service.UDPServiceListener;
import jp.tf_web.radiolink.net.udp.service.UDPServiceReceiver;
import jp.tf_web.radiolink.qrcode.QRCodeUtil;
import jp.tf_web.radiolink.qrcode.ScanQRCodeResultListener;
import jp.tf_web.radiolink.sensor.LightSensorManager;
import jp.tf_web.radiolink.sensor.LightSensorManagerListener;
import jp.tf_web.radiolink.util.BitmapUtil;


public class HomeActivity extends Activity
{
    private static String TAG = "HomeActivity";

    //ハンドラー
    private Handler handler;

    //メインのレイアウト
    private LinearLayout mainLayout;

    //ログ表示用テキスト
    private TextView tv;

    //Bluetoothヘッドセットへの接続等
    private BluetoothAudioDeviceManager bluetoothAudioDeviceManager;

    //再生処理をするクラス
    private TrackManager trackManager;

    //録音関連の処理をするクラス
    private RecordManager recordManager;

    //MEDIA_BUTTONのクリック受け取り
    private MediaButtonReceiver mediaButtonReceiver;

    //照度センサー
    private LightSensorManager lightSensorManager;

    //課金処理の実装
    private InAppBillingUtil inAppBillingUtil;

    //Opusデコード,エンコード
    private OpusManager opusManager;

    //UDPで受信したパケットを受け取るレシーバー
    private UDPServiceReceiver udpServiceReceiver;

    //API処理をするユーテリティ
    private NCMBUtil ncmbUtil;

    //ローカルIP,ポート
    private InetSocketAddress localAddr;

    //パブリックIP,ポート
    private InetSocketAddress publicAddr;

    //選択中のチャンネル
    private Channel activeChannel;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.d(TAG, "onCreate()");

        Log.d(TAG, " DEVICE:"+Build.DEVICE+" MODEL:"+Build.MODEL);

        handler = new Handler();

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

        //パケット 処理テストボタン
        Button btnPacketChannel = (Button)findViewById(R.id.btnPacket);
        btnPacketChannel.setOnClickListener(this.btnPacketOnClickListener);

        //QRコードスキャン ボタン
        Button btnQRCodeScan = (Button)findViewById(R.id.btnQRCodeScan);
        btnQRCodeScan.setOnClickListener(this.btnQRCodeScanOnClickListener);


        //課金 処理の為の初期化
        inAppBillingUtil = new InAppBillingUtil(getApplicationContext(),inAppBillingUtilListener);
        inAppBillingUtil.setup();

        //各クラスの初期化
        initialize();
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
        inAppBillingUtil.dispose();

        if(opusManager == null) return;
        try{
            opusManager.close();
        }
        catch ( IOException e ){
            Log.e( TAG,"Could not close or flush stream", e );
        }
    }

    //各クラスの初期化
    private void initialize(){

        //API処理をするユーテリティ
        ncmbUtil = new NCMBUtil(getApplicationContext(),Config.NCMB_APP_KEY,Config.NCMB_CLIENT_KEY);

        //OPUSデコード,エンコード
        opusManager = new OpusManager(Config.SAMPLE_RATE_IN_HZ,
                1,
                Config.OPUS_FRAME_SIZE,
                Config.OPUS_OUTPUT_BITRATE_BPS);

        //UDPServiceからの受信を受け取るレシーバー
        udpServiceReceiver = new UDPServiceReceiver(udpServiceListener);
        udpServiceReceiver.registerReceiver(getApplicationContext());

        //Bluetoothヘッドセットを利用する
        bluetoothAudioDeviceManager = new BluetoothAudioDeviceManager(getApplicationContext());

        //再生処理の初期化
        trackManager = new TrackManager(getApplicationContext(), Config.SAMPLE_RATE_IN_HZ);

        //録音関連処理の初期化
        recordManager = new RecordManager(getApplicationContext(),Config.SAMPLE_RATE_IN_HZ,Config.OPUS_FRAME_SIZE*2,recordManagerListener);

        //照度センサーを初期化
        lightSensorManager = new LightSensorManager(getApplicationContext(),lightSensorManagerListener);
    }

    //各クラスの開始
    private void start(){
        //ローカルIPアドレスを取得
        NetWorkUtil.getLocalIpv4Address(new NetWorkUtil.GetLocalIpv4AddressListener() {
            @Override
            public void onResult(final String address) {
                Log.d(TAG,"local IpAddress:"+address);

                localAddr = new InetSocketAddress(address,Config.BIND_PORT);

                //UDPサービス START
                Map<String,Object> params = new HashMap<String,Object>(){
                    {
                        put(UDPService.KEY_NAME_BIND_ADDRESS, address);
                        put(UDPService.KEY_NAME_BIND_PORT, Integer.valueOf(Config.BIND_PORT));
                    }
                };
                UDPService.sendCmd(getApplicationContext(), UDPService.CMD_START, params);
            }
        });

        //Bluetoothヘッドセットがあればそれを使う
        bluetoothAudioDeviceManager.startVoiceRecognition();

        //MEDIA_BUTTON イベントを受信する
        mediaButtonReceiver = new MediaButtonReceiver(getApplicationContext(), mediaButtonReceiverListener);
        mediaButtonReceiver.registerReceiver();

        //再生 開始
        trackManager.start(AudioManager.STREAM_MUSIC);

        //録音 開始
        recordManager.start();

        //照度センサーを利用 開始
        lightSensorManager.start();
    }

    //各クラスの終了
    private void stop(){
        //録音 停止
        if(recordManager != null) {
            recordManager.stop();
        }
        //再生 停止
        if(trackManager != null) {
            trackManager.stop();
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
                    ncmbUtil.joinChannelUser(c, publicAddr, localAddr, new JoinChannelUserlistener() {
                        /** 成功
                         *
                         * @param channel
                         */
                        @Override
                        public void success(final Channel channel) {
                            //JOINに成功
                            activeChannel = channel;
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

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "getChannelList error " + e);
                }
            });

        }
    };

    //Channel Exit ボタンクリック時
    private View.OnClickListener btnChannelExitOnClickListener = new View.OnClickListener() {
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

                    //チャンネルに Exit する
                    ncmbUtil.exitChannelUser(c, publicAddr, localAddr, new ExitChannelUserlistener() {
                        /** 成功
                         *
                         */
                        @Override
                        public void success() {
                            //EXIT に成功
                            activeChannel = null;
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

                @Override
                public void error(NCMBException e) {
                    Log.e(TAG, "getChannelList error " + e);
                }
            });
        }
    };

    //購入ボタン クリック時
    private View.OnClickListener btnInAppBillingOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //購入処理
            if(inAppBillingUtil == null) return;
            inAppBillingUtil.onBuyButtonClicked(HomeActivity.this);
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

    //パケットテストボタン
    private View.OnClickListener btnPacketOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PacketUtil.getInstance().test();
        }
    };

    //パケットテストボタン
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
            QRCodeUtil.scanQRCode(HomeActivity.this);
        }
    };

    /** 録音結果を受け取るリスナー
     *
     */
    private RecordManagerListener recordManagerListener = new RecordManagerListener(){

        /** 録音結果の通知
         *
         * @param data 録音したPCMデータ
         * @param size 録音データのサイズ
         * @param volume 録音ボリュームの最大値
         */
        @Override
        public void onAudioRecord(final byte[] data,final int size,final short volume) {
            if(activeChannel == null){
                //Log.d(TAG, "activeChannel is null");
                return;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "volume:"+volume);
                    tv.setText("volume:" + volume);
                }
            });
            //TODO: volume閾値での簡易VAD
            if(volume < Config.VOLUME_THRESHOLD){
                //仮でVAD識別の為に背景色を変更
                setBackgroundColor(Color.RED);
                return;
            }
            //仮でVAD識別の為に背景色を変更
            setBackgroundColor(Color.GREEN);

            //TODO: volume を 画面に反映させる

            //OPUSエンコード
            byte[] opus = opusManager.encode(data);
            if(opus.length > 1) {
                //パケットにまとめてから UDPでユーザーに送信する
                Packet packet = PacketUtil.getInstance().createPacket();
                packet.addPayload(opus);

                udpServiceSendByteArray(packet.toByteArray());
            }
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

    /** UDPサービスに byte[]データを送信
     *
     * @param src  byte[]データ
     */
    private void udpServiceSendByteArray(final byte[] src){
        if(activeChannel == null) return;
        //データを送信
        String publicIp = publicAddr.getAddress().getHostAddress();
        int publicPort = publicAddr.getPort();
        for(ChannelUser cu:activeChannel.getChannelUserList()){
            Log.d(TAG,"cu:"+cu.getUser().getUserName());
            //自分以外に送信
            String ip = cu.publicSocketAddress.getAddress().getHostAddress();
            int port = cu.publicSocketAddress.getPort();
            Log.d(TAG," ip:"+ip+":"+port+" publicIp:"+publicIp+":"+publicPort);
            if((ip.equals(publicIp)) && (publicPort == port)){
                continue;
            }
            //送信
            udpServiceSendByteArray(cu,src);
        }
    }

    /** UDPサービスに byte[]データを送信
     *
     * @param channelUser チャンネルユーザー
     * @param src  byte[]データ
     */
    private void udpServiceSendByteArray(final ChannelUser channelUser,final byte[] src){
        //データを送信
        Map<String,Object> params = new HashMap<String,Object>(){
            {
                //送信先パブリックIP,ポート
                put(UDPService.KEY_NAME_CONNECT_HOST, channelUser.publicSocketAddress.getAddress().getHostAddress());
                put(UDPService.KEY_NAME_CONNECT_PORT, Integer.valueOf(channelUser.publicSocketAddress.getPort()));

                //送信先ローカルIPポート
                put(UDPService.KEY_NAME_SEND_HOST, channelUser.localSocketAddress.getAddress().getHostAddress());
                put(UDPService.KEY_NAME_SEND_PORT, Integer.valueOf(channelUser.localSocketAddress.getPort()));

                put(UDPService.KEY_NAME_SEND_BUFFER, src);
            }
        };
        UDPService.sendCmd(getApplicationContext(), UDPService.CMD_SEND, params);
    }

    //ブロードキャストレシーバーから受信したイベントを受け取る
    private UDPServiceListener udpServiceListener = new UDPServiceListener(){

        /** STUN Binding 結果を通知
         *
         * @param publicSocketAddr パブリックIP,ポート
         */
        @Override
        public void onStunBinding(final InetSocketAddress publicSocketAddr){
            Log.d(TAG, "onStunBinding");
            //パブリックIP,ポートを保存
            publicAddr = publicSocketAddr;
        }

        /** UDPServiceから届いいた メッセージを受信した場合
         *
         * @param cmd コマンド文字列
         * @param data 受信した byte[]
         */
        @Override
        public void onReceive(final String cmd, final byte[] data) {
            Log.d(TAG, "onReceive cmd:" + cmd);
            if(cmd == null){
                return;
            }

            if (cmd.equals(UDPService.CMD_RECEIVE)) {
                //チャンネルへのJOIN常態を確認
                if(activeChannel == null){
                    return;
                }
                if(data == null){
                    Log.d(TAG, "buf is null");
                    return;
                }
                //受信データのサイズ
                Log.i(TAG, "receive data size:"+data.length);

                //バイト配列からパケットを生成
                Packet packet = PacketUtil.getInstance().createPacket(data);
                //TODO: シーケンス番号順や送信元識別子順にソートする
                //TODO: タイムスタンプが古い物は破棄

                //OPUSデコードする
                List<Payload> payload = packet.getPayload();
                for(Payload p:payload){
                    //ペイロードから音を取り出す
                    byte[] pcm = opusManager.decode(p.getData(),Config.OPUS_FRAME_SIZE);

                    //再生
                    trackManager.write(pcm, 0, pcm.length);
                }
            }
        }
    };
}
