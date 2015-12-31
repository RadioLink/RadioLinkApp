/** ホーム画面
 *
 */
package jp.tf_web.radiolink;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
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
import jp.tf_web.radiolink.net.NetWorkUtil;
import jp.tf_web.radiolink.net.udp.service.UDPService;
import jp.tf_web.radiolink.net.udp.service.UDPServiceListener;
import jp.tf_web.radiolink.net.udp.service.UDPServiceReceiver;
import jp.tf_web.radiolink.sensor.LightSensorManager;
import jp.tf_web.radiolink.sensor.LightSensorManagerListener;
import jp.tf_web.radiolink.util.ByteArrayUtil;


public class HomeActivity extends Activity
{
    private static String TAG = "HomeActivity";

    //ハンドラー
    private Handler handler;

    //メインのレイアウト
    public LinearLayout mainLayout;

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

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Log.d(TAG, "onCreate()");

        handler = new Handler();

        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);

        TextView tv = (TextView)findViewById(R.id.txtView);
        tv.setText(R.string.app_name);

        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this.btnStartOnClickListener);

        Button btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this.btnStopOnClickListener);

        //アイテム購入ボタン
        Button btnInAppBilling = (Button)findViewById(R.id.btnInAppBilling);
        btnInAppBilling.setOnClickListener(this.btnInAppBillingOnClickListener);

        //STUNテストボタン
        Button btnStunBinding = (Button)findViewById(R.id.btnStunBinding);
        btnStunBinding.setOnClickListener(this.btnStunBindingOnClickListener);

        //課金 処理の為の初期化
        inAppBillingUtil = new InAppBillingUtil(getApplicationContext(),inAppBillingUtilListener);
        inAppBillingUtil.setup();

        //各クラスの初期化
        initialize();
    }

    /** アプリを再起動した等
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");
        if(bluetoothAudioDeviceManager.isBluetoothConnected()) {
            //ヘッドセットが有効だった場合はボタンを押されたとして通知
            MediaButtonReceiver.onClickMediaButton(getApplicationContext());
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 購入結果をActivityが受け取るための設定
        if (!inAppBillingUtil.onActivityResult(requestCode, resultCode, data)) {
            //課金と無関係の場合、メインの onActivityResult を実行
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


    //購入ボタン クリック時
    private View.OnClickListener btnInAppBillingOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //購入処理
            if(inAppBillingUtil == null) return;
            inAppBillingUtil.onBuyButtonClicked(HomeActivity.this);
        }
    };

    //STUN ボタンクリック時
    private View.OnClickListener btnStunBindingOnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
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
            //録音結果を受け取る
            //TODO: volume閾値での簡易VAD
            if(volume < 200){
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
                //UDPで送信
                udpServiceSendByteArray("192.168.0.9", Config.BIND_PORT, opus);
            }

            //仮でエコーしてみる
            //Log.d(TAG, "onAudioRecord data:" + data.length + " size:" + size + " volume:" + volume);
            //trackManager.write(data, 0, size);
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
     * @param addr 送信先ホストアドレス
     * @param port ポート
     * @param src  byte[]データ
     */
    private void udpServiceSendByteArray(final String addr,final int port,final byte[] src){
        //サービスにデータを送信
        Map<String,Object> params = new HashMap<String,Object>(){
            {
                put(UDPService.KEY_NAME_CONNECT_HOST, addr);
                put(UDPService.KEY_NAME_CONNECT_PORT, Integer.valueOf(port));

                put(UDPService.KEY_NAME_SEND_HOST, addr);
                put(UDPService.KEY_NAME_SEND_PORT, Integer.valueOf(port));

                put(UDPService.KEY_NAME_SEND_BUFFER, src);
            }
        };
        UDPService.sendCmd(getApplicationContext(), UDPService.CMD_SEND, params);
    }

    //ブロードキャストレシーバーから受信したイベントを受け取る
    private UDPServiceListener udpServiceListener = new UDPServiceListener(){

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
                if(data == null){
                    Log.d(TAG, "buf is null");
                    return;
                }
                //受信データのサイズ
                Log.i(TAG, "receive data size:"+data.length);

                //OPUSデコードする
                byte[] pcm = opusManager.decode(data,Config.OPUS_FRAME_SIZE);

                //再生
                trackManager.write(pcm, 0, pcm.length);
            }
        }
    };
}
