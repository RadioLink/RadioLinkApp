package jp.tf_web.radiolink;

import java.net.InetAddress;

/** 設定
 *
 * Created by furukawanobuyuki on 2015/12/11.
 */
public class Config {

    private Config(){}

    //サンプルレート 録音,OPUSの設定に利用する
    public static final int SAMPLE_RATE_IN_HZ = 16*1000;

    //録音バッファのサイズ
    //OPUS_FRAME_SEC 0.0025, 0.005, 0.01, 0.02, 0.04, 0.06
    public static final int OPUS_FRAME_SIZE = (int)(SAMPLE_RATE_IN_HZ*0.04);

    // OPUS Output
    // サンプルレート:8000 src_size:480
    // 320 kb/s bitrate.   enc_size:396
    // 160 kb/s bitrate.   enc_size:396
    //  80 kb/s bitrate.   enc_size:342
    //  48 kb/s bitrate.   enc_size:275  FMラジオ
    //  32 kb/s bitrate.   enc_size:223  固定電話,AMラジオ
    //  12 kb/s bitrate.   enc_size: 80  3G回線
    //   8 kb/s bitrate.   enc_size: 42  携帯電話
    public static final int OPUS_OUTPUT_BITRATE_BPS =32*1024;

    //照度センサーの閾値
    public static final int LIGHT_SENSOR_THRESHOLD = 20;

    //Google Play アプリのライセンスキー
    public static final String GOOGLE_PLAY_PUBLICKEY = PrivateConfig.GOOGLE_PLAY_PUBLICKEY;

    //Google Play 課金アイテムのID
    public static final String PRODUCT_ITEM_1_ID = PrivateConfig.PRODUCT_ITEM_1_ID;

    //Google STUN Server
    public static final String STUN_SERVER_NAME = "stun.l.google.com";
    public static final int STUN_SERVER_PORT = 19302;

    //UDPからの受信に使うポート
    public static final int BIND_PORT = 55560;

    //ニフティクラウド mobile backend アプリケーションキー
    public static final String NCMB_APP_KEY = PrivateConfig.NCMB_APP_KEY;

    //ニフティクラウド mobile backend クライアントキー
    public static final String NCMB_CLIENT_KEY = PrivateConfig.NCMB_CLIENT_KEY;
}
