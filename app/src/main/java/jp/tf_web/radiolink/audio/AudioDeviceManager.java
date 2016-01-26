package jp.tf_web.radiolink.audio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.util.List;

import jp.tf_web.radiolink.bluetooth.BluetoothControlReceiver;

/** Bluetoothヘッドセットを有効にするための設定
 *
 * Created by furukawanobuyuki on 2015/11/05.
 */
public class AudioDeviceManager {
    private static String TAG = "AudioDeviceManager";

    private Context context;

    //Bluetooth アダプター
    private BluetoothAdapter bluetoothAdapter;

    //ヘッドセット
    private BluetoothHeadset bluetoothHeadset;

    //Bluetoothデバイス
    private BluetoothDevice bluetoothDevice;

    //Bluetoothヘッドセットが接続ステータス
    private boolean _isBluetoothConnected;

    private AudioManager audioManager;

    private ComponentName controlReceiverName;

    //オーディオデバイス タイプ
    public enum AUDIO_DEVICE_MODE{
        NORMAL,
        SPEAKER,
        HEADSET
    }

    //現在のオーディオデバイスモード
    private AUDIO_DEVICE_MODE audioDeviceMode = AUDIO_DEVICE_MODE.NORMAL;

    //インストラクタ
    public AudioDeviceManager(Context context){
        this.context = context;

        //各種初期化
        initialize();
    }

    //Bluetoothオーディオデバイスとの接続ステータス
    public boolean isBluetoothConnected(){
        return _isBluetoothConnected;
    }

    /** 各種初期化
     *
     */
    private void initialize(){
        //Bluetoothアダプターを初期化
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Log.d(TAG, "bluetoothAdapter is null");
            return;
        }
        bluetoothAdapter.getProfileProxy(context, bluetoothProfileListener, BluetoothProfile.HEADSET);
    }

    /** オーディオデバイスの設定
     *
     * @param mode
     */
    public void setAudioDevice(AUDIO_DEVICE_MODE mode){
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if(mode == AUDIO_DEVICE_MODE.HEADSET) {
            //ヘッドセット
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(false);
            audioManager.setBluetoothScoOn(true);
            audioManager.startBluetoothSco();
        }
        else if(mode == AUDIO_DEVICE_MODE.SPEAKER){
            //スピーカー
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(true);
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
        else if(mode == AUDIO_DEVICE_MODE.NORMAL){
            //通話用スピーカー
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(false);
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
        audioDeviceMode = mode;
    }

    /** 現在のオーディオデバイスモードを取得する
     *
     * @return
     */
    public AUDIO_DEVICE_MODE getAudioDeviceMode(){
        return audioDeviceMode;
    }

    //ヘッドセットに接続
    public void startVoiceRecognition(){
        //オーディオデバイスの設定
        setAudioDevice(AUDIO_DEVICE_MODE.NORMAL);

        if(bluetoothDevice == null) return;

        //MEDIA_BUTTONのイベントを受け取る
        controlReceiverName = new ComponentName(context, BluetoothControlReceiver.class);
        audioManager.registerMediaButtonEventReceiver(controlReceiverName);

        //Bluetooth ヘッドセットを利用する
        bluetoothHeadset.startVoiceRecognition(bluetoothDevice);
    }

    //ヘッドセットを切断
    public void stopVoiceRecognition(){
        if(bluetoothDevice == null) return;

        //Bluetooth ヘッドセットの利用を停止する
        bluetoothHeadset.stopVoiceRecognition(bluetoothDevice);

        setAudioDevice(AUDIO_DEVICE_MODE.NORMAL);

        //bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
        //bluetoothAdapter = null;
    }

    //Bluetoothプロファイル サービスリスナー
    private BluetoothProfile.ServiceListener bluetoothProfileListener = new BluetoothProfile.ServiceListener(){
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG,"onServiceConnected");
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = (BluetoothHeadset) proxy;
                List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
                if(devices.size() > 0){
                    _isBluetoothConnected = true;
                    bluetoothDevice = devices.get(0);

                    for(BluetoothDevice device:devices){
                        Log.d(TAG,"device name:"+device.getName());
                    }
                }
            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(TAG,"onServiceDisconnected");
                bluetoothHeadset = null;
                _isBluetoothConnected = false;
            }
        }
    };
}
